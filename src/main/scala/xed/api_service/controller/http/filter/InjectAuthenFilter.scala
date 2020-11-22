package xed.api_service.controller.http.filter

import com.google.inject.Inject
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Singleton
import xed.api_service.domain.exception.UnAuthenticatedError
import xed.api_service.util.{LoggerUtils, ZConfig}
import xed.caas.domain.thrift.TUserInfo
import xed.userprofile._


@Singleton
class InjectAuthenFilter @Inject()(userAuthService: AuthenService,
                                   userProfileService: UserProfileService,
                                   authHolder        : SessionHolder)
  extends SimpleFilter[Request, Response]
    with Logging {

  private val sessionKey = ZConfig.getString("auth.cookie")
  private val authorizationKey = ZConfig.getString("auth.authorization")

  override def apply(request: Request, service: Service[Request, Response]) = {
    getUserAuth(request).flatMap(auth => {
      authHolder.setAuth(auth, Some(PerformerInfo("http", auth.map(_.username)))) {
        service(request)
      }
    })
  }

  def getUserAuth(request: Request): Future[Option[SignedInUser]] = {
    getSessionId(request) match {
      case Some(sessionId) =>
        userAuthService.getUserWithSessionId(sessionId).flatMap({
          case Some(info) =>
            userProfileService.getProfile(info.username).rescue({
              case ex: Throwable =>
                logger.error(s"No user profile was found for the user= ${info.username}  with the session id = $sessionId", ex)
                Future.value(None)
            }).map(profile => Some(info, profile))
          case _ =>
            logger.error(s"No user was found for the session id = $sessionId")
            Future.value(None)
        }).map({
          case Some(res) =>
            val userPermInfo = res._1
            val profile = res._2
            val roleIds = Option(userPermInfo.roles.map(_.id).toSet)
              .getOrElse(Set.empty[Int])
            val permissions = Set.empty[String]
            Some(SignedInUser(
              sessionId,
              userPermInfo.username,
              profile,
              roleIds,
              permissions,
              Map(0 -> permissions)
            ))
          case _ => None
        })
      case _ =>
        logger.info(s"Request with no session id: ${request.method} ${request.path}")
        logger.info(request.toString())
        Future.value(None)
    }
  }

  private def getSessionId(request: Request): Option[String] = {
    val authCookie = request.cookies
      .get(sessionKey)
      .map(_.value)
      .flatMap(x => if(x!=null && x.trim.nonEmpty) Some(x.trim) else None)
    val authHeader = request.headerMap.get(authorizationKey)

    if (authCookie.isDefined) authCookie else authHeader
  }
}

@Singleton
class LoggedInUserFilter @Inject()(userAuthService: AuthenService,
                                   authHolder     : SessionHolder)
  extends SimpleFilter[Request, Response] {

  override def apply(request: Request,
                     service: Service[Request, Response]): Future[Response] = {
    authHolder.optUser match {
      case Some(u) => service(request)
      case _ => Future.exception(UnAuthenticatedError(Some("No logged in user for this request")))
    }
  }

}
