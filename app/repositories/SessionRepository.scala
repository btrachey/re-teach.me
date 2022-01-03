package repositories

import models.CustomSession
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.compat._
import reactivemongo.api.bson.{BSONDocument, BSONObjectID}
import reactivemongo.api.commands.WriteResult

import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS
import java.util.UUID
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class SessionRepository @Inject()(
                                   implicit executionContext: ExecutionContext,
                                   reactiveMongoApi: ReactiveMongoApi
                                 ) extends GenericRepository[CustomSession] {

  override def collection: Future[BSONCollection] = reactiveMongoApi.database.map(db => db.collection("session"))

  override def create(session: CustomSession): Future[WriteResult] = collection
    .flatMap(_.insert(ordered = false)
      .one(copySession(session)))

  override def update(id: BSONObjectID, session: CustomSession): Future[WriteResult] = collection
    .flatMap(_.update(ordered = false)
      .one(BSONDocument("_id" -> id), copySession(session)))

  def findByToken(token: String): Future[Option[CustomSession]] = collection
    .flatMap(_.find(BSONDocument("token" -> token), Option.empty[CustomSession]).one[CustomSession]).map(_.filter(!_.isExpired))

  def findByUser(email: String): Future[Option[CustomSession]] = collection
    .flatMap(_.find(BSONDocument("email" -> email), Option.empty[CustomSession]).one[CustomSession]).map(_.filter(!_.isExpired))

  def isValidToken(token: String): Future[Boolean] = findByToken(token)
    .map(_.exists(!_.isExpired))

  private def copySession(session: CustomSession): CustomSession = {
    val expiration: Instant = Instant.now.plus(4, HOURS)
    session.copy(
      token = Some(s"${session.email}:token:${UUID.randomUUID().toString}"),
      expiration = Some(expiration)
    )
  }
}
