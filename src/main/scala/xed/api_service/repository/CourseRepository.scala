package xed.api_service.repository

import org.elasticsearch.client.transport.TransportClient
import xed.api_service.domain.course.CourseInfo

case class CourseRepository(client: TransportClient,
                             config: ESConfig,
                             esType: String) extends AbstractESRepository[CourseInfo]
