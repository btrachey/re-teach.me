package controllers.security

import models.{User, CustomSession}
import play.api.i18n.MessagesApi
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthMessagesRequest[A](val user: User, messagesApi: MessagesApi, request: Request[A]) extends MessagesRequest[A](request, messagesApi)

class AuthenticatedActionBuilder(val parser: BodyParser[AnyContent],
                                 messagesApi: MessagesApi,
                                 builder: AuthenticatedBuilder[User])
                                (implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthMessagesRequest, AnyContent] {

  @Inject
  def this(parser: BodyParsers.Default,
           messagesApi: MessagesApi,
           builder: SessionAuthenticatedBuilder)(implicit ec: ExecutionContext) = {
    this(parser: BodyParser[AnyContent], messagesApi, builder)
  }

  override def invokeBlock[A](request: Request[A], block: AuthMessagesRequest[A] => Future[Result]): Future[Result] = {
    builder.authenticate(request, { authRequest: AuthenticatedRequest[A, User] =>
      block(new AuthMessagesRequest[A](authRequest.user, messagesApi, request))
    })
  }
}
