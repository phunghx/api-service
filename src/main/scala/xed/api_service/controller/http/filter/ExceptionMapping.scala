package xed.api_service.controller.http.filter


import com.fasterxml.jackson.core.JsonParseException
import com.google.inject.Singleton
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassMappingException
import com.twitter.inject.Logging
import javax.inject.Inject
import xed.api_service.domain.exception.XedException
import xed.api_service.module.{ApiError, BaseResponse}
import xed.api_service.service.Analytics
import xed.api_service.service.Operation._
import xed.api_service.util.LoggerUtils
import xed.api_service.util.ThreadUtil._

/**
  * @author anhlt
  */
@Singleton
class CommonExceptionMapping @Inject()(response: ResponseBuilder, analytics: Analytics) extends ExceptionMapper[Throwable] {
  private val logger = LoggerUtils.getLogger("CommonExceptionMapping")

  override def toResponse(request: Request, ex: Throwable): Response = {
    logError(request,ex)
    val error = ex match {
      case ex: XedException => ApiError(ex.getStatus.code, reason = ex.reason, ex.getMessage)
      case _ => ApiError(Status.InternalServerError.code,XedException.InternalError, ex.getMessage)
    }
    analytics.log(API_ERROR, None, Map(
      "func" -> Thread.currentThread().getMethodName,
      "error" -> error
    ))
    response.status(error.code).json(
      BaseResponse(success = false, data = None, error = Some(error)
    ))
  }

  private def logError(request: Request, ex: Throwable): Unit = {
    logger.error(s"${request.contentString} => ${ex.getClass.getName}: ${ex.getMessage}",ex)
  }
}

@Singleton
class CaseClassExceptionMapping @Inject()(response: ResponseBuilder) extends ExceptionMapper[CaseClassMappingException] with Logging {
  override def toResponse(request: Request, ex: CaseClassMappingException): Response = {
    logError(request,ex)
    response.badRequest.json(BaseResponse(success = false,
        data = None,
        error = Some(ApiError(Status.BadRequest.code, reason = XedException.BadRequest, ex.errors.head.getMessage))
    ))
  }
  private def logError(request: Request, ex: Throwable): Unit = {
    logger.error(s"${request.contentString} => ${ex.getClass.getName}: ${ex.getMessage}",ex)
  }
}

@Singleton
class JsonParseExceptionMapping @Inject()(response: ResponseBuilder) extends ExceptionMapper[JsonParseException] with Logging {
  override def toResponse(request: Request, ex: JsonParseException): Response = {
    logError(request,ex)
    response.badRequest.json(BaseResponse(success = false,
        data = None,
        error = Some(ApiError(Status.BadRequest.code,XedException.BadRequest, ex.getMessage))
    ))
  }
  private def logError(request: Request, ex: Throwable): Unit = {
    logger.error(s"${request.contentString} => ${ex.getClass.getName}: ${ex.getMessage}",ex)
  }
}

