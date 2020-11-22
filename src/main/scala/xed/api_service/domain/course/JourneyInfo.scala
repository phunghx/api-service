package xed.api_service.domain.course

import xed.api_service.repository.ElasticsearchObject
import xed.api_service.util.JsonUtils
import xed.userprofile.domain.ShortUserProfile

import scala.collection.mutable.ListBuffer




case class JourneyInfo(id: String,
                       name: Option[String],
                       thumbnail: Option[String],
                       description: Option[String],
                       deckIds: Option[ListBuffer[String]],
                       creator: Option[String],
                       status: Option[Int],
                       var updatedTime: Option[Long],
                       var createdTime: Option[Long],
                       var creatorDetail: Option[ShortUserProfile] = None) extends ElasticsearchObject {

  override def esId: String = id

  override def esSource: String = JsonUtils.toJson(this)
}