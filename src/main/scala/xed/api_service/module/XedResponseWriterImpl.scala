package xed.api_service.module

import com.google.common.net.MediaType._
import com.google.inject.Inject
import com.twitter.finatra.http.marshalling
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyWriter, WriterResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.Logging
import org.apache.commons.lang.ClassUtils

case class ApiError(code: Int,
                    reason: String,
                    message: String)

case class BaseResponse(success: Boolean,
                        data: Option[Any],
                        error: Option[ApiError])



class XedResponseWriterImpl @Inject()(mapper: FinatraObjectMapper,
                                      fileResolver: FileResolver) extends DefaultMessageBodyWriter with Logging {

  mapper.registerModule(CustomJsonModule)

  override def write(obj: Any): WriterResponse = {
    if (isPrimitiveOrWrapper(obj.getClass)) {
      marshalling.WriterResponse(JSON_UTF_8, mapper.writeValueAsString(BaseResponse(success = true,
        data = Some(obj), error = None)))
    } else {
      obj match {
        case r: BaseResponse =>
          marshalling.WriterResponse(JSON_UTF_8, mapper.writeValueAsString(r))
       case ex: Throwable =>
          marshalling.WriterResponse(JSON_UTF_8, mapper.writeValueAsString(BaseResponse(success = false, data = None, error = Some(ApiError(500, "internal_error", ex.getMessage)))))
        case a: Any =>
          marshalling.WriterResponse(JSON_UTF_8, mapper.writeValueAsString(BaseResponse(success = true,
            data = Some(a), error = None)))
      }
    }

  }

  private def isPrimitiveOrWrapper(clazz: Class[_]): Boolean = {
    clazz.isPrimitive ||  ClassUtils.wrapperToPrimitive(clazz) != null
  }

}
