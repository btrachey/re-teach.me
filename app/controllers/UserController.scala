package controllers

import models.{ApprovalUser, UserCreationForm, LoginForm, CustomSession}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import reactivemongo.api.bson.BSONObjectID
import repositories.{ApprovalUserRepository, SessionRepository}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class UserController @Inject()(
                                implicit executionContext: ExecutionContext,
                                val userRepository: ApprovalUserRepository,
                                val sessionRepository: SessionRepository,
                                val cc: MessagesControllerComponents
                              ) extends MessagesAbstractController(cc) {


    val userCreationForm: Form[UserCreationForm] = Form(
      mapping(
        "Email" -> email,
        "Password" -> text,
        "First Name" -> text,
        "Middle Name" -> optional(text),
        "Last Name" -> text
      )(UserCreationForm.apply)(UserCreationForm.unapply)
    )

  def newUser: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.adduser(userCreationForm))
  }

  def newUserPost: Action[AnyContent] = Action.async { implicit request =>
    userCreationForm.bindFromRequest().fold(
      errorForm => Future.successful(BadRequest(views.html.adduser(errorForm))),
      formData => {
        val user = ApprovalUser(formData)
        userRepository
          .createIfNew(user.copy(password = user.password.map(ApprovalUser.PwManager.hashPassword)))
          .map(_.map(writeResult => Ok(s"Created user: $writeResult"))
            .getOrElse(Ok(views.html.adduser(userCreationForm.fill(formData)
              .withError("Email","A user with this email address already exists")))))
      }
    )
  }

  val userLoginForm: Form[LoginForm] = Form(
    mapping(
      "Email" -> email,
      "Password" -> text
    )(LoginForm.apply)(LoginForm.unapply)
  )

  def login: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.userlogin(userLoginForm))
  }

  def loginPost: Action[AnyContent] = Action.async { implicit request =>
    userLoginForm.bindFromRequest().fold(
      errorForm => Future.successful(BadRequest(views.html.userlogin(errorForm))),
      formData => {
        userRepository.findByEmail(formData.email)
          .map(_.flatMap(foundUser =>
            ApprovalUser.PwManager.checkPassword(formData.password, foundUser.password.get)))
          .flatMap{
            case Some(true) =>
              val sessionInstance = sessionRepository.create(CustomSession(email = formData.email))
                .flatMap(_ => sessionRepository.findByUser(formData.email))
              sessionInstance.flatMap(_.map(ses => Future.successful(Redirect(routes.FactController.index())
                .addingToSession("sessionToken" -> ses.token.get)))
                .getOrElse(Future.successful(
                  BadRequest(views.html.userlogin(userLoginForm.fill(formData)
                    .withError("Password", "Unable to log in, please try again"))))))

            case _ => Future.successful(
              BadRequest(views.html.userlogin(userLoginForm.fill(formData)
                .withError("Password", "Incorrect password"))))
        }
      }
    )
  }


  def findAll(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    userRepository.findAll().map {
      users => Ok(Json.toJson(users))
    }
  }

  def findOne(id: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val objectIdTryResult = BSONObjectID.parse(id)
    objectIdTryResult match {
      case Success(objectId) => userRepository.findOne(objectId).map(user => Ok(Json.toJson(user)))
      case Failure(_) => Future.successful(BadRequest("Cannot parse the provided user ID"))
    }
  }

  def create(): Action[JsValue] = Action.async(controllerComponents.parsers.json) { implicit request =>
    request.body.validate[ApprovalUser].fold(
      _ => Future.successful(BadRequest("Cannot parse request body")),
      user => userRepository.create(user).map(_ => Created(Json.toJson(user)))
    )
  }

  def update(id: String): Action[JsValue] = Action.async(controllerComponents.parsers.json) { implicit request =>
    request.body.validate[ApprovalUser].fold(
      _ => Future.successful(BadRequest("Cannot parse request body")),
      user => {
        val objectIdTryResult = BSONObjectID.parse(id)
        objectIdTryResult match {
          case Success(objectId) => userRepository.update(objectId, user).map(result => Ok(Json.toJson(result.ok)))
          case Failure(_) => Future.successful(BadRequest("Cannot parse the provided user ID"))
        }
      }
    )
  }

  def delete(id: String): Action[JsValue] = Action.async(controllerComponents.parsers.json) { implicit request =>
    val objectIdTryResult = BSONObjectID.parse(id)
    objectIdTryResult match {
      case Success(objectId) => userRepository.delete(objectId).map(_ => NoContent)
      case Failure(_) => Future.successful(BadRequest("Cannot parse the provided user ID"))
    }
  }
}