package xed.api_service.service

/**
 * Created by phg on 2019-09-28.
 **/
trait MessageService {
  def getMessage(key: String, languageCode: String): String
}

object MessageKey {

}

case class HardCodeMessageService() extends MessageService {

  override def getMessage(key: String, languageCode: String): String = {
    "Xin lỗi, tôi chưa được đào tạo về tính năng này và tôi sẽ cố gắng cập nhật nó sớm nhất. Hiện tại, tôi có thể giúp bạn: Review card, Learn vocabulary, Read News"

  }
}