package xed.api_service.service

import java.net.InetAddress
import java.util.concurrent.atomic.LongAdder

import com.google.inject.Guice
import com.twitter.inject.{Injector, IntegrationTest}
import com.twitter.util.Future
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.client.PreBuiltTransportClient
import xed.api_service.domain._
import xed.api_service.domain.request.SearchRequest
import xed.api_service.module.XedApiModule
import xed.api_service.repository.ESRepository.ZActionRequestBuilder
import xed.api_service.repository.{DeckRepository, ESRepository, SRSRepository}
import xed.api_service.util.Implicits.FutureEnhance


class UpdateDeckSRSTool extends IntegrationTest {

   val BUFFER_SIZE = 20
   val CARD_PER_DECK = 50
   val MAX_CARD_COUNT = 150

  override protected def injector: Injector =  Injector(Guice.createInjector(Seq(XedApiModule):_*))

  private val devClient = createClient(clusterName = "es_dev_cluster", servers = Seq("3.0.226.54:9300"))
  private val prodClient = createClient(clusterName = "xed_live_cluster", servers = Seq("3.1.242.19:9300"))
  private val client = devClient


  private val deckRepository = injector.instance[DeckRepository]
  private val cardService = injector.instance[CardService]
  private val reviewRepository = injector.instance[SRSRepository]
  val counter = new LongAdder()


  test("Update deck ids for vocabulary category") {
    val deckIds = List.range(0, 200).map(i => s"xed_vocabulary_$i")
    val decks = deckIds.map(deckId => {
      Deck(
        id = deckId,
        category = Some("deck_cat_vocabulary"),
        username = None,
        name = None,
        thumbnail = None,
        description = None,
        design = None,
        cards = None,
        updatedTime = Some(System.currentTimeMillis())
      )
    })
    val r = deckRepository.multiUpdate(decks).sync()

    println(s"Update results: $r")
  }

  test("Backup deck") {
    val fromIndex = "deck"
    val toIndex = s"${fromIndex}_backup"
    val batchSize = 50

    // backupIndex(client, fromIndex, toIndex, batchSize)
    //backupIndex(client, toIndex, fromIndex, batchSize)
  }


  protected def backupIndex(client: TransportClient, fromIndex: String, toIndex: String, batchSize: Int) = {
    def searchScroll(client: TransportClient,
                               index: String,
                               searchRequest: SearchRequest,
                               fn: Seq[SearchHit] => Unit): Boolean = {

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


      val (query,_) = ESRepository.buildESQuery(searchRequest)

      val f =  client.prepareSearch(index)
        .setQuery(query)
        .setScroll(TimeValue.timeValueMinutes(1L))
        .addSort("updated_time",SortOrder.ASC)
        .setFrom(0)
        .setSize(batchSize).asyncGet().flatMap(recursiveSearch(_, fn))

      f.sync()

    }
    val counter = new LongAdder()
    val successCounter = new LongAdder()
    val failedCounter = new LongAdder()

    def backupToIndex(client: TransportClient, index: String, searchHits: Seq[SearchHit]) = {
      val bulk = searchHits.foldLeft(client.prepareBulk())((bulk,searchHit) => {
        bulk.add(new IndexRequest(index,searchHit.getType, searchHit.getId).source(searchHit.getSourceAsString,XContentType.JSON))
      })

      if(bulk.numberOfActions() > 0) {
        bulk.asyncGet().map(response => {
          val successCount = response.getItems.filterNot(_.isFailed).size
          val failCount = response.getItems.filter(_.isFailed).size
          response.getItems.filter(_.isFailed).map(_.getFailureMessage).foreach(s => {
            println(s)
          })
          successCounter.add(successCount)
          failedCounter.add(failCount)
        }).sync()
      }

      println(s"${successCounter}/${failedCounter}/$counter")
    }

    searchScroll(client, fromIndex,SearchRequest(size = batchSize), searchHits => backupToIndex(client,toIndex,searchHits))
  }


  protected def createClient(clusterName: String, servers: Seq[String]) = {
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
