package xed.api_service.service

import java.io.File
import java.net.InetAddress

import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.Guice
import com.twitter.inject.{Injector, IntegrationTest}
import com.twitter.util.Future
import org.apache.commons.io.FileUtils
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.nutz.ssdb4j.SSDBs
import xed.api_service.domain.{Card, Deck, Status}
import xed.api_service.module._
import xed.api_service.repository.DeckRepository
import xed.api_service.repository.ESRepository.ZActionRequestBuilder
import xed.api_service.repository.card.SSDBCardRepository
import xed.api_service.util.Implicits.FutureEnhance
import xed.api_service.util.{JsonUtils, ZConfig}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class RecoverMissingReviewCardTool extends IntegrationTest {


  override protected def injector: Injector =  Injector(Guice.createInjector(
    XedApiModule,
    PublicPathConfigModule
  ))

val cardKey = ZConfig.getString("es_client.ssdb_card_hashmap_name")
  val ssdb = SSDBs.pool(
    "34.87.143.227",
    8888,
    5000,
    null)

  val cardRepo = SSDBCardRepository(
    ssdb =  ssdb,
    cardKey
  )

  val deckRepository = injector.instance[DeckRepository]
  val srsBotService = injector.instance[SRSService]
  val VALID_COURSE_ID = Set(
    "V0001",
    "V0002",
    "V0003",
    "V0004",
    "phrasal_verb",
    "verb_collocations"
  )
  test("DELETE missing learning card users") {
    val fileContent = FileUtils.readFileToString(new File("./data/missing_review_cards.json"))
    val json = JsonUtils.fromJson[mutable.Seq[JsonNode]](fileContent)

    json.foreach(userJson =>{
      val username = userJson.at("/username").asText()
      val cardByCourses = JsonUtils.fromNode[mutable.Map[String,Seq[Seq[Any]]]](userJson.at("/card_by_courses"))

      cardByCourses.foreach(entry =>{
        val courseId = entry._1
        val cardIds = entry._2.map(x => x(0).toString)

      })
    })
  }

  test("Get missing learning card users") {
//
//   val  profileIds = getAllUserProfileIds()
//    println(profileIds.size)
//
//    val userWithLearningCourseMap = profileIds.map(username =>{
//      val (courseIds, validCourseIds) = getLearningCouseIds(username)
//      username -> (courseIds, validCourseIds)
//    }).filter(_._2._1.nonEmpty).toMap
//
//    val userWithValidCourseMap = profileIds.map(username =>{
//      val (courseIds, validCourseIds) = getLearningCouseIds(username)
//      username -> (courseIds, validCourseIds)
//    }).filter(_._2._2.nonEmpty).toMap
//
//    println(
//      s"""
//        |Profiles: ${profileIds.size}
//        |Learning profiles: ${userWithLearningCourseMap.size}
//        |Valid learning profiles: ${userWithValidCourseMap.size}
//        |""".stripMargin)
//
//    Implicits.tryWith(new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./data/missing_review_cards.json")))) {
//      writer => {
//        var maxCard= 0;
//
//        val data = ListBuffer.empty[JsonNode]
//
//        userWithValidCourseMap.foreach(entry =>{
//          val username = entry._1
//          val (courseIds, validCourseIds) = entry._2
//
//          val cardByCourseMap = validCourseIds.map(courseId =>{
//            val cardIds  = learnRepo.getCompletedCards(username, courseId)
//            val cardHasReviewMap = srsBotService.multiGetReviewInfo(username, cardIds).sync()
//            val missingReviewCards = cardIds.filterNot( cardHasReviewMap.contains(_))
//
//            if(maxCard < missingReviewCards.size) {
//              maxCard = missingReviewCards.size
//            }
//
//            courseId -> missingReviewCards
//          }).filter(_._2.nonEmpty).toMap
//
//          if(cardByCourseMap.nonEmpty) {
//            data.append(JsonUtils.toNode[JsonNode](Map(
//              "username" -> username,
//              "course_ids" -> courseIds,
//              "card_by_courses" -> cardByCourseMap
//            )))
//
//          }
//
//        })
//
//        writer.write(JsonUtils.toJson(data))
//        println(
//          s"""
//            |Total missing-card profiles: ${data.size}
//            |""".stripMargin)
//      }
//    }
//


  }


  def getAllUserProfileIds() = {
    import scala.collection.JavaConversions._
    val r = ssdb.hkeys("caas_profile","","",500000000)
    r.listString()
      .map(s => JsonUtils.fromJson[JsonNode](s))
      .map(_.at("/id").asText(null))
      .filter(_!=null)
  }


  def rebuildMissingDeck(): Unit = {


    val deckMap = scala.collection.mutable.Map.empty[String, DeckData]
    val cardMap = scala.collection.mutable.Map.empty[String, Card]
    val allDeckIds = ListBuffer.empty[String]

    //Get all card ids
    val allCardIds = cardRepo.getAllCardIds(500000000).sync()

    //Group deck with its cards
    allCardIds.grouped(100).foreach(cardIds => {
      cardRepo.getCards(cardIds)
        .sync()
        .foreach(card => {
          val username = card.username.get
          val deckId = card.deckId.get

          cardMap.put(card.id, card)

          deckMap.get(deckId) match {
            case Some(deckData) =>
              deckData.cardIds.append(card.id)
            case _ =>
              val deckData = DeckData(
                username,
                ListBuffer(card.id)
              )
              allDeckIds.append(deckId)
              deckMap.put(deckId, deckData)
          }
        })
    })

    //Remove exist decks
    allDeckIds.grouped(50).foreach(deckIds => {
      deckRepository.multiGet(deckIds)
        .sync()
        .map(_._1)
        .foreach(existDeckId => {
          deckMap.remove(existDeckId)
        })
    })

    val finalDecks = createMissingDeckInfo(deckMap, cardMap)
    FileUtils.writeStringToFile(new File("./data/recovered_decks.json"), JsonUtils.toJson(finalDecks))
    val totalRecoveredDecksSuccess = finalDecks.grouped(50).map(decks =>{
      deckRepository.multiInsert(decks, false).sync().size
    }).sum

    println(s"Total decks: ${allDeckIds.size}")
    println(s"Total missing decks: ${deckMap.size}")
    println(s"Total recovered decks: ${finalDecks.size}")
    println(s"Total succeess recovered decks: ${totalRecoveredDecksSuccess}")
    println(s"Total card: ${allCardIds.size}")


  }



  def createMissingDeckInfo(deckMap: mutable.Map[String, DeckData], cardMap: mutable.Map[String, Card]) : Seq[Deck] = {

    val finalDecks = deckMap.map(entry => {
      //Sort cards by created_time
      val sortedCardIds = entry._2.cardIds
        .map(cardMap.get)
        .filter(_.isDefined)
        .map(_.get)
        .sortBy(_.createdTime.get)(Ordering[Long])
        .map(_.id)
      Deck(
        id = entry._1,
        username = Option(entry._2.username),
        name = None,
        thumbnail = None,
        description = None,
        design = None,
        cards = Some(ListBuffer(sortedCardIds:_*)),
        deckStatus = Option(Status.PROTECTED.id),
        updatedTime = Some(System.currentTimeMillis()),
        createdTime = Some(System.currentTimeMillis())
      )
    }).groupBy(_.username.get)
      .flatMap(entry =>{
        val username = entry._1
        val decks = entry._2

        decks.zipWithIndex.map(e =>{
          e._1.copy(name = Some(s"Deck ${e._2+1}"))
        })
      })


    finalDecks.toSeq
  }


  private def searchCardScroll(client: TransportClient,
                           index: String,
                           fn:Seq[SearchHit] => Unit,
                           size: Int = 50): Boolean = {

    def recursiveSearch(r: SearchResponse, fn:Seq[SearchHit] => Unit ): Future[Boolean] = {
      val data = r.getHits.getHits

      fn(data)
      if(data.nonEmpty && r.getScrollId!= null) {
        client.prepareSearchScroll(r.getScrollId)
          .setScroll(TimeValue.timeValueMinutes(1L))
          .asyncGet()
          .flatMap(recursiveSearch(_,fn))

      } else {
        Future.value(true)
      }
    }


    val f =  client.prepareSearch(index)
      .setTypes("card")
      .setQuery(QueryBuilders.matchAllQuery())
      .setScroll(TimeValue.timeValueMinutes(1L))
      .setFrom(0)
      .setSize(size).asyncGet().flatMap(recursiveSearch(_, fn))

    f.sync()

  }

  private def createClient(clusterName: String, servers: Seq[String]) = {
    val client = new PreBuiltTransportClient(Settings.builder()
      .put("cluster.name", clusterName)
      .put("client.transport.sniff", "false")
      .build())

    servers.map(s => {
      val hostPort = s.split(":")
      (hostPort(0), hostPort(1).toInt)
    }).foreach(hostPort => {
      info(s"Add ${hostPort._1}:${hostPort._2} to transport address")
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostPort._1), hostPort._2))
    })
    client
  }

}
