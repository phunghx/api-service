package chatbot

import java.util.UUID

import xed.api_service.service.{DialogFlowNLPService, NLPService}

/**
 * Created by phg on 2019-09-14.
 **/
object TestDialogFlow {
  private val PROJECT_ID = "xed-v2"
  private val GOOGLE_APPLICATION_CREDENTIALS = "./conf/xed-v2-ef3c84d78341.json"

  private val service: NLPService = DialogFlowNLPService(PROJECT_ID, GOOGLE_APPLICATION_CREDENTIALS)

  private val sessionId: String = UUID.randomUUID().toString
  private val languageCode: String = "en"


  def main(args: Array[String]): Unit = {
    test()
  }


  private def test(): Unit = {
    val text = "How are you?"
    val result = service.detectIntentText(UUID.randomUUID().toString, contexts = Seq.empty, text, languageCode)

    result.foreach(result =>{
      println("Detect intent text:")
      println(s"${result.getIntent.getDisplayName} - ${result.getIntentDetectionConfidence}")
      println(s" - $text")
      println(s" - ${result.getFulfillmentText}")
    })
  }
}
