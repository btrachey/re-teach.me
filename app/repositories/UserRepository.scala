package repositories

import models.User
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, BSONObjectID}
import reactivemongo.api.bson.compat._
import reactivemongo.api.commands.WriteResult

import java.time.Instant
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class UserRepository @Inject()(
                                        implicit executionContext: ExecutionContext,
                                        reactiveMongoApi: ReactiveMongoApi
                                      ) extends GenericRepository[User] {

  override def collection: Future[BSONCollection] = reactiveMongoApi.database.map(db => db.collection("users"))

  override def create(user: User): Future[WriteResult] = collection
    .flatMap(_.insert(ordered = false)
      .one(user.copy(_creationDate = Some(Instant.now), _updateDate = Some(Instant.now))))

  override def update(id: BSONObjectID, user: User): Future[WriteResult] = collection
    .flatMap(_.update(ordered = false)
      .one(BSONDocument("_id" -> id), user.copy(_updateDate = Some(Instant.now))))

  def createIfNew(user: User): Future[Option[WriteResult]] = findByEmail(user.email.get)
    .flatMap{
      case Some(e) => Future.successful(None)
      case None => create(user).map(Some(_))
    }

  def findByEmail(email: String): Future[Option[User]] = collection
    .flatMap(_.find(BSONDocument("email" -> email.toLowerCase), Option.empty[User]).one[User])

}
