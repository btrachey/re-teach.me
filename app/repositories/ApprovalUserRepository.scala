package repositories

import models.ApprovalUser
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, BSONObjectID}
import reactivemongo.api.bson.compat._
import reactivemongo.api.commands.WriteResult

import java.time.Instant
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class ApprovalUserRepository @Inject()(
                                        implicit executionContext: ExecutionContext,
                                        reactiveMongoApi: ReactiveMongoApi
                                      ) extends GenericRepository[ApprovalUser] {

  override def collection: Future[BSONCollection] = reactiveMongoApi.database.map(db => db.collection("users"))

  override def create(user: ApprovalUser): Future[WriteResult] = collection
    .flatMap(_.insert(ordered = false)
      .one(user.copy(_creationDate = Some(Instant.now), _updateDate = Some(Instant.now))))

  override def update(id: BSONObjectID, user: ApprovalUser): Future[WriteResult] = collection
    .flatMap(_.update(ordered = false)
      .one(BSONDocument("_id" -> id), user.copy(_updateDate = Some(Instant.now))))

  def createIfNew(user: ApprovalUser): Future[Option[WriteResult]] = findByEmail(user.email)
    .flatMap{
      case Some(e) => Future.successful(None)
      case None => create(user).map(Some(_))
    }

  def findByEmail(email: String): Future[Option[ApprovalUser]] = collection
    .flatMap(_.find(BSONDocument("email" -> email.toLowerCase), Option.empty[ApprovalUser]).one[ApprovalUser])

}
