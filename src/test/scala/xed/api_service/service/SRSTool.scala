package xed.api_service.service

import java.net.URLEncoder

import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.Guice
import com.twitter.inject.{Injector, IntegrationTest}
import scalaj.http
import xed.api_service.module.XedApiModule
import xed.api_service.util.Implicits.FutureEnhance
import xed.api_service.util.JsonUtils
import xed.userprofile.SignedInUser
import xed.userprofile.domain.UserProfile

class SRSTool extends IntegrationTest {


  override protected def injector: Injector =  Injector(Guice.createInjector(Seq(XedApiModule):_*))

  private val deckService = injector.instance[DeckService]

  val email = "sang@x.education"
  var user: SignedInUser = SignedInUser(
          session = "",
          username = "up-ac097a28-a459-4686-9992-202dc5371a0e"
        )


  test("Unpublish decks") {
    val deckIds = List.range(0,200).map(i => s"xed_vocabulary_$i")

    deckIds.foreach(deckId => {
      deckService.unpublishDeck(user,deckId).sync()
    })
  }

//  test("Reset review") {
//    val profile = getUserProfile(email)
//
//    user  = SignedInUser(
//      session = "",
//      username = profile.username
//    )
//    val result = srsService.searchLearningCards(user,new SearchRequest(size = 2000)).sync()
//
//    val cardIds = result.records.map(_.cardId)
//
//    cardIds.foreach(cardId => {
//      val r = srsService.delete(user,cardId).sync()
//      println(s"$cardId -> $r")
//    })
//
//    println(s"Data:\n $result")
//  }

//  test("Add Card to review") {
//    val deck = deckService.getDeck(deckId).sync()
//    val cardIds = deck.cards.getOrElse(Nil)
//
//    val response = srsService.multiAdd(user, cardIds).sync();
//
//    println(s"Add: ${response.size} cards to SRS for user $email")
//  }


  private def getUserProfile(email: String) = {

    val response = http.Http(s"http://xed.ai/api/user/profile/email/${URLEncoder.encode(email,"utf-8")}").asString
    val data = JsonUtils.fromJson[JsonNode](response.body)

    if(data.at("/success").asBoolean(false)) {
      JsonUtils.fromJson[UserProfile](data.at("/data").toString)
    }else {
      throw  new Exception(s"Fail to get profile")
    }
  }

}
