package repositories

import models.Fact
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, BSONObjectID}
import reactivemongo.api.bson.compat._
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, ReadPreference}

import java.time.Instant
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class FactRepository @Inject()(
                                implicit executionContext: ExecutionContext,
                                reactiveMongoApi: ReactiveMongoApi
                              ) extends GenericRepository[Fact] {

  override def collection: Future[BSONCollection] = reactiveMongoApi.database.map(db => db.collection("facts"))

  override def create(fact: Fact): Future[WriteResult] = collection
    .flatMap(_.insert(ordered = false)
      .one(fact.copy(_creationDate = Some(Instant.now), _updateDate = Some(Instant.now))))

  override def update(id: BSONObjectID, fact: Fact): Future[WriteResult] = collection
    .flatMap(_.update(ordered = false)
      .one(BSONDocument("_id" -> id), fact.copy(_updateDate = Some(Instant.now))))

  def findGTEYear(year: Int): Future[Seq[Fact]] = collection
    .flatMap(_.find(BSONDocument("year" -> BSONDocument("$gte" -> year)), Option.empty[Fact])
      .cursor[Fact](ReadPreference.Primary)
      .collect[Seq](100, Cursor.FailOnError[Seq[Fact]]()))

}
