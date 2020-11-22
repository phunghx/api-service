package xed.chatbot.domain

import com.twitter.finatra.request.RouteParam
import xed.api_service.domain.{PageNumberRequest, Pageable}

/**
 * Created by phg on 2019-09-29.
 **/
//  def listIntents(projectId: String, pageable: Pageable = PageTokenRequest(), languageCode: String = "en-US"): Page[Intent]
//  def createIntent(name: String, projectId: String, trainingPhrases: Seq[String], messageTexts: Seq[String], languageCode: String = "en-US"): Intent
//  def deleteIntent(projectId: String, intentId: String): Unit
//
//  def listEntityTypes(projectId: String): Seq[EntityType]
//  def createEntityType(projectId: String, name: String, kind: String): EntityType
//  def deleteEntityType(projectId: String, entityTypeId: String): Unit
//
//  def listEntities(projectId: String, entityTypeId: String, languageCode: String = "en-US"): Seq[Entity]
//  def createEntity(projectId: String, entityTypeId: String, entityValue: String, synonyms: Seq[String], languageCode: String = "en-US"): Unit
//  def deleteEntity(projectId: String, entityTypeId: String, entityValue: String, languageCode: String = "en-US"): Unit

case class ListIntentRequest(projectId: String, page: Int = 1, size: Int = 10, languageCode: String) {
  def getPageable: Pageable = PageNumberRequest(page, size)
}

case class CreateIntentRequest(projectId: String, languageCode: String, name: String, trainingPhrases: Seq[String], messageTexts: Seq[String])

case class ListEntityTypeRequest(projectId: String)

case class CreateEntityTypeRequest(projectId: String, name: String, kind: String)

case class ListEntityRequest(projectId: String, languageCode: String)

case class CreateEntityRequest(projectId: String, languageCode: String)

case class DeleteRequest(
  @RouteParam projectId: String,
  @RouteParam id: String
)
