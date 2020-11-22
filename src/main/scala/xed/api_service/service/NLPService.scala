package xed.api_service.service

import java.io.FileInputStream

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.dialogflow.v2beta1.EntityType.{Entity, Kind}
import com.google.cloud.dialogflow.v2beta1.Intent.Message.Text
import com.google.cloud.dialogflow.v2beta1.Intent.TrainingPhrase.Part
import com.google.cloud.dialogflow.v2beta1.Intent.{Message, TrainingPhrase}
import com.google.cloud.dialogflow.v2beta1._
import com.google.inject.Inject
import xed.api_service.domain.{Page, PageImpl, PageTokenRequest, Pageable}

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

/**
 * Created by phg on 2019-09-14.
 **/
trait NLPService {

  def detectIntentText(sessionId: String, contexts: Seq[Context] = Seq.empty, text: String, languageCode: String):  Option[QueryResult]

  def listIntents(pageable: Pageable = PageTokenRequest(), languageCode: String): Page[Intent]

  def createContext(sessionId: String, name: String, lifeSpanCount: Int =5 ): Context

  def createIntent(name: String, trainingPhrases: Seq[String], messageTexts: Seq[String], languageCode: String): Intent

  def deleteIntent(intentId: String): Unit

  def listEntityTypes(): Seq[EntityType]

  def createEntityType(name: String, kind: String): EntityType

  def deleteEntityType(entityTypeId: String): Unit

  def listEntities(entityTypeId: String, languageCode: String): Seq[Entity]

  def createEntity(entityTypeId: String, entityValue: String, synonyms: Seq[String], languageCode: String): Unit

  def deleteEntity(entityTypeId: String, entityValue: String, languageCode: String): Unit
}

case class DialogFlowNLPService @Inject()(projectId: String, credentialFile: String) extends NLPService {

  private val credentials = GoogleCredentials
    .fromStream(new FileInputStream(credentialFile))
    .createScoped("https://www.googleapis.com/auth/cloud-platform")

  private val sessionClient: SessionsClient = SessionsClient.create(
    SessionsSettings.newBuilder()
      .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
      .build()
  )

  private val contextClient = ContextsClient.create(
    ContextsSettings.newBuilder()
      .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
      .build()
  )

  private val intentsClient: IntentsClient = IntentsClient.create(
    IntentsSettings.newBuilder()
      .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
      .build()
  )

  private val entityTypeClient: EntityTypesClient = EntityTypesClient.create(
    EntityTypesSettings.newBuilder()
      .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
      .build()
  )


  override def detectIntentText(sessionId: String,
                                contexts: Seq[Context] = Seq.empty,
                                text: String,
                                languageCode: String): Option[QueryResult] = {
    try {
      val request = DetectIntentRequest.newBuilder
        .setSession(SessionName.of(projectId, sessionId).toString)
        .setQueryInput(QueryInput.newBuilder().setText(
          TextInput.newBuilder().setText(text).setLanguageCode(languageCode)
        ).build())
        .setQueryParams(QueryParameters.newBuilder().addAllContexts(contexts))
        .build()
      val result = sessionClient.detectIntent(request).getQueryResult
      Some(result)

    } catch {
      case NonFatal(e) => e.printStackTrace()
        None
    }
  }

  override def createContext(sessionId: String, name: String, lifeSpanCount: Int =5 ): Context = {
    Context.newBuilder().setName(
      s"projects/$projectId/agent/sessions/$sessionId/contexts/$name"
    ).setLifespanCount(lifeSpanCount)
      .build()

  }


  override def listIntents(pageable: Pageable = PageTokenRequest(), languageCode: String): Page[Intent] = {
    val request = ListIntentsRequest.newBuilder()
      .setParent(Option(ProjectAgentName.of(projectId)).map(_.toString).orNull)
    if (pageable.size > 0) request.setPageSize(pageable.size)
    pageable.nextToken.map(token => request.setPageToken(token))
    request.setLanguageCode(languageCode)
    val response = intentsClient.listIntents(ProjectAgentName.of(projectId))
    PageImpl(response.iterateAll().toArray, pageable, response.getPage.getPageElementCount, nextToken = Option(response.getNextPageToken))
  }

  override def createIntent(name: String, trainingPhrases: Seq[String], messageTexts: Seq[String], languageCode: String): Intent = {


    intentsClient.createIntent(
      ProjectAgentName.of(projectId),
      Intent.newBuilder().setDisplayName(name)
        .addMessages(
          Message.newBuilder().setText(Text.newBuilder().addAllText(messageTexts).build())
        ).addAllTrainingPhrases(
        trainingPhrases.map(phrase => {
          TrainingPhrase.newBuilder().addParts(Part.newBuilder().setText(phrase).build()).build()
        })
      ).build(),
      languageCode
    )
  }

  override def deleteIntent(intentId: String): Unit = intentsClient.deleteIntent(IntentName.of(projectId, intentId))

  override def listEntityTypes(): Seq[EntityType] = entityTypeClient.listEntityTypes(ProjectAgentName.of(projectId)).iterateAll().toSeq

  override def createEntityType(name: String, kind: String): EntityType = {
    entityTypeClient.createEntityType(ProjectAgentName.of(projectId),
      EntityType.newBuilder()
        .setDisplayName(name)
        .setKind(Kind.valueOf(kind))
        .build()
    )
  }

  override def deleteEntityType(entityTypeId: String): Unit = entityTypeClient.deleteEntityType(EntityTypeName.of(projectId, entityTypeId))

  override def listEntities(entityTypeId: String, languageCode: String): Seq[Entity] = {
    val entityType = entityTypeClient.getEntityType(EntityTypeName.of(projectId, entityTypeId))
    entityType.getEntitiesList
  }

  override def createEntity(entityTypeId: String, entityValue: String, synonyms: Seq[String], languageCode: String): Unit = {
    val entity = Entity.newBuilder()
      .setValue(entityValue)
      .addAllSynonyms(synonyms)
      .build()
    entityTypeClient.batchCreateEntitiesAsync(EntityTypeName.of(projectId, entityTypeId), List(entity), languageCode).get()
  }

  override def deleteEntity(entityTypeId: String, entityValue: String, languageCode: String): Unit = {
    entityTypeClient.batchDeleteEntitiesAsync(EntityTypeName.of(projectId, entityTypeId), List(entityValue), languageCode)
  }

}