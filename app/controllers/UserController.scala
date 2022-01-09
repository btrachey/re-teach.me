package controllers

import controllers.security.AuthenticatedActionBuilder
import models.{CustomSession, LoginForm, User, UserCreationForm}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.api.bson.BSONObjectID
import repositories.{SessionRepository, UserRepository}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class UserController @Inject()(
                                implicit executionContext: ExecutionContext,
                                val userRepository: UserRepository,
                                val sessionRepository: SessionRepository,
                                val cc: MessagesControllerComponents,
                                val authenticatedAction: AuthenticatedActionBuilder
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
  val userLoginForm: Form[LoginForm] = Form(
    mapping(
      "Email" -> email,
      "Password" -> nonEmptyText
    )(LoginForm.apply)(LoginForm.unapply)
  )

  def newUser: Action[AnyContent] = authenticatedAction { implicit request =>
    Ok(views.html.adduser(userCreationForm))
  }

  def newUserPost: Action[AnyContent] = authenticatedAction.async { implicit request =>
    userCreationForm.bindFromRequest().fold(
      errorForm => Future.successful(BadRequest(views.html.adduser(errorForm))),
      formData => {
        val user = User(formData)
        userRepository
          .createIfNew(user.copy(password = user.password.map(User.PwManager.hashPassword)))
          .map(_.map(writeResult => Ok(s"Created user: $writeResult"))
            .getOrElse(Ok(views.html.adduser(userCreationForm.fill(formData)
              .withError("Email", "A user with this email address already exists")))))
      }
    )
  }

  def login: Action[AnyContent] = authenticatedAction { implicit request =>
    Ok(views.html.userlogin(userLoginForm))
  }

  def loginPost: Action[AnyContent] = authenticatedAction.async { implicit request =>
    userLoginForm.bindFromRequest().fold(
      errorForm => Future.successful(BadRequest(views.html.userlogin(errorForm))),
      formData => {
        userRepository.findByEmail(formData.email)
          .flatMap {
            case Some(foundUser) => {
              User.PwManager.checkPassword(formData.password, foundUser.password.get) match {
                case Some(true) => {
                  val sessionInstance = sessionRepository.findByUser(formData.email).flatMap {
                    case Some(customSession) => Future.successful(Some(customSession))
                    case None => sessionRepository.create(CustomSession(email = formData.email)).flatMap(_ => sessionRepository.findByUser(formData.email))
                  }
                  sessionInstance.flatMap(_.map(ses => Future.successful(Redirect(routes.FactController.index)
                    .addingToSession("sessionToken" -> ses.token.get).flashing("loginSuccess" -> "Successfully Logged In")))
                    .getOrElse(Future.successful(
                      BadRequest(views.html.userlogin(userLoginForm.fill(formData)
                        .withError("Password", "Unable to log in, please try again"))))))
                }

                case _ => Future.successful(
                  BadRequest(views.html.userlogin(userLoginForm.fill(formData)
                    .withError("Password", "Incorrect password"))))
              }
            }
            case None => Future.successful(BadRequest(views.html.userlogin(userLoginForm.fill(formData).withError("Email", "User not found"))))
          }
      }
    )
  }

  def findAll(): Action[AnyContent] = authenticatedAction.async { implicit request =>
    userRepository.findAll().map {
      users => Ok(Json.toJson(users))
    }
  }

  def findOne(id: String): Action[AnyContent] = authenticatedAction.async { implicit request =>
    val objectIdTryResult = BSONObjectID.parse(id)
    objectIdTryResult match {
      case Success(objectId) => userRepository.findOne(objectId).map(user => Ok(Json.toJson(user)))
      case Failure(_) => Future.successful(BadRequest("Cannot parse the provided user ID"))
    }
  }

  def userAdmin(): Action[AnyContent]= authenticatedAction.async { implicit request =>
    request.user.role match {
      case "Admin" => for {
        allUsers <- userRepository.findAll()
      } yield Ok(views.html.useradmin(allUsers))
      case _ => Future.successful(Ok(views.html.notauthorized()))
    }
  }

  def setRole(id: String, updatedRole: String): Action[AnyContent] = authenticatedAction.async { implicit request =>
    request.user.role match {
      case "Admin" =>
        val bsonId = BSONObjectID.parse(id)
        bsonId match {
          case Success(bid) => for {
            user <- userRepository.findOne(bid)
            _ <- userRepository.update(bid, user.get.copy(role = updatedRole))
          } yield Redirect(routes.UserController.userAdmin())
          case Failure(e) => Future.successful(Redirect(routes.UserController.userAdmin()).flashing("updateFailed" -> "Role Update Failed"))
        }

      case _ => Future.successful(Ok(views.html.notauthorized()))
    }
  }
}
