package repositories

import models.Fact
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.{BSONDocument, BSONObjectID}
import reactivemongo.api.bson.compat._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, ReadPreference}

import java.time.Instant
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class OldFactRepository @Inject()(
                                implicit executionContext: ExecutionContext,
                                reactiveMongoApi: ReactiveMongoApi
                              ) {
  def collection: Future[BSONCollection] = reactiveMongoApi.database.map(db => db.collection("facts"))

  def findAll(limit: Int = 100): Future[Seq[Fact]] = collection
    .flatMap(_.find(BSONDocument(), Option.empty[Fact])
    .cursor[Fact](ReadPreference.Primary)
    .collect[Seq](limit, Cursor.FailOnError[Seq[Fact]]()))

  def findOne(id: BSONObjectID): Future[Option[Fact]] = collection
    .flatMap(_.find(BSONDocument("_id" -> id), Option.empty[Fact])
    .one[Fact])

  def create(fact: Fact): Future[WriteResult] = collection
    .flatMap(_.insert(ordered = false)
    .one(fact.copy(_creationDate = Some(Instant.now), _updateDate = Some(Instant.now))))

  def update(id: BSONObjectID, fact: Fact): Future[WriteResult] = collection
    .flatMap(_.update(ordered = false)
    .one(BSONDocument("_id" -> id), fact.copy(_updateDate = Some(Instant.now))))

  def delete(id: BSONObjectID): Future[WriteResult] = collection
    .flatMap(_.delete().one(BSONDocument("_id" -> id), Some(1)))

}
