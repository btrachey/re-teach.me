package controllers

import controllers.security.AuthenticatedActionBuilder
import models.{Fact, FactCreateForm}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import reactivemongo.api.bson.BSONObjectID
import repositories.{FactRepository, UnapprovedFactRepository, UserRepository}
import play.api.Logging

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class FactController @Inject()(
                                implicit executionContext: ExecutionContext,
                                val factRepository: FactRepository,
                                val unapprovedFactRepository: UnapprovedFactRepository,
                                val cc: MessagesControllerComponents,
                                val authenticatedAction: AuthenticatedActionBuilder
                              ) extends MessagesAbstractController(cc) with Logging {

  val yearFormSingle: Form[Int] = Form(
    single(
      "Year" -> number
    )
  )
  val factCreateForm: Form[FactCreateForm] = Form(
    mapping(
      "Title" -> text,
      "Description" -> text,
      "References" -> seq(text)
    )(FactCreateForm.apply)(FactCreateForm.unapply)
  )

  def findAll: Action[AnyContent] = authenticatedAction.async { implicit request: Request[AnyContent] =>
    factRepository.findAll().map {
      facts => Ok(Json.toJson(facts))
    }
  }

  def findOne(id: String): Action[AnyContent] = authenticatedAction.async { implicit request: Request[AnyContent] =>
    val objectIdTryResult = BSONObjectID.parse(id)
    objectIdTryResult match {
      case Success(objectId) => factRepository.findOne(objectId).map(fact => Ok(Json.toJson(fact)))
      case Failure(_) => Future.successful(BadRequest("Cannot parse the provided fact ID"))
    }
  }

  def createPage: Action[AnyContent] = authenticatedAction { implicit request =>
    request.user.role match {
      case "Generic" => Ok(views.html.notauthorized())
      case _ => Ok(views.html.factcreate(factCreateForm))
    }
  }

  def formCreate: Action[AnyContent] = authenticatedAction.async { implicit request =>
    request.user.role match {
      case "Generic" =>
        Future.successful(Ok(views.html.notauthorized()))
      case _ =>
        factCreateForm.bindFromRequest().fold(
          errorForm => Future.successful(BadRequest(views.html.factcreate(errorForm))),
          formData => {
            val referencesWithIndex = formData.references.zipWithIndex
            val references: Map[String, String] = referencesWithIndex.map(tup => (tup._2 + 1).toString -> tup._1).toMap
            val fact = Fact.fromForm(formData, references, request.user)
            val createFact = unapprovedFactRepository.create(fact)
            Future.successful(Redirect(routes.FactController.singleFact(fact._id.get.stringify, isUnapprovedFact = true)).flashing("newFact" -> "fact created"))
          }
        )

    }
  }

  def index: Action[AnyContent] = authenticatedAction { implicit request =>
    Ok(views.html.factindex(yearFormSingle))
  }

  def byYear: Action[AnyContent] = authenticatedAction.async { implicit request =>
    yearFormSingle.bindFromRequest().fold(
      errorForm => Future.successful(Ok(views.html.factindex(errorForm))),
      year => {
        val allFacts = factRepository.findAll()
        allFacts.map(facts => Ok(views.html.factdisplay(year, facts)))
      }
    )
  }

  def singleFact(id: String, isUnapprovedFact: Boolean = false): Action[AnyContent] = authenticatedAction.async { implicit request =>
    val singleFact = BSONObjectID.parse(id) match {
      case Success(id) => if (isUnapprovedFact) {unapprovedFactRepository.findOne(id)} else factRepository.findOne(id)
      case _ => Future.successful(Option.empty[Fact])
    }
    singleFact.map(_.map(fact => Ok(views.html.singlefactdisplay(fact))).getOrElse(Ok(views.html.index())))
  }
}