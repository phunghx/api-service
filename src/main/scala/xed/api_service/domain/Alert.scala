package xed.api_service.domain

import xed.userprofile.domain.ShortUserProfile

/**
 * @author andy
 * @since 6/9/20
 **/
object AlertTypes {
  final val REVIEW = "review_alert"

}

case class Alert(id: Int,
                 alertType: String,
                 recipient: String,
                 title: String,
                 description: Option[String],
                 fields: Map[String,Any],
                 recipientDetail: Option[ShortUserProfile] = None) extends  Serializable

