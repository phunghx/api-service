//package xed.api_service.controller
//
//import com.fasterxml.jackson.databind.JsonNode
//import xed.api_service.controller.FakeData._
//import xed.api_service.domain._
//import xed.api_service.domain.request._
//import xed.api_service.util.JsonUtils
//
//import scala.collection.mutable.ListBuffer
//
//
//class DeckControllerTest extends ControllerTest {
//
//  val deckApi = "/deck"
//  val cardApi = "/card"
//  val srsApi = "/srs/card"
//
//  val testCardRequest = generateCardRequest()
//
//  val deckIds = ListBuffer.empty[String]
//  val addedCardIds = ListBuffer.empty[String]
//
//
//
//  test(testName = "Search deck by category id"){
//    val node: JsonNode = JsonUtils.toNode("deck_cat_vocabulary")
//    val request = SearchRequest(from = 0, size = 4)
//      .addIfNotExist(TermsQuery("category", ListBuffer(node)))
//    val r = server.httpPost(s"$deckApi/search",
//      postBody = JsonUtils.toJson(request),
//      headers = headers)
//
//
//    var response = JsonUtils.fromJson[JsonNode](r.getContentString())
//    assert(response.at("/success").asBoolean(false))
//  }
//
//  test("Create Deck") {
//
//    val request = CreateDeckRequest(
//      name = "Bộ sưu tập câu hỏi thi sát hạch lái xe A2",
//      description = Some("Chọn lọc năm 2018"),
//      thumbnail = Some("https://sample-videos.com/img/Sample-jpg-image-50kb.jpg"),
//      category = None,
//      deckStatus = None,
//      design = None
//    )
//
//    val r = server.httpPost(path = deckApi,
//      postBody = JsonUtils.toJson(request),
//      headers = headers
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//    val deck = JsonUtils.fromJson[Deck](response.at("/data").toString)
//
//    deckIds.append(deck.id)
//  }
//
//  test("Get Deck") {
//      deckIds.foreach( id => {
//        val r = server.httpGet(path = s"$deckApi/$id",
//          headers = headers
//        )
//
//        val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//        assert(response.at("/success").asBoolean(false))
//        val deck = JsonUtils.fromJson[Deck](response.at("/data").toString)
//        assert(deck!=null)
//      })
//  }
//
//  test("Edit Deck") {
//
//    val request = EditDeckRequest(
//      deckId = deckIds.head,
//      name = Some("Bộ sưu tập câu hỏi thi lái xe A2"),
//      thumbnail = Some("https://sample-videos.com/img/Sample-jpg-image-50kb.jpg"),
//      description = Some("Sưu tầm"),
//      category = None,
//      deckStatus = None,
//      design = None
//    )
//
//    val r = server.httpPut(path = s"$deckApi/${deckIds.head}",
//      putBody = JsonUtils.toJson(request),
//      headers = headers
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//
//  }
//
//  test("Get List Decks") {
//    val request = GetDeckRequest(deckIds = deckIds)
//    val r = server.httpPost(path = s"$deckApi/list",
//      headers = headers,
//      postBody = JsonUtils.toJson(request)
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//    val decks = JsonUtils.fromJson[Map[String,Deck]](response.at("/data").toString)
//    assert(decks.nonEmpty)
//  }
//
//  test("Search deck") {
//
//    val request =  SearchRequest()
//   //   .addIfNotExist(MatchQuery("query","100 common"))
////      .addIfNotExist(MatchQuery("name","silent: A,B,C"))
//      .addIfNotExist(SortQuery("created_time",SortQuery.ORDER_ASC))
//
//
//    val r = server.httpPost(path = s"$deckApi/search?query=cau",
//      headers = headers,
//      postBody = JsonUtils.toJson(request)
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//
//    val decks = JsonUtils.fromJson[Seq[Deck]](response.at("/data/records").toString)
//
//    assert(decks.nonEmpty)
//  }
//
//  test("Add single card to deck") {
//
//    deckIds.foreach( deckId => {
//      val request: AddCardRequest = AddCardRequest(
//        deckId = deckId,
//        cardVersion = testCardRequest.head.cardVersion,
//        design = testCardRequest.head.design
//      )
//
//      val r = server.httpPost(path = s"$deckApi/$deckId/card",
//        headers = headers,
//        postBody = JsonUtils.toJson(request)
//      )
//
//      val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//      assert(response.at("/success").asBoolean(false))
//      val card = JsonUtils.fromJson[String](response.at("/data").toString)
//      assert(card!=null)
//      addedCardIds.append(card)
//    })
//  }
//
//  test("Add multi cards to deck") {
//
//
//
//    deckIds.foreach( deckId => {
//      val request = AddMultiCardRequest(
//        deckId = deckId,
//        cards =  testCardRequest
//      )
//
//      val r = server.httpPost(path = s"$deckApi/$deckId/cards",
//        headers = headers,
//        postBody = JsonUtils.toJson(request)
//      )
//
//      val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//      assert(response.at("/success").asBoolean(false))
//      val cardIds = JsonUtils.fromJson[Seq[String]](response.at("/data").toString)
//      assert(cardIds!=null)
//      addedCardIds.appendAll(cardIds)
//    })
//  }
//
//  test("Get Card") {
//    addedCardIds.foreach(cardId => {
//      val r = server.httpGet(path = s"$cardApi/$cardId",
//        headers = headers
//      )
//
//      val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//      assert(response.at("/success").asBoolean(false))
//    })
//  }
//
//  test("Get Card with SRS Model") {
//    val request = GetCardRequest(cardIds = addedCardIds)
//    val r = server.httpPost(path = s"$cardApi/detail/list",
//      headers = headers,
//      postBody = JsonUtils.toJson(request)
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//
//    val srsCards = JsonUtils.fromJson[Seq[SRSCard]](response.at("/data").toString)
//
//    assert(srsCards.nonEmpty)
//
//  }
//
//  test("Get Card with SRS Model as Tab") {
//    val request = GetCardRequest(cardIds = addedCardIds)
//    val r = server.httpPost(path = s"$cardApi/detail/tab",
//      headers = headers,
//      postBody = JsonUtils.toJson(request)
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//
//    val srsCards = JsonUtils.fromJson[Map[String,Seq[SRSCard]]](response.at("/data").toString)
//
//    assert(srsCards.nonEmpty)
//
//  }
//
//  test("Edit Card") {
//
//    addedCardIds.foreach(cardId => {
//      val request = EditCardRequest(
//        cardId = cardId,
//        design = testCardRequest.headOption.flatMap(_.design)
//      )
//      val r = server.httpPut(path = s"$cardApi/$cardId",
//        headers = headers,
//        putBody = JsonUtils.toJson(request)
//      )
//
//      val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//      assert(response.at("/success").asBoolean(false))
//    })
//  }
//
//  test("Get List Card") {
//    val request = GetCardRequest(cardIds = addedCardIds)
//    val r = server.httpPost(path = s"$cardApi/list",
//      headers = headers,
//      postBody = JsonUtils.toJson(request)
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//    val cards = JsonUtils.fromJson[Map[String,Card]](response.at("/data").toString)
//    assert(cards.nonEmpty)
//  }
//
//  test("Get Deck After add card") {
//    deckIds.foreach( id => {
//      val r = server.httpGet(path = s"$deckApi/$id",
//        headers = headers
//      )
//
//      val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//      assert(response.at("/success").asBoolean(false))
//      val deck = JsonUtils.fromJson[Deck](response.at("/data").toString)
//      assert(deck!=null)
//
//      val cardIds = deck.cards.getOrElse(Nil)
//      val sameCardIds =cardIds.intersect(addedCardIds)
//
//      assert(cardIds.size == sameCardIds.size)
//
//    })
//  }
//
//  test("Search learning cards") {
//    Thread.sleep(2000)
//    val request =  SearchRequest()
//      .addIfNotExist(SortQuery("created_time",SortQuery.ORDER_DESC))
//
//    val r = server.httpPost(path = s"$srsApi/search/learning",
//      headers = headers,
//      postBody = JsonUtils.toJson(request)
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//
//    val learningCards = JsonUtils.fromJson[Seq[ReviewInfo]](response.at("/data/records").toString)
//
//    assert(learningCards.nonEmpty)
//  }
//
//  test("Get due cards") {
//    Thread.sleep(3000)
//    val request =  SearchRequest()
//
//    val r = server.httpPost(path = s"$srsApi/search/due",
//      headers = headers,
//      postBody = JsonUtils.toJson(request)
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//
//    val learningCards = JsonUtils.fromJson[Seq[ReviewInfo]](response.at("/data/records").toString)
//
//    assert(learningCards.nonEmpty)
//  }
//
//
//  test("Unpublish deck") {
//
//    deckIds.foreach( deckId => {
//      val r = server.httpPost(path = s"$deckApi/$deckId/unpublish",
//        headers = headers,
//        postBody = ""
//      )
//
//      val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//      assert(response.at("/success").asBoolean(false))
//    })
//  }
//
//
//  test("Delete & Cleanup decks") {
//    deckIds.foreach(x => deleteDeck(x))
//  }
//
//  test("Delete & Cleanup cards") {
//    addedCardIds.foreach(x => deleteCard(x))
//  }
//
//  test("Get deck categories"){
//    val r = server.httpGet(s"$deckApi/category/list",
//      headers = headers)
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//    assert(response.at("/success").asBoolean(false))
//  }
//
//  private def deleteDeck(id: String) = {
//    val r = server.httpDelete(path = s"$deckApi/${id}",
//      headers = headers
//    )
//
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//
//    assert(response.at("/success").asBoolean(false))
//  }
//
//  private def deleteCard(id: String) = {
//    val r = server.httpDelete(path = s"$cardApi/${id}",
//      headers = headers
//    )
//    val response = JsonUtils.fromJson[JsonNode](r.getContentString())
//    assert(response.at("/success").asBoolean(false))
//  }
//
//}
