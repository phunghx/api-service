package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finatra.http.Controller
import xed.api_service.service.NLPService
import xed.chatbot.domain._

/**
 * Created by phg on 2019-09-29.
 **/
case class NLPController@Inject()(nlpService: NLPService) extends Controller {

  get("/nlp/intents") {
    req: ListIntentRequest => nlpService.listIntents(req.getPageable, req.languageCode)
  }

  post("/nlp/intents") {
    req: CreateIntentRequest =>
      nlpService.createIntent(
        name = req.name,
        trainingPhrases = req.trainingPhrases,
        messageTexts = req.messageTexts,
        languageCode = req.languageCode
      )
  }

  delete("/nlp/intents/:project_id/:id") {
    req: DeleteRequest => nlpService.deleteIntent(intentId = req.id)
  }

  get("/nlp/entity_types") {
    req: ListEntityTypeRequest => nlpService.listEntityTypes()
  }

  post("/nlp/entity_types") {
    req: CreateEntityTypeRequest => nlpService.createEntityType(name = req.name, kind = req.kind)
  }
}
