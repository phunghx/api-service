package xed.api_service.repository

import com.twitter.util.Future
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.histogram.{DateHistogramInterval, Histogram, InternalDateHistogram}
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum
import xed.api_service.domain.ReviewHistoryInfo
import xed.api_service.domain.metric.{Entry, LineData}
import xed.api_service.repository.ESRepository.ZActionRequestBuilder

import scala.collection.JavaConversions._

case class ReviewHistoryRepository(client: TransportClient,
                                   config: ESConfig,
                                   esType: String) extends AbstractESRepository[ReviewHistoryInfo] {


  def getReviewTimeChart(username: String,
                         interval: Int,
                         beginTime: Long, endTime: Long): Future[LineData] = {

    prepareSearch.setTypes(esType)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("username",username))
        .must(QueryBuilders.rangeQuery("created_time").gte(beginTime).lt(endTime))
        .must(QueryBuilders.termQuery("success",true)))
      .addAggregation(
        AggregationBuilders.dateHistogram("date_agg")
          .field("created_time")
          .order(Histogram.Order.KEY_ASC)
          .dateHistogramInterval(DateHistogramInterval.minutes(interval))
          .subAggregation(AggregationBuilders.sum("duration_agg").field("duration"))
      )

      .addAggregation(AggregationBuilders.sum("total_duration_agg").field("duration"))
      .setSize(0)
      .asyncGet().map(r => {

      val totalDuration = (r.getAggregations.get[InternalSum]("total_duration_agg")).getValue.toLong
      val agg = r.getAggregations.get[InternalDateHistogram]("date_agg")
      val metrics = parseCountMetric(agg)

      LineData("review_time",totalDuration, metrics)
    })
  }


  private def parseCountMetric(histogram: InternalDateHistogram) = {
    histogram.getBuckets.map(bucket => {
      val time = bucket.getKeyAsString.toLong
      val duration = bucket.getAggregations.get[InternalSum]("duration_agg").getValue.toInt
      val hits = bucket.getDocCount

      Entry(time,duration,hits)
    })
  }

}

