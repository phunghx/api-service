package xed.api_service.repository


import com.twitter.app.LoadService.Binding
import com.twitter.inject.Logging
import com.twitter.util.{Future, Promise, Return, Throw}
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.support.AbstractListenableActionFuture
import org.elasticsearch.action.{ActionRequest, ActionRequestBuilder, ActionResponse, DocWriteRequest}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.unit.{Fuzziness, TimeValue}
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.engine.DocumentMissingException
import org.elasticsearch.index.query.{Operator, QueryBuilders}
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.sort.{SortBuilders, SortOrder}
import org.elasticsearch.threadpool.ThreadPool
import org.elasticsearch.transport.RemoteTransportException
import xed.api_service.domain.request.{SearchRequest, SortQuery}
import xed.api_service.domain.response.PageResult
import xed.api_service.util.{Implicits, JsonUtils}

import scala.annotation.tailrec


object ESRepository {


  implicit class ZActionRequestBuilder[I <: ActionRequest, J <: ActionResponse, K <: ActionRequestBuilder[I, J, K]](arb: ActionRequestBuilder[I, J, K]) {


    val internalThreadPool: ThreadPool = internalThreadPool(arb, arb.getClass)

    @tailrec
    private def underlyingError(t: Throwable): Throwable = {
      t match {
        case null => t
        case remote: RemoteTransportException => if (remote.getCause == null) remote else underlyingError(remote.getCause)
        case x => x
      }
    }

    @tailrec
    private[this] def internalThreadPool(arb: ActionRequestBuilder[I, J, K], cls: Class[_]): ThreadPool = {
      if (cls.getSimpleName.equals("ActionRequestBuilder")) {
        val f = cls.getDeclaredField("threadPool")
        f.setAccessible(true)
        f.get(arb).asInstanceOf[ThreadPool]
      }
      else
        internalThreadPool(arb, cls.getSuperclass)
    }

    def asyncGet(): Future[J] = {
      val promise = Promise[J]()
      val listener = new AbstractListenableActionFuture[J, J](internalThreadPool) {
        override def onFailure(e: Exception): Unit = promise.setException(underlyingError(e))

        override def onResponse(result: J): Unit = promise.setValue(result)

        override def convert(listenerResponse: J): J = listenerResponse
      }

      arb.execute(listener)
      promise
    }

  }

  def buildESQuery(searchRequest: SearchRequest) = {
    val query = QueryBuilders.boolQuery()

    val sorts = searchRequest.getSorts.map(s => {
      SortBuilders.fieldSort(s.field).order(if (SortQuery.ORDER_ASC.equalsIgnoreCase(s.order)) SortOrder.ASC else SortOrder.DESC)
    })

    searchRequest.getTerms.foreach(t => {
      query.must(QueryBuilders.termsQuery(t.field, t.getParsedValue.map(_.toString): _*))
    })

    searchRequest.getNotTerms.foreach(t => {
      query.mustNot(QueryBuilders.termsQuery(t.field, t.getParsedValue.map(_.toString): _*))
    })

    searchRequest.getRanges.foreach(q => {
      val r = QueryBuilders.rangeQuery(q.field)
      q.lowValue match {
        case Some(x) if q.lowIncluded => r.gte(x)
        case Some(x) if !q.lowIncluded => r.gt(x)
        case _ =>
      }
      q.highValue match {
        case Some(x) if q.highIncluded => r.lte(x)
        case Some(x) if !q.highIncluded => r.lt(x)
        case _ =>
      }
      query.must(r)
    })
    //    searchRequest.getMatches.foreach(t => {
    //      query.must(
    //        QueryBuilders.queryStringQuery(s"*${t.getParsedValue.head.toString.toLowerCase}*")
    //          .field(t.field)
    //          .allowLeadingWildcard(t.allowLeadingWildcard.getOrElse(true).toString.toBoolean)
    //          .analyzeWildcard(t.analyzeWildcard.getOrElse(true).toString.toBoolean)
    //          .defaultOperator(if (t.getDefaultOperator.equals("and")) Operator.AND else Operator.OR)
    //      )
    //    })

    searchRequest.getMatches.foreach(t => {
      query.must(
        QueryBuilders.matchQuery(t.field, s"*${t.getParsedValue.head.toString.toLowerCase}*")
          .operator(if (t.getDefaultOperator.equals("and")) Operator.AND else Operator.OR)
          .fuzziness(Fuzziness.TWO)

      )
    })
    (query, sorts)
  }

  def escape(s: String): String = {
    val sb = new StringBuilder
    var i = 0
    while ( {
      i < s.length
    }) {
      val c = s.charAt(i)
      // These characters are part of the query syntax and must be escaped
      if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~' || c == '*' || c == '?' || c == '|' || c == '&' || c == '/') sb.append('\\')
      sb.append(c)

      {
        i += 1;
        i - 1
      }
    }
    sb.toString
  }
}

case class ESConfig(indexName: String,
                    indexSettings: String,
                    indexMappings: Map[String, String],
                    aggTimeZone: Option[String] = None
                   ) extends Serializable

case class UpdateResult(count: Int, data: Option[Map[String, Boolean]])


abstract class ESRepository extends Logging with Serializable {
  protected val client: TransportClient
  protected val config: ESConfig

  try {
    val indexExisted =
      prepareExist.execute().get().isExists
    if (!indexExisted) {
      createIndexWithMapping()
    } else {
      mergeMapping()
    }

  } catch {
    case x: Exception => {
      error(s"Check Index Exist Failed: ${x.getMessage}")
    }
  }

  private[this] def createIndexWithMapping(): Unit = {
    info(s"Prepare Create Index ${config.indexName}")
    val prepareIndex = client.admin().indices().prepareCreate(config.indexName)

    info(s"--> Index Settings ${config.indexSettings}")
    prepareIndex.setSettings(config.indexSettings, XContentType.YAML)
    config.indexMappings.foreach(mapping => {
      info(s"--> Add type ${mapping._1}")
      info(s"--> With mapping $mapping")
      prepareIndex.addMapping(mapping._1, mapping._2, XContentType.YAML)
    })
    if (!prepareIndex.execute().actionGet().isAcknowledged) {
      throw new Exception("prepare index environment failed")
    }
  }

  private[this] def mergeMapping(): Unit = {
    config.indexMappings.foreach(mapping => {
      info(s"--> Add type ${mapping._1}")
      info(s"--> With mapping ${mapping._2}")
      if (putMappingSync(mapping._1, mapping._2).isAcknowledged) {
        info(s"=> Mapping merged! Type ${mapping._1} with mapping ${mapping._2}")
      } else {
        throw new Exception("Merge mapping failed")
      }
    })
  }

  private[this] def putMappingSync(esType: String, src: String) = {
    client.admin().indices()
      .preparePutMapping(config.indexName)
      .setType(esType)
      .setSource(src, XContentType.YAML)
      .execute().actionGet()
  }


  def prepareExist = client.admin().indices().prepareExists(config.indexName)

  def prepareScroll(scrollId: String) = client.prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMinutes(1))

  def prepareBulk = client.prepareBulk

  def prepareSearch = client.prepareSearch(config.indexName)

  def prepareGet = client.prepareGet().setIndex(config.indexName)

  def prepareMultiGet = client.prepareMultiGet()

  def prepareIndex = client.prepareIndex().setIndex(config.indexName)

  def prepareUpdate = client.prepareUpdate().setIndex(config.indexName)

  def prepareDelete = client.prepareDelete().setIndex(config.indexName)


}


trait ElasticsearchObject {

  def esId: String

  def esSource: String
}


abstract class AbstractESRepository[T <: ElasticsearchObject](implicit m: Manifest[Binding[T]]) extends ESRepository {

  import ESRepository._

  protected val esType: String

  private def indexRequest(item: T, isCreated: Boolean = true) = {
    val id = if (item.esId == null || item.esId.isEmpty) null else item.esId
    if (isCreated)
      prepareIndex
        .setType(esType)
        .setId(id)
        .setSource(JsonUtils.toJson(item), XContentType.JSON)
        .setOpType(DocWriteRequest.OpType.CREATE)
    else
      prepareIndex
        .setType(esType)
        .setId(id)
        .setSource(JsonUtils.toJson(item), XContentType.JSON)
        .setOpType(DocWriteRequest.OpType.INDEX)
  }

  def insert(item: T, isCreated: Boolean = true): Future[Option[String]] = {
    indexRequest(item, isCreated).asyncGet().map(r => {
      Option(r.getId)
    })
  }

  def multiInsert(items: Seq[T], isCreated: Boolean = true): Future[Seq[T]] = {
    if (items == null || items.isEmpty) Future.Nil else {
      items.foldLeft(prepareBulk)((builder, item) => {
        builder.add(indexRequest(item, isCreated))
      }).asyncGet().map(response => {
        response.getItems
          .filterNot(_.isFailed)
          .map(r => r.getItemId)
          .map(items(_))
      })
    }
  }


  def update(item: T): Future[UpdateResult] = {
    prepareUpdate.setType(esType)
      .setId(item.esId)
      .setDoc(JsonUtils.toJson(item), XContentType.JSON)
      .asyncGet()
      .map(r => r.getId match {
        case id if id != null && id.nonEmpty => UpdateResult(1, Some(Map(id -> true)))
        case _ => UpdateResult(0, None)
      })
  }

  def multiUpdate(items: Seq[T]): Future[UpdateResult] = {
    val request = items.foldLeft(prepareBulk)((builder, item) => {
      builder.add(prepareUpdate.setType(esType)
        .setId(item.esId)
        .setDoc(JsonUtils.toJson(item), XContentType.JSON))
    })
    if (request.numberOfActions() > 0)
      request.asyncGet().map(response => {
        val count = response.getItems.filterNot(_.isFailed).size
        val details = response.getItems.map(r => items(r.getItemId).esId -> !r.isFailed).toMap
        UpdateResult(count, Some(details))
      })
    else Future.value(UpdateResult(0, None))
  }


  def delete(id: String): Future[Boolean] = {
    prepareDelete.setType(esType)
      .setId(id)
      .asyncGet().map(_ => true)
  }

  def multiDelete(ids: Seq[String]): Future[UpdateResult] = {
    if (ids != null && ids.nonEmpty) {
      ids.foldLeft(prepareBulk)((builder, id) => {
        builder.add(prepareDelete.setType(esType).setId(id))
      }).asyncGet().map(r => {
        val count = r.getItems.map(r => if (r.isFailed) 0 else 1).sum
        val details = r.getItems.map(r => ids(r.getItemId) -> !r.isFailed).toMap
        UpdateResult(count, Some(details))
      })
    } else Future.value(UpdateResult(0, None))
  }

  def get(id: String)(implicit m: Manifest[T]): Future[Option[T]] = {
    prepareGet.setType(esType)
      .setId(id)
      .asyncGet()
      .map(r => {
        if (r.isExists)
          Some(JsonUtils.fromJson[T](r.getSourceAsString))
        else None
      }).transform({
      case Return(r) => Future.value(r)
      case Throw(e) if e.isInstanceOf[DocumentMissingException] => Future.None
    })
  }


  def multiGet(ids: Seq[String])(implicit m: Manifest[T]): Future[Map[String, T]] = {

    if (ids.nonEmpty) {
      val request = ids.foldLeft(prepareMultiGet)((request, id) => {
        request.add(config.indexName, esType, id)
      })

      request.asyncGet().map(
        _.getResponses
          .filterNot(_.isFailed)
          .filter(_.getResponse.isExists)
          .map(r => {
        JsonUtils.fromJson[T](r.getResponse.getSourceAsString)
      }).map(x => x.esId -> x).toMap
      )
    } else Future.value(Map.empty)

  }

  def getNotExistIds(ids: Seq[String])(implicit m: Manifest[T]): Future[Seq[String]] = {

    if (ids.nonEmpty) {
      val request = ids.foldLeft(prepareMultiGet)((request, id) => {
        request.add(config.indexName, esType, id)
      })

      request.asyncGet().map(r =>{
        val itemMap =  r.getResponses
          .filterNot(_.isFailed)
          .filter(_.getResponse.isExists)
          .map(r => {
            JsonUtils.fromJson[T](r.getResponse.getSourceAsString)
          }).map(x => x.esId -> x).toMap
        ids.filterNot(itemMap.contains(_))
      })
    } else Future.value(Seq.empty)

  }


  def genericSearch(searchRequest: SearchRequest)(implicit m: Manifest[T]): Future[PageResult[T]] = {
    val (query, sorts) = ESRepository.buildESQuery(searchRequest)
    val req = prepareSearch.setTypes(esType)
      .setQuery(query)
      .setFrom(searchRequest.from)
      .setSize(searchRequest.size)
    sorts.foreach(req.addSort(_))
    req.asyncGet().map(r => {
      val data = r.getHits.getHits.map(h => JsonUtils.fromJson[T](h.getSourceAsString))
      PageResult[T](r.getHits.getTotalHits, data)
    })
  }

  def foreach(index: String,
              types: Option[Seq[String]],
              batchSize: Int,
              fn:Seq[SearchHit] => Unit): Future[Boolean] = Implicits.async {

    def recursiveSearch(r: SearchResponse, fn: Seq[SearchHit] => Unit): Boolean = {

      val data = r.getHits.getHits
      fn(data)
      if (data.nonEmpty && r.getScrollId != null) {
        val resp = client.prepareSearchScroll(r.getScrollId)
          .setScroll(TimeValue.timeValueMinutes(1L))
          .execute().actionGet()

        recursiveSearch(resp, fn)
      } else {
        true
      }
    }

    val searchRequestBuilder = client.prepareSearch(index)
      .setTypes(types.getOrElse(Seq.empty):_*)
      .setQuery(QueryBuilders.matchAllQuery())
      .setScroll(TimeValue.timeValueMinutes(1L))
      .setFrom(0)
      .setSize(batchSize)

    val r = searchRequestBuilder.execute().actionGet()
    recursiveSearch(r, fn)
  }

  def foreach(searchRequest: SearchRequest,
              batchSize: Int,
              fn:Seq[SearchHit] => Unit) : Future[Boolean] = Implicits.async {

    def _recursiveSearchScroll(response: SearchResponse, fn: Seq[SearchHit] => Unit): Boolean = {

      val data = response.getHits.getHits
      fn(data)
      if (data.nonEmpty && response.getScrollId != null) {
        val resp = client.prepareSearchScroll(response.getScrollId)
          .setScroll(TimeValue.timeValueMinutes(1L))
          .execute()
          .actionGet()
        _recursiveSearchScroll(resp, fn)
      } else {
        true
      }
    }

    val (query, sorts) = ESRepository.buildESQuery(searchRequest)
    val searchRequestBuilder = prepareSearch.setTypes(esType)
      .setQuery(query)
      .setScroll(TimeValue.timeValueMinutes(1L))
      .setFrom(0)
      .setSize(batchSize)
    sorts.foreach(searchRequestBuilder.addSort(_))

    val response = searchRequestBuilder.execute().actionGet()
    _recursiveSearchScroll(response, fn)

  }
}



