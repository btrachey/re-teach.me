package controllers

import controllers.security.AuthenticatedActionBuilder
import play.api.mvc._
import repositories.{SessionRepository, UserRepository}

import javax.inject._
import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                implicit executionContext: ExecutionContext,
                                val sessionRepository: SessionRepository,
                                val userRepository: UserRepository,
                                val controllerComponents: ControllerComponents,
                                val authenticatedAction: AuthenticatedActionBuilder
                              ) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index: Action[AnyContent] = authenticatedAction { implicit request =>
    Ok(views.html.index())
  }

  def untrail(path: String): Action[AnyContent] = Action { implicit request =>
    MovedPermanently("/" + path)
  }
}
