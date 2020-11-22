package xed.api_service.domain.course

import xed.api_service.repository.ElasticsearchObject
import xed.api_service.util.JsonUtils

case class CategoryInfo(id: String,
                        name: Option[String],
                        var documentStatus: Option[Int],
                        var updatedTime: Option[Long],
                        var createdTime: Option[Long]) extends ElasticsearchObject {

  override def esId: String = id

  override def esSource: String = JsonUtils.toJson(this)
}