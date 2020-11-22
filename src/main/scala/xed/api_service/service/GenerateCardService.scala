package xed.api_service.service

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.util.Future
import scalaj.http
import xed.api_service.domain.{CardDesign, GeneratedCardResponse}
import xed.api_service.util.JsonUtils

trait GenerateCardService {

  def generateCard(word: String): Future[Option[GeneratedCardResponse]]
}

case class GenerateCardServiceImpl(host: String) extends GenerateCardService {

  override def generateCard(word: String): Future[Option[GeneratedCardResponse]] = Future {
    try{
      val postData = JsonUtils.toJson(Map(
        "word" -> word,
        "source_lang" -> "en",
        "target_lang" -> "en"
      ))

      val body = http.Http(s"$host/api/generate/card/vocab")
        .header("Content-Type","application/json")
        .postData(postData)
        .asString.body

      val r = JsonUtils.fromJson[JsonNode](body)
      val isSuccess = r.at("/success").asBoolean(false)
      if(isSuccess) {
        val data = JsonUtils.fromJson[Map[String,CardDesign]](r.at("/data").toString)

        if(data.contains("noun"))
          data.get("noun").map(x => GeneratedCardResponse(word,"noun", x))
        else if(data.contains("verb"))
          data.get("verb").map(x => GeneratedCardResponse(word,"verb", x))
        else if(data.contains("adjective"))
          data.get("adjective").map(x => GeneratedCardResponse(word,"adjective", x))
        else
          data.headOption.map(e => GeneratedCardResponse(word, e._1, e._2))
      }else {
        println(s"No dictionary record for: $word")
        None
      }
    }catch {
      case ex: Exception =>
        println(s"Exception for: $word")
        None
    }
  }

}