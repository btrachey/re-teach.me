package models

import play.api.libs.json.{Format, Json}
import reactivemongo.bson.{BSONObjectID, _}

import java.time.Instant

case class Fact(
                 _id: Option[BSONObjectID] = None,
                 _creationDate: Option[Instant] = None,
                 _updateDate: Option[Instant] = None,
                 title: String,
                 description: String
               )

object Fact extends HasBSONObjectID {
  def apply(factCreateForm: FactCreateForm): Fact = apply(factCreateForm.title, factCreateForm.description)

  def apply(title: String, description: String): Fact = new Fact(
    _id = Some(BSONObjectID.generate()),
    title = title,
    description = description
  )

  implicit val fmt: Format[Fact] = Json.format[Fact]

  implicit object FactBSONReader extends BSONDocumentReader[Fact] {
    def read(doc: BSONDocument): Fact = {
      Fact(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONDateTime]("_creationDate").map(dt => Instant.ofEpochMilli(dt.value)),
        doc.getAs[BSONDateTime]("_updateDate").map(dt => Instant.ofEpochMilli(dt.value)),
        doc.getAs[String]("title").get,
        doc.getAs[String]("description").get
      )
    }
  }

  implicit object FactBSONWriter extends BSONDocumentWriter[Fact] {
    def write(fact: Fact): BSONDocument = {
      BSONDocument(
        "_id" -> fact._id,
        "_creationDate" -> fact._creationDate.map(date => BSONDateTime(date.toEpochMilli)),
        "_updateDate" -> fact._updateDate.map(date => BSONDateTime(date.toEpochMilli)),
        "title" -> fact.title,
        "description" -> fact.description
      )
    }
  }
}