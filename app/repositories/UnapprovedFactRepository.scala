package repositories

import models.{Fact, User}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.compat._
import reactivemongo.api.bson.{BSONDocument, BSONObjectID}
import reactivemongo.api.commands.WriteResult

import java.time.Instant
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class UnapprovedFactRepository @Inject()(
                                implicit executionContext: ExecutionContext,
                                reactiveMongoApi: ReactiveMongoApi,
                                factRepository: FactRepository
                              ) extends GenericRepository[Fact] {

  override def collection: Future[BSONCollection] = reactiveMongoApi.database.map(db => db.collection("unapprovedFacts"))

  override def create(fact: Fact): Future[WriteResult] = collection
    .flatMap(_.insert(ordered = false)
      .one(fact.copy(_creationDate = Some(Instant.now), _updateDate = Some(Instant.now))))

  override def update(id: BSONObjectID, fact: Fact): Future[WriteResult] = collection
    .flatMap(_.update(ordered = false)
      .one(BSONDocument("_id" -> id), fact.copy(_updateDate = Some(Instant.now))))

  def approveFact(id: BSONObjectID, approver: User): Future[WriteResult] =
    for {
      unapprovedFact <- findOne(id)
      writeResult <- factRepository.create(unapprovedFact.get.copy(approver = Some(approver)))
      _ <- delete(unapprovedFact.get._id.get)
    } yield writeResult

}
