package controllers

import models.ApprovalUser
import javax.inject._
import play.api._
import play.api.mvc._
import repositories.{SessionRepository, ApprovalUserRepository}
import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                implicit executionContext: ExecutionContext,
                                val sessionRepository: SessionRepository,
                                val userRepository: ApprovalUserRepository,
                                val controllerComponents: ControllerComponents
                              ) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

//  def priv() = Action { implicit request: Request[AnyContent] =>
//    val userOpt = extractUser(request)
//    userOpt.map
//  }
//
//  private def extractUser(req: RequestHeader): Option[ApprovalUser] = {
//    val sessionTokenOpt = req.session.get("sessionToken")
//    sessionTokenOpt.map(token => sessionRepository.findByToken(token)).flatMap()
//  }
}
