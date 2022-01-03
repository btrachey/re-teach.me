package controllers.security

import models.{CustomSession, User}
import play.api.mvc.Security._
import play.api.mvc.{AnyContent, BodyParser, BodyParsers, RequestHeader}
import repositories.{SessionRepository, UserRepository}

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class SessionAuthenticatedBuilder @Inject()(parser: BodyParsers.Default, val sessionRepository: SessionRepository, val userRepository: UserRepository)(implicit ex: ExecutionContext)
  extends AuthenticatedBuilder[User]({ req: RequestHeader =>
//    req.session.get("sessionToken").flatMap(CustomSession.parseSessionTokenString).filter(!_.isExpired)
    Await.result(
      req.session.get("sessionToken") match {
        case Some(t) => sessionRepository.findByToken(t).flatMap{
          case Some(s) => userRepository.findByEmail(s.email)
          case None => Future.successful(Some(User.genericUser))
        }
        case None => Future.successful(Some(User.genericUser))
      },
      10 seconds
    )
  }, parser) {
//  @Inject()
//  def this(parser: BodyParsers.Default)(implicit ex: ExecutionContext) = {
//    this(parser: BodyParser[AnyContent], sessionRepository: SessionRepository)
//  }
}
