package xed.chatbot.service

import com.twitter.util.Future
import xed.dictionary.domain.thrift.TDictionary
import xed.dictionary.service.TDictionaryService

trait DictionaryService {

  def lookup(sourceLang: String, targetLang: String, word: String): Future[Option[TDictionary]]

  def multiLookup(sourceLang: String, targetLang: String, words: Seq[String]): Future[Map[String, TDictionary]]

}


case class DictionaryServiceImpl(client: TDictionaryService.MethodPerEndpoint) extends DictionaryService {

  override def lookup(sourceLang: String, targetLang: String, word: String): Future[Option[TDictionary]] = {
    for {
      r<- client.lookup(sourceLang,targetLang,word)
    } yield r.dictionary
  }

  override def multiLookup(sourceLang: String, targetLang: String, words: Seq[String]): Future[Map[String, TDictionary]] = {
    for {
      r<- client.multiLookup(sourceLang,targetLang,words)
    } yield {
      r._2.map(_.toMap)
        .getOrElse(Map.empty)
    }
  }

}