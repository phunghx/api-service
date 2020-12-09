package xed.api_service.repository

import com.fasterxml.jackson.databind.node.TextNode
import com.twitter.util.Future
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.histogram.{DateHistogramInterval, Histogram, InternalDateHistogram}
import org.elasticsearch.search.aggregations.bucket.terms.{StringTerms, Terms}
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum
import xed.api_service.domain.metric.{Entry, LineData}
import xed.api_service.domain.request.{RangeQuery, SearchRequest, SortQuery, TermsQuery}
import xed.api_service.domain.{ReviewInfo, SRSStatus}
import xed.api_service.repository.ESRepository.ZActionRequestBuilder
import xed.api_service.util.TimeUtils
import xed.chatbot.domain.leaderboard.LeaderBoardItem
import xed.userprofile.SignedInUser

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationDouble

object SRSRepository {

  def buildReviewId(username: String, source: String, cardId: String) = {
    s"$source.$username.$cardId"
  }

}


case class SRSRepository(client: TransportClient,
                         config: ESConfig,
                         esType: String) extends AbstractESRepository[ReviewInfo] {



  def getReviewInfo(username: String, source: String, cardId: String) = {
    get(SRSRepository.buildReviewId(username,source,cardId))
  }

  def getReviewInfos(username: String, source: String, cardIds: Seq[String])  = {
    val ids = cardIds.map(SRSRepository.buildReviewId(username,source,_))
    multiGet(ids)
  }

  def deleteReviewInfo(username: String, source: String, cardId: String) = {
    delete(SRSRepository.buildReviewId(username,source,cardId))
  }

  def deleteReviewInfos(username: String, source: String, cardIds: Seq[String])  = {
    val ids = cardIds.map(SRSRepository.buildReviewId(username,source,_))
    multiDelete(ids).map(_.count)
  }

  def getCardReport(username: String,
                    interval: Int,
                    fromTime: Long,
                    toTime: Long): Future[Map[String, LineData]] = {

    val query = QueryBuilders.boolQuery()
      .must(QueryBuilders.termQuery("username", username))
      .must(QueryBuilders.rangeQuery("last_review_time").gte(fromTime).lt(toTime + 1.days.toMillis))

    val agg = AggregationBuilders.terms("status_agg")
      .field("status")
      .minDocCount(0)
      .subAggregation(AggregationBuilders.dateHistogram("date_agg")
        .field("last_review_time")
        .order(Histogram.Order.KEY_ASC)
        .dateHistogramInterval(DateHistogramInterval.minutes(interval)))

    prepareSearch.setTypes(esType)
      .setQuery(query).addAggregation(agg).setSize(0)
      .asyncGet()
      .map(r => {
        val map = r.getAggregations
        map.get[StringTerms]("status_agg").getBuckets.map(bucket => {
          val status = bucket.getKeyAsString
          val agg = bucket.getAggregations.get[InternalDateHistogram]("date_agg")
          val series = parseCountMetric(agg)

          status -> LineData(status, bucket.getDocCount, series)
        }).toMap
      })
  }

  def getNewCardCount(username: String,
                      fromTime: Long,
                      toTime: Long): Future[Long] = {

    val query = QueryBuilders.boolQuery()
      .must(QueryBuilders.termQuery("username", username))
      .must(QueryBuilders.rangeQuery("created_time").gte(fromTime).lt(toTime + 1.days.toMillis))
      .mustNot(QueryBuilders.termQuery("status", SRSStatus.Ignored))

    prepareSearch.setTypes(esType)
      .setQuery(query)
      .setSize(0)
      .asyncGet()
      .map(_.getHits.totalHits)
  }

  def getCardReportV2(username: String,
                    fromTime: Long,
                    toTime: Long): Future[Map[String, Long]] = {

    val query = QueryBuilders.boolQuery()
      .must(QueryBuilders.termQuery("username", username))
      .must(QueryBuilders.rangeQuery("last_review_time").gte(fromTime).lt(toTime + 1.days.toMillis))

    val agg = AggregationBuilders
      .terms("status_agg").field("status")
      .minDocCount(0)

    prepareSearch.setTypes(esType)
      .setQuery(query).addAggregation(agg).setSize(0)
      .asyncGet()
      .map(r => {
        val map = r.getAggregations
        map.get[StringTerms]("status_agg").getBuckets.map(bucket => {
          val status = bucket.getKeyAsString
          status -> bucket.getDocCount
        }).toMap
      })
  }


  def getTotalDueCardDetail(username: String): Future[Map[String, Long]] = {

    val (time, _) = TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis())

    val queryBuilder = QueryBuilders.boolQuery()
      .must(QueryBuilders.termQuery("username", username))
      .must(QueryBuilders.rangeQuery("due_date").lte(time))
      .must(QueryBuilders.termQuery("status", SRSStatus.Learning))

    val agg = AggregationBuilders.terms("sources")
      .field("source")
      .size(1000)

    prepareSearch.setTypes(esType)
      .setQuery(queryBuilder)
      .addAggregation(agg)
      .setSize(0)
      .asyncGet().map(r => {
      r.getAggregations
        .get[StringTerms]("sources")
        .getBuckets.map(bucket => {
        val source = bucket.getKeyAsString
        val count = bucket.getDocCount

        source -> count
      }).toMap
    })
  }


  def getTotalDueCardBySource(username: String, source: String): Future[Long] = {

    val (time, _) = TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis())

    val searchRequest = SearchRequest(from = 0, size = 0)
      .addIfNotExist(TermsQuery("source", ListBuffer(TextNode.valueOf(source))))
      .addIfNotExist(TermsQuery("username", ListBuffer(TextNode.valueOf(username))))
      .addIfNotExist(RangeQuery("due_date", None, false, Some(time), true))
      .addIfNotExist(TermsQuery("status", ListBuffer(TextNode.valueOf(SRSStatus.Learning))))
      .addIfNotExist(SortQuery("due_date", SortQuery.ORDER_ASC))

    genericSearch(searchRequest).map(_.total)
  }

  def getTopByNewCard(beginTime: Long, endTime: Long, size: Int = 100): Future[Seq[LeaderBoardItem]] = {

    prepareSearch.setTypes(esType)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.rangeQuery("created_time").gte(beginTime).lt(endTime))
        .mustNot(QueryBuilders.termQuery("status", SRSStatus.Ignored))
      ).addAggregation(
        AggregationBuilders.terms("users")
          .field("username")
          .size(size)
          .order(Terms.Order.count(false))
      ).setSize(0)
      .asyncGet().map(r => {
      val agg = r.getAggregations.get[StringTerms]("users")

      agg.getBuckets.zipWithIndex.map({ case (bucket, index) => {
        val username = bucket.getKeyAsString
        val cardCount = bucket.getDocCount

        LeaderBoardItem(
          username = username,
          point = cardCount.toInt,
          rank = index + 1,
          userProfile = None
        )
      }})
    })
  }

  def getLearningCardStats(username: String, dayInterval: Int, fromTime: Long, toTime: Long): Future[LineData] = {
    getCardReport(username,dayInterval,fromTime,toTime)
      .map(_.get(SRSStatus.Learning).getOrElse(LineData(SRSStatus.Learning,0,Seq.empty)))
  }

  def getDoneCardStats(username: String, dayInterval: Int, fromTime: Long, toTime: Long): Future[LineData] = {
    getCardReport(username,dayInterval,fromTime,toTime)
      .map(_.get(SRSStatus.Done).getOrElse(LineData(SRSStatus.Done,0,Seq.empty)))
  }

  def getDeckIds(searchRequest: SearchRequest): Future[Map[String,Seq[String]]] = {
    val (query, _) = ESRepository.buildESQuery(searchRequest)
    prepareSearch.setTypes(esType)
      .setQuery(query)
      .setFrom(searchRequest.from)
      .setSize(0)
      .addAggregation(AggregationBuilders.terms("decks")
        .field("deck_id")
        .minDocCount(1L)
        .size(searchRequest.size)
        .subAggregation(AggregationBuilders.terms("cards")
          .field("card_id")
          .size(2000)
        )
      ).asyncGet().map(r => {
      r.getAggregations
        .get[StringTerms]("decks")
        .getBuckets
        .map(bucket => {
          val deckId = bucket.getKeyAsString
          val cardIds = bucket.getAggregations
            .get[StringTerms]("cards")
            .getBuckets.map(_.getKeyAsString)

          deckId -> cardIds
        }).toMap
    })
  }


  private def parseCountMetric(histogram: InternalDateHistogram) = {
    histogram.getBuckets.map(bucket => {
      val time = bucket.getKeyAsString.toLong
      val total = bucket.getDocCount.toInt
      val hits = bucket.getDocCount

      Entry(time,total,hits)
    })
  }
}

