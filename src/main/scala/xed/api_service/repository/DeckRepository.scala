package xed.api_service.repository

import com.twitter.util.Future
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.index.query.{Operator, QueryBuilders}
import org.elasticsearch.search.sort.SortBuilders
import xed.api_service.domain.Deck
import xed.api_service.domain.request.SearchRequest
import xed.api_service.domain.response.PageResult
import xed.api_service.repository.ESRepository.ZActionRequestBuilder
import xed.api_service.util.JsonUtils


case class DeckRepository(client: TransportClient,
                          config: ESConfig,
                          esType: String) extends AbstractESRepository[Deck] {

  def search(query: Option[String], searchRequest: SearchRequest): Future[PageResult[Deck]] =  {
    val (builder, sorts) = ESRepository.buildESQuery(searchRequest)

    val queryBuilder = query match {
      case Some(query) if query.nonEmpty =>
        QueryBuilders.queryStringQuery(s"*${ESRepository.escape(query)}*")
          .field("name", 20.0f)
          .field("description", 1.5f)
          .allowLeadingWildcard(true)
          .analyzeWildcard(true)
          .defaultOperator(Operator.OR)
      case _ => QueryBuilders.matchAllQuery()
    }



    val req = prepareSearch.setTypes(esType)
      .setQuery(QueryBuilders.boolQuery()
        .must(queryBuilder)
        .must(builder)
      ).setFrom(searchRequest.from)
      .setSize(searchRequest.size)


    if(sorts.nonEmpty) {
      req.addSort(SortBuilders.scoreSort())
      sorts.foreach(req.addSort)
    }

    req.asyncGet().map(r => {
      val data = r.getHits.getHits.map(h => JsonUtils.fromJson[Deck](h.getSourceAsString))
      PageResult[Deck](r.getHits.getTotalHits, data)
    })
  }

}




