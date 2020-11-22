package xed.api_service.service

import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter}
import java.net.InetAddress
import java.util
import java.util.concurrent.atomic.AtomicLong

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.inject.Test
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.client.PreBuiltTransportClient
import xed.api_service.domain.design.v100.Text
import xed.api_service.domain.{ReviewInfo, SRSSource}
import xed.api_service.repository.ESRepository.ZActionRequestBuilder
import xed.api_service.repository.SRSRepository
import xed.api_service.util.Implicits.FutureEnhance
import xed.api_service.util.{Implicits, JsonUtils}

class BackupNewsESTool extends Test {
  val sourceLang = "en"
  val targetLang = "en"

 // private val client = createClient(clusterName = "es_dev_cluster", servers = Seq("34.87.113.166:9300"))
  private val client = createClient(clusterName = "xed_live_cluster", servers = Seq("34.87.143.227:9300"))


  test("Migrate news -> articles 2") {

    val srcClient = client

    val backupIndexConfs = Seq(
      ( "news", Seq("language", "category"))
    )

    val newIndexMap = Map(
      "news" -> "articles"
    )

    backupIndexConfs.foreach({
      case (index, types) =>
        val successCounter = new AtomicLong()
        val failCounter = new AtomicLong()

        searchScroll(srcClient, index, Option(types), searchHits => {
          backup(
            srcClient,
            newIndexMap.get(index).get,
            searchHits,
            successCounter,
            failCounter
          )
        },0, 500)

        println(
          s"""
             |Index: $index
             |Total: ${successCounter.get()} + ${failCounter.get()}
             |OK: $successCounter
             |Error: $failCounter
           """.stripMargin)

    })
  }


  test("Migrate news -> articles") {

    val srcClient = client

    val backupIndexConfs = Seq(
      ( "news", Seq("news"))
    )

    val newIndexMap = Map(
      "news" -> "articles"
    )

    backupIndexConfs.foreach({
      case (index, types) =>
        val successCounter = new AtomicLong()
        val failCounter = new AtomicLong()

        searchScroll(srcClient, index, Option(types), searchHits => {
          backupNews(
            srcClient,
            newIndexMap.get(index).get,
            searchHits,
            successCounter,
            failCounter
          )
        },0, 500)

        println(
          s"""
             |Index: $index
             |Total: ${successCounter.get()} + ${failCounter.get()}
             |OK: $successCounter
             |Error: $failCounter
           """.stripMargin)

    })
  }


  //  test("Backup to file") {
//    val folder = "./data/es_backup"
//
//    val srcClient = prodClient
//
//    val indices: Seq[String] = Seq(
//      "notification"
//    )
//
//    indices.foreach(index => {
//      val successCounter = new AtomicLong()
//      val failCounter = new AtomicLong()
//      searchScroll(srcClient,index, searchHits => {
//        backupToFile(folder,index, searchHits, successCounter, failCounter )
//      }, 50)
//
//      println(
//        s"""
//           |Index: $index
//           |Total: ${successCounter.get()} + ${failCounter.get()}
//           |OK: $successCounter
//           |Error: $failCounter
//           """.stripMargin)
//
//    })
//  }

  private def backupNews(client: TransportClient,
                     index: String,
                     searchHits: Seq[SearchHit],
                     successCounter: AtomicLong,
                     failCounter: AtomicLong) = {
    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._
    val bulk = searchHits.foldLeft(client.prepareBulk())((bulk,searchHit) => {
      val source = searchHit.getSourceAsMap
      source.remove("html_content")
      val newContents = source.get("contents").asInstanceOf[util.List[String]].map(s => Text(s)).asJava
      source.put("contents", newContents)

      bulk.add(new IndexRequest(
        index,
//        searchHit.getType,
        "article",
        searchHit.getId).source(JsonUtils.toJson(source), XContentType.JSON))
    })

    if(bulk.numberOfActions() > 0) {
      bulk.asyncGet().map(response => {
        val successCount = response.getItems.filterNot(_.isFailed).size
        val failCount = response.getItems.filter(_.isFailed).size
        successCounter.addAndGet(successCount)
        failCounter.addAndGet(failCount)
      }).sync()

      println(
        s"""
           |Total: ${successCounter.get()} + ${failCounter.get()}
           """.stripMargin)
    }
  }

  private def backupToFile(folder: String,
                     index: String,
                     searchHits: Seq[SearchHit],
                     successCounter: AtomicLong,
                     failCounter: AtomicLong) = {

    val file = new File(s"$folder/$index.data.txt")
    if (file.getParentFile != null && !file.getParentFile.exists()) {
      file.getParentFile.mkdirs()
    }

    Implicits.tryWith(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)))) { writer => {

      searchHits.foreach(searchHit => {
        val data = JsonUtils.toJson(Map(
          "_index" -> index,
          "_type" -> searchHit.getType,
          "_id" -> searchHit.getId,
          "source" -> JsonUtils.fromJson[JsonNode](searchHit.getSourceAsString)
        ), false)
        println(data)
        writer.write(data)
        writer.write("\n")
      })

      successCounter.addAndGet(searchHits.size)
      println(
        s"""
           |Total: ${successCounter.get()} + ${failCounter.get()}
           """.stripMargin)
    }
    }
  }

  private def backupSRSModel(client: TransportClient,
                     index: String,
                     esType: String,
                     searchHits: Seq[SearchHit],
                     source: String,
                     successCounter: AtomicLong,
                     failCounter: AtomicLong) = {
    val bulkRequestBuilder = searchHits.foldLeft(client.prepareBulk())((bulk,searchHit) => {
      var reviewInfo = JsonUtils.fromJson[ReviewInfo](searchHit.getSourceAsString)
      reviewInfo = reviewInfo.copy(
        id = SRSRepository.buildReviewId(reviewInfo.username.get, source, reviewInfo.cardId.get),
        source = source
      )
      val request = new IndexRequest(index,
        esType,
        reviewInfo.id
      ).source(reviewInfo.esSource,XContentType.JSON)
      bulk.add(request)
    })

    if(bulkRequestBuilder.numberOfActions() > 0) {
      bulkRequestBuilder.asyncGet().map(response => {
        val successCount = response.getItems.filterNot(_.isFailed).size
        val failCount = response.getItems.filter(_.isFailed).size
        successCounter.addAndGet(successCount)
        failCounter.addAndGet(failCount)
        println(
          s"""|Total: ${successCounter.get()} + ${failCounter.get()}""".stripMargin)
      }).sync()
    }
  }


  private def backup(client: TransportClient,
                     index: String,
                     searchHits: Seq[SearchHit],
                     successCounter: AtomicLong,
                     failCounter: AtomicLong) = {
    val bulkRequestBuilder = searchHits.foldLeft(client.prepareBulk())((bulk,searchHit) => {
      val request = new IndexRequest(index,
        searchHit.getType,
        searchHit.getId
      ).source(searchHit.getSourceAsString,XContentType.JSON)
      bulk.add(request)
    })

    if(bulkRequestBuilder.numberOfActions() > 0) {
      bulkRequestBuilder.asyncGet().map(response => {
        val successCount = response.getItems.filterNot(_.isFailed).size
        val failCount = response.getItems.filter(_.isFailed).size
        successCounter.addAndGet(successCount)
        failCounter.addAndGet(failCount)
        println(
          s"""
             |Total: ${successCounter.get()} + ${failCounter.get()}
           """.stripMargin)
      }).sync()
    }
  }

  private def searchScroll(client: TransportClient,
                           index: String,
                           types: Option[Seq[String]],
                           fn:Seq[SearchHit] => Unit,
                           from: Int = 0,
                           batchSize: Int = 20): Boolean = {

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
      .setScroll(TimeValue.timeValueMinutes(60L))
      .setFrom(from)
      .setSize(batchSize)
      .addSort("created_time", SortOrder.ASC)

    val r = searchRequestBuilder.execute().actionGet()
    recursiveSearch(r, fn)

  }

  private def createClient(clusterName: String, servers: Seq[String]) = {
    val client = new PreBuiltTransportClient(Settings.builder()
      .put("cluster.name", clusterName)
      .put("client.transport.sniff", "false")
      .build())

    servers.map(s => {
      val hostPort = s.split(":")
      (hostPort(0), hostPort(1).toInt)
    }).foreach(hostPort => {
      info(s"Add ${hostPort._1}:${hostPort._2} to transport address")
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostPort._1), hostPort._2))
    })
    client
  }

}
