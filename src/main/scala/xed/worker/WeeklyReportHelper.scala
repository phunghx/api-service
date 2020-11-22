package xed.worker

import java.net.InetAddress
import java.util.TimeZone

import com.google.inject.Inject
import com.typesafe.config.Config
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.transport.client.PreBuiltTransportClient
import xed.api_service.util.ZConfig

import scala.collection.JavaConversions._
import scala.language.implicitConversions


/**
 * Created by phg on 2020-04-24.
 **/
case class WeeklyReportHelper @Inject()(timeZone: TimeZone) extends Elasticsearchable with TimeHelper {

  private val config: Config = ZConfig.getConf("es_client")
  private val client: TransportClient = {
    val client = new PreBuiltTransportClient(Settings.builder()
      .put("cluster.name", config.getString("cluster_name"))
      .put("client.transport.sniff", "false")
      .build())
    config.getStringList("servers").map(_.split(":")).filter(_.length == 2).foreach(hp => {
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hp(0)), hp(1).toInt))
    })
    client
  }

  private val indexName = "srs"
  private val srsType = config.getString("srs_type")
  private val courseType = config.getString("course_type")

  def getReviewsLastWeek: Map[String, Long] = {
    getAggregationUser(
      QueryBuilders.boolQuery()
//        .mustRange("last_review_time",
//          gte = Some(lastSunday(week = 0, timeZone = timeZone)),
//          lte = Some(lastSaturday(week = 0, timeZone = timeZone)))
        .mustRange("memorizing_level", gte = Some(6))
    )
  }

  private def getAggregationUser(query: QueryBuilder): Map[String, Long] = {
    println(query)
    val res = client.prepareSearch(indexName).setTypes(srsType, courseType)
      .setQuery(query)
      .addAggregation(
        AggregationBuilders.terms("count_long_term")
          .field("username")
          .size(100000)
      )
      .setFetchSource(false)
      .setSize(0)
      .execute().actionGet()
    println(res)
    res.getAggregations.get[Terms]("count_long_term")
      .getBuckets
      .map(bucket => {
        bucket.getKey.toString -> bucket.getDocCount
      }).toMap
  }
}
