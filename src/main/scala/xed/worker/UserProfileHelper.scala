package xed.worker

import java.net.InetAddress
import java.util.{Calendar, TimeZone}

import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.config.Config
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.transport.client.PreBuiltTransportClient
import xed.api_service.util.JsonUtils._

import scala.collection.JavaConversions._
import scala.language.implicitConversions

/**
 * Created by phg on 2020-03-12.
 **/
case class UserProfileHelper @Inject()(@Named("user-profile-config") config: Config) extends Elasticsearchable with TimeHelper {

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

  private val indexName = config.getString("index")
  private val typeName = config.getString("type")

  def searchUsers(
    offset: Option[Int] = None,
    lastOpenBefore: Option[Long] = None
  ): Array[UserInfo] = {
    val res = client.prepareSearch(indexName).setTypes(typeName)
      .setSize(1000)
      .setQuery(
        QueryBuilders.boolQuery()
          .mustTerm("time_zone_offset_seconds", offset)
          .mustRange("last_open", lt = lastOpenBefore)
      )
      .setScroll(TimeValue.timeValueMinutes(5))
      .execute().actionGet()

    val users: Array[UserInfo] = res
    if (users.length < 1000) {
      users
    } else {
      users ++ getAllUser(res.getScrollId)
    }
  }

  private def getAllUser(scrollId: String): Array[UserInfo] = {
    if (scrollId != null && scrollId.nonEmpty) {
      val res = client
        .prepareSearchScroll(scrollId)
        .setScroll(TimeValue.timeValueMinutes(5))
        .execute().actionGet()
      val users: Array[UserInfo] = res
      if (users.length < 1000) {
        users
      } else {
        users ++ getAllUser(res.getScrollId)
      }
    } else Array()
  }
}

case class UserInfo(
  userId: String,
  timeZoneOffsetSeconds: Int,
  email: String
)

trait TimeHelper {
  def currentUTCHour: Int = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.HOUR_OF_DAY)

  def lastSunday(week: Int = 1, timeZone: TimeZone = TimeZone.getTimeZone("UTC")): Long = {
    val calendar = Calendar.getInstance(timeZone)
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    calendar.add(Calendar.WEEK_OF_MONTH, -week)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    calendar.getTimeInMillis
  }

  def lastSaturday(week: Int = 1, timeZone: TimeZone = TimeZone.getTimeZone("UTC")): Long = {
    val calendar = Calendar.getInstance(timeZone)
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    calendar.add(Calendar.WEEK_OF_MONTH, -(week -1))
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    calendar.getTimeInMillis - 1
  }
}

trait Elasticsearchable {

  implicit class OptionQueryBuilder(queryBuilder: BoolQueryBuilder) {

    def mustStringQuery(key: String, value: Option[String]): BoolQueryBuilder = {
      value match {
        case Some(v) => queryBuilder.must(QueryBuilders.queryStringQuery(v).field(key))
        case _ => queryBuilder;
      }
    }

    def mustTerm(key: String, value: Option[_]): BoolQueryBuilder = {
      value match {
        case Some(v) => queryBuilder.must(QueryBuilders.termQuery(key, v))
        case _ => ;
      }
      queryBuilder
    }

    def shouldTerm(key: String, value: Option[_]): BoolQueryBuilder = {
      value match {
        case Some(v) => queryBuilder.should(QueryBuilders.termQuery(key, v))
        case _ => ;
      }
      queryBuilder
    }

    def mustTerms(key: String, value: Option[String]): BoolQueryBuilder = {
      value match {
        case Some(v) => queryBuilder.must(QueryBuilders.termsQuery(key, v.split(","): _*))
        case _ => ;
      }
      queryBuilder
    }

    def mustTermsArray(key: String, values: Option[Array[String]]): BoolQueryBuilder = {
      values match {
        case Some(v) => queryBuilder.must(QueryBuilders.termsQuery(key, v: _*))
        case _ => ;
      }
      queryBuilder
    }

    def mustRange(key: String, gte: Option[_] = None, lte: Option[_] = None, gt: Option[_] = None, lt: Option[_] = None): BoolQueryBuilder = {
      if (gte.isDefined || lte.isDefined || gt.isDefined || lt.isDefined) {
        val range = QueryBuilders.rangeQuery(key)
        gte match {
          case Some(g) => range.gte(g)
          case _ => ;
        }
        lte match {
          case Some(l) => range.lte(l)
          case _ => ;
        }
        gt match {
          case Some(g) => range.gt(g)
          case _ => ;
        }
        lt match {
          case Some(l) => range.lt(l)
          case _ => ;
        }
        queryBuilder.must(range)
      }
      queryBuilder
    }

    def shouldRange(key: String, gte: Option[_] = None, lte: Option[_] = None, gt: Option[_] = None, lt: Option[_] = None): BoolQueryBuilder = {
      if (gte.isDefined || lte.isDefined || gt.isDefined || lt.isDefined) {
        val range = QueryBuilders.rangeQuery(key)
        gte match {
          case Some(g) => range.gte(g)
          case _ => ;
        }
        lte match {
          case Some(l) => range.lte(l)
          case _ => ;
        }
        gt match {
          case Some(g) => range.gt(g)
          case _ => ;
        }
        lt match {
          case Some(l) => range.lt(l)
          case _ => ;
        }
        queryBuilder.should(range)
      }
      queryBuilder
    }

    def mustWillCard(key: String, value: Option[String], boost: Option[Float] = None): BoolQueryBuilder = {
      value match {
        case Some(v) => val query = QueryBuilders.wildcardQuery(key, v)
          boost match {
            case Some(b) => query.boost(b)
            case _ => ;
          }
          queryBuilder.must(query)
        case _ => ;
      }
      queryBuilder
    }
  }

  implicit def SearchResponse2ArrayObject[A: Manifest](searchResponse: SearchResponse): Array[A] = {
    searchResponse.getHits.getHits.map(_.getSourceAsString.asJsonObject[A])
  }
}