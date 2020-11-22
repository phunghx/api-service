package xed.api_service.repository

import org.elasticsearch.client.transport.TransportClient
import xed.api_service.domain.course.{CategoryInfo, JourneyInfo}


case class CategoryRepository(client: TransportClient,
                              config: ESConfig,
                              esType: String) extends AbstractESRepository[CategoryInfo] {
}

case class JourneyRepository(client: TransportClient,
                             config: ESConfig,
                             esType: String) extends AbstractESRepository[JourneyInfo]
