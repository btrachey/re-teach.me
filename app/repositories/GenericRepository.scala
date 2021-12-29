package repositories

import play.modules.reactivemongo.ReactiveMongoApi
import play.api.Logging
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.compat._
import reactivemongo.api.bson.{BSONDocument, BSONDocumentWriter, BSONObjectID}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.BSONDocumentReader

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

abstract class GenericRepository[A] @Inject()(
                                implicit executionContext: ExecutionContext,
                                implicit val documentReader: BSONDocumentReader[A],
                                implicit val documentWriter: BSONDocumentWriter[A],
                                reactiveMongoApi: ReactiveMongoApi
                              ) extends Logging {
  def collection: Future[BSONCollection]

  def findAll(limit: Int = 100): Future[Seq[A]] = collection
    .flatMap(_.find(BSONDocument(), Option.empty[A])
    .cursor[A](ReadPreference.Primary)
    .collect[Seq](limit, Cursor.FailOnError[Seq[A]]()))

  def findOne(id: BSONObjectID): Future[Option[A]] = collection
    .flatMap(_.find(BSONDocument("_id" -> id), Option.empty[A])
    .one[A])

  def delete(id: BSONObjectID): Future[WriteResult] = collection
    .flatMap(_.delete().one(BSONDocument("_id" -> id), Some(1)))

  def create(obj: A): Future[WriteResult]

  def update(id: BSONObjectID, obj: A): Future[WriteResult]

}
