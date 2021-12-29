package controllers.security

import models.CustomSession
import play.api.mvc.Security._
import play.api.mvc.{AnyContent, BodyParser, BodyParsers, RequestHeader}
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class SessionAuthenticatedBuilder @Inject()(parser: BodyParsers.Default, val sessionRepository: SessionRepository)(implicit ex: ExecutionContext)
  extends AuthenticatedBuilder[CustomSession]({ req: RequestHeader =>
    Await.result(req.session.get("sessionToken").map(t => sessionRepository.findByToken(t)).getOrElse(Future.successful(None)), 10 seconds)
  }, parser) {
//  @Inject()
//  def this(parser: BodyParsers.Default)(implicit ex: ExecutionContext) = {
//    this(parser: BodyParser[AnyContent], sessionRepository: SessionRepository)
//  }
}
