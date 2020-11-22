package xed.api_service.repository

import org.elasticsearch.client.transport.TransportClient
import xed.api_service.domain.ReviewHistoryInfo

case class CountRepository(esType: String,
                           client: TransportClient,
                           config: ESConfig) extends AbstractESRepository[ReviewHistoryInfo] {

}

