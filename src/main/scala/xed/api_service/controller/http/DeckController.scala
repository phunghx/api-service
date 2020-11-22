package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.thrift.thriftscala.ClientErrorCause.Unauthorized
import com.twitter.util.Future
import xed.api_service.controller.http.filter.DataRequestContext.MainRequestContextSyntax
import xed.api_service.controller.http.filter.LoggedInUserFilter
import xed.api_service.controller.http.filter.parser.{GlobalDeckSearchRequest, SearchDeckRequestParser}
import xed.api_service.domain.Deck
import xed.api_service.domain.exception.UnAuthorizedError
import xed.api_service.domain.request._
import xed.api_service.domain.response.PageResult
import xed.api_service.service.{Analytics, DeckService, Operation}
import xed.profiler.Profiler
import xed.userprofile.domain.ShortUserProfile
import xed.userprofile.domain.UserProfileImplicits.UserProfileWrapper
import xed.userprofile.{SessionHolder, UserProfileService}

case class DeckController @Inject()(
  deckService: DeckService,
  profileService: UserProfileService,
  analytics: Analytics,
  sessionHolder: SessionHolder
) extends XController {
  private val apiPath = "/deck"

  filter[LoggedInUserFilter].post(s"$apiPath/:deck_id/card") {
    request: AddCardRequest =>
      Profiler(s"$clazz.addCard") {
        deckService.addCard(sessionHolder.getUser, request)
      }
  }

  filter[LoggedInUserFilter].post(s"$apiPath/add_news_card") {
    request: AddNewsCardRequest => {
      Profiler(s"$clazz.addNewsCard") {
        deckService.addCardToNewsDeck(sessionHolder.getUser, request)
      }
    }
  }

  filter[LoggedInUserFilter].post(s"$apiPath/:deck_id/cards") {
    request: AddMultiCardRequest =>
      Profiler(s"$clazz.addCards") {
        deckService.addMultiCards(sessionHolder.getUser, request)
      }
  }

  filter[LoggedInUserFilter].post(s"$apiPath/:deck_id/remove_cards") {
    request: RemoveMultiCardRequest =>
      Profiler(s"$clazz.removeCards") {
        deckService.removeCards(sessionHolder.getUser, request)
      }
  }

  filter[LoggedInUserFilter].post(s"$apiPath") {
    request: CreateDeckRequest =>
      Profiler(s"$clazz.createDeck") {
        deckService.createDeck(sessionHolder.getUser, request).flatMap(enhanceOwnerDetail(_))
      }
  }

  filter[LoggedInUserFilter].put(s"$apiPath/:deck_id") {
    request: EditDeckRequest =>
      Profiler(s"$clazz.editDeck") {
        deckService.updateDeck(sessionHolder.getUser, request)
      }
  }


  post(s"$apiPath/set_likes") {
    request: ChangeLikeRequest =>
      Profiler(s"$clazz.changeLikes") {
        request.sk.equals(secretKey) match {
          case true => deckService.multiUpdate(request.build())
          case _ => Future.exception(UnAuthorizedError())
        }
      }
  }

  post(s"$apiPath/set_weekly_likes") {
    request: ChangeWeekyLikeRequest =>
      Profiler(s"$clazz.changeWeeklyLikes") {
        request.sk.equals(secretKey) match {
          case true => deckService.multiUpdate(request.build())
          case _ => Future.exception(UnAuthorizedError())
        }
      }
  }

  filter[LoggedInUserFilter].get(s"$apiPath/:deck_id") {
    request: Request =>
      Profiler(s"$clazz.getDeck") {
        val deckId = request.getParam("deck_id")
        analytics.log(Operation.VIEW_DECK, sessionHolder.getUser.userProfile, Map(
          "func" -> s"$apiPath/:deck_id"
        ))
        deckService.getDeck(deckId).flatMap(enhanceOwnerDetail(_))
      }
  }

  filter[LoggedInUserFilter].post(s"$apiPath/list") {
    request: GetDeckRequest =>
      Profiler(s"$clazz.getDecks") {
        deckService.getDeckAsMap(sessionHolder.getUser, request.deckIds).flatMap(enhanceOwnerDetails)
      }
  }

  filter[LoggedInUserFilter].post(s"$apiPath/:deck_id/unpublish") {
    request: Request =>
      Profiler(s"$clazz.unpublish") {
        val deckId = request.getParam("deck_id")
        deckService.unpublishDeck(sessionHolder.getUser, deckId)
      }
  }

  filter[LoggedInUserFilter].post(s"$apiPath/:deck_id/publish") {
    request: Request =>
      Profiler(s"$clazz.publish") {
        val deckId = request.getParam("deck_id")
        deckService.publishDeck(sessionHolder.getUser, deckId)
      }
  }

  filter[LoggedInUserFilter].post(s"$apiPath/multi_del") {
    request: GetDeckRequest =>
      Profiler(s"$clazz.deleteDecks") {
        deckService.temporaryDeleteDecks(sessionHolder.getUser, request.deckIds)
      }
  }

  filter[LoggedInUserFilter].delete(s"$apiPath/:deck_id/del") {
    request: Request =>
      Profiler(s"$clazz.deleteDeck") {
        val deckId = request.getParam("deck_id")
        deckService.temporaryDeleteDeck(sessionHolder.getUser, deckId)
      }
  }

  filter[LoggedInUserFilter].delete(s"$apiPath/:deck_id") {
    request: Request =>
      Profiler(s"$clazz.hardDeleteDeck") {
        val deckId = request.getParam("deck_id")
        deckService.deleteDeck(sessionHolder.getUser, deckId)
      }
  }


  filter[LoggedInUserFilter].get(s"$apiPath/list/new") {
    request: Request =>
      Profiler(s"$clazz.getNewDecks") {
        deckService.getNewDecks(sessionHolder.getUser).flatMap(enhanceOwnerDetails)
      }
  }

  filter[LoggedInUserFilter].get(s"$apiPath/trending") {
    request: Request =>
      Profiler(s"$clazz.getTrendingDecks") {
        deckService.getTrendingDecks(sessionHolder.getUser, request.getIntParam("size", 20)).flatMap(enhanceOwnerDetails)
      }
  }

  filter[LoggedInUserFilter].filter[SearchDeckRequestParser]
    .post(s"$apiPath/search") {
      request: Request =>
        Profiler(s"$clazz.searchGlobalDeck") {
          val searchRequest = request.requestData[GlobalDeckSearchRequest]
          deckService.search(sessionHolder.getUser, searchRequest.query, searchRequest.searchRequest)
            .flatMap(r => enhanceOwnerDetails(r.records).map(PageResult(r.total, _)))
        }
    }


  filter[LoggedInUserFilter].post(s"$apiPath/search/me") {
    request: SearchRequest =>
      Profiler(s"$clazz.searchMyDeck") {
        deckService.searchMyDeck(sessionHolder.getUser, request)
          .flatMap(r => enhanceOwnerDetails(r.records).map(PageResult(r.total, _)))
      }
  }

  filter[LoggedInUserFilter].get(s"$apiPath/category/list") {
    _: Request =>
      Profiler(s"$clazz.getCategories") {
        deckService.getDeckCategories()
      }
  }


  private def enhanceOwnerDetail(deck: Deck): Future[Deck] = {
    for {
      userProfile <- if (deck.username.isDefined) profileService.getProfile(deck.username.get)
      else Future.value(None)
      _ = {
        deck.ownerDetail = userProfile.map(_.toShortProfile)
      }

    } yield deck
  }


  private def enhanceOwnerDetails(decks: Seq[Deck]): Future[Seq[Deck]] = {
    val userNames = decks.flatMap(_.username)

    val injectFn = (requests: Seq[Deck], users: Map[String, ShortUserProfile]) => {
      requests.foreach(request => {
        if (request.username.isDefined) {
          request.ownerDetail = users.get(request.username.get)
        }
      })
      requests
    }

    for {
      userProfiles <- profileService.getProfiles(userNames)
      shortProfiles = userProfiles.map(e => e._1 -> e._2.toShortProfile)
      r = injectFn(decks, shortProfiles)
    } yield r
  }


  private def enhanceOwnerDetails(decks: Map[String, Deck]): Future[Map[String, Deck]] = {
    val userNames = decks.flatMap(_._2.username).toSeq

    val injectFn = (requests: Map[String, Deck], users: Map[String, ShortUserProfile]) => {
      requests.foreach(request => {
        if (request._2.username.isDefined) {
          request._2.ownerDetail = users.get(request._2.username.get)
        }
      })
      requests
    }

    for {
      userProfiles <- profileService.getProfiles(userNames)
      shortProfiles = userProfiles.map(e => e._1 -> e._2.toShortProfile)
      r = injectFn(decks, shortProfiles)
    } yield r
  }
}
