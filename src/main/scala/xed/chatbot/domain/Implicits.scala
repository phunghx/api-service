package xed.chatbot.domain

import com.fasterxml.jackson.databind.JsonNode
import xed.api_service.domain.design.v100.{Dictionary, PhraseExample, Pronunciation, Translation}
import xed.api_service.util.JsonUtils
import xed.dictionary.domain.thrift.TDictionary

import scala.collection.JavaConversions._

/**
 * @author andy
 * @since 1/31/20
 **/
object Implicits {

  implicit class TDictionaryLike(dict: TDictionary) {

    def toDictComponentMap(numTranslation: Int = 1): Map[String, Dictionary] = {
      JsonUtils
        .fromJson[Map[String, JsonNode]](dict.data.get)
        .map(e => {
          val pronunciations = e._2.at("/pronunciations")
            .elements()
            .map(node => JsonUtils.fromJson[Pronunciation](node.toString))
            .toSeq

          val images = e._2.at("/images")
            .elements()
            .map(_.asText())
            .filter(x => x != null && x.nonEmpty)
            .toSeq

          val translations = e._2.at("/translations")
            .elements()
            .filterNot(_.isMissingNode)
            .map(_.toTranslation(1))
            .take(numTranslation)
            .toSeq

          e._1 -> Dictionary(
            word = dict.word,
            partOfSpeech = e._1,
            pronunciations = pronunciations,
            translations = translations,
            images = Some(images))

        })
    }
  }

  implicit class TranslationLike(node: JsonNode) {

    def toTranslation(numExamples: Int = 1): Translation = {
      val meaning = node.at("/meaning").asText("")
      val description = Option(node.at("/description").asText(null))
      val examples = node.at("/examples")
        .elements()
        .map(s => PhraseExample(s.asText()))
        .take(numExamples)
        .toSeq

      Translation(
        meaning = meaning,
        description = description,
        examples = examples
      )
    }
  }

}
