package controllers.security

import play.api.mvc._
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](username: String, request: Request[A]) extends WrappedRequest[A](request)

class UserAction @Inject()(
                            val parser: BodyParsers.Default,
                            val sessionRepository: SessionRepository,
                          )(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[UserRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
    val sessionTokenOpt = request.session.get("sessionToken")
    val sessionFuture = sessionTokenOpt.map(sessionRepository.findByToken).getOrElse(Future.successful(None))
    sessionFuture.flatMap(
      _.map(s => block(UserRequest(s.email, request)))
        .getOrElse(Future.successful(Results.Unauthorized("You are not authorized to view this content")))
    )
  }

  //  override def invokeBlock[A](request: Request[A]): Future[Result] = {
  //    val sessionTokenOpt = request.session.get("sessionToken")
  //    val sessionFuture = sessionTokenOpt.map(sessionRepository.findByToken).getOrElse(Future.successful(None))
  //    Future.successful {
  //      sessionFuture.flatMap(_.flatMap{
  //        case s if !s.isExpired => Some(UserRequest(s.username, request))
  //        case s => Some(UserRequest(s.username, Results.Unauthorized("You are not permitted to view this content")))
  //      }))
  //        yield UserRequest(s.username, request)}
  //  }
}