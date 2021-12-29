package controllers

import models.{Fact, FactCreateForm}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import reactivemongo.api.bson.BSONObjectID
import repositories.FactRepository
import controllers.security.AuthenticatedActionBuilder

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class FactController @Inject()(
                                implicit executionContext: ExecutionContext,
                                val factRepository: FactRepository,
                                val cc: MessagesControllerComponents,
                                val authenticatedAction: AuthenticatedActionBuilder
                              ) extends MessagesAbstractController(cc) {

  val yearFormSingle: Form[Int] = Form(
    single(
      "year" -> number
    )
  )

  def findAll(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    factRepository.findAll().map {
      facts => Ok(Json.toJson(facts))
    }
  }

  def findOne(id: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val objectIdTryResult = BSONObjectID.parse(id)
    objectIdTryResult match {
      case Success(objectId) => factRepository.findOne(objectId).map(fact => Ok(Json.toJson(fact)))
      case Failure(_) => Future.successful(BadRequest("Cannot parse the provided fact ID"))
    }
  }

  def create(): Action[JsValue] = Action.async(controllerComponents.parsers.json) { implicit request =>
    request.body.validate[Fact].fold(
      _ => Future.successful(BadRequest("Cannot parse request body")),
      fact => factRepository.create(fact).map(_ => Created(Json.toJson(fact)))
    )
  }

  val factCreateForm: Form[FactCreateForm] = Form(
    mapping(
      "Title" -> text,
      "Description" -> text
    )(FactCreateForm.apply)(FactCreateForm.unapply)
  )

  def createPage: Action[AnyContent] =  authenticatedAction { implicit request =>
    Ok(views.html.factcreate(factCreateForm))
  }

  def formCreate: Action[AnyContent] = authenticatedAction.async { implicit request =>
    factCreateForm.bindFromRequest().fold(
      errorForm => Future.successful(BadRequest(views.html.factcreate(errorForm))),
      formData => {
        val fact = Fact(formData)
        val createFact = factRepository.create(fact)
        Future.successful(Redirect(routes.FactController.singleFact(fact._id.get.stringify)))
      }
    )
  }

  def update(id: String): Action[JsValue] = Action.async(controllerComponents.parsers.json) { implicit request =>
    request.body.validate[Fact].fold(
      _ => Future.successful(BadRequest("Cannot parse request body")),
      fact => {
        val objectIdTryResult = BSONObjectID.parse(id)
        objectIdTryResult match {
          case Success(objectId) => factRepository.update(objectId, fact).map(result => Ok(Json.toJson(result.ok)))
          case Failure(_) => Future.successful(BadRequest("Cannot parse the provided fact ID"))
        }
      }
    )
  }

  def delete(id: String): Action[JsValue] = Action.async(controllerComponents.parsers.json) { implicit request =>
    val objectIdTryResult = BSONObjectID.parse(id)
    objectIdTryResult match {
      case Success(objectId) => factRepository.delete(objectId).map(_ => NoContent)
      case Failure(_) => Future.successful(BadRequest("Cannot parse the provided fact ID"))
    }
  }

  def index(): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.factindex(yearFormSingle))
  }

  def byYear: Action[AnyContent] = Action.async { implicit request =>
    yearFormSingle.bindFromRequest().fold(
      errorForm => Future.successful(Ok(views.html.factindex(errorForm))),
      year => {
        val allFacts = factRepository.findAll()
        allFacts.map(facts => Ok(views.html.factdisplay(year, facts)))
      }
    )
  }

  def singleFact(id: String): Action[AnyContent] = Action.async { implicit request =>
    val singleFact = BSONObjectID.parse(id) match {
      case Success(id) => factRepository.findOne(id)
      case _ => Future.successful(Option.empty[Fact])
    }
    singleFact.map(_.map(fact => Ok(views.html.singlefactdisplay(fact))).getOrElse(Ok(views.html.index())))
  }
}