package xed.chatbot.domain

import com.google.cloud.dialogflow.v2beta1.QueryResult

/**
 * Created by phg on 2019-09-14.
 **/
case class DetectionIntent(result: QueryResult)  {

  def confidence: Float = result.getIntentDetectionConfidence

  def fulfillmentText: String = result.getFulfillmentText

  def isNotFound: Boolean = isEmpty

  def isEmpty: Boolean = result == null || name.isEmpty

  def name: String = result.getIntent.getDisplayName
}
