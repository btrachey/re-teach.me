package models

import play.api.libs.json.{Format, Json}
import reactivemongo.bson.{BSONObjectID, BSONArray, BSONValue, _}

import java.time.Instant

case class Fact(
                 _id: Option[BSONObjectID] = None,
                 _creationDate: Option[Instant] = None,
                 _updateDate: Option[Instant] = None,
                 title: String,
                 description: String,
                 references: Option[Map[String, String]] = None
               )

object Fact extends HasBSONObjectID {

  def fromForm(factCreateForm: FactCreateForm, references: Map[String, String]): Fact = new Fact(
    _id = Some(BSONObjectID.generate()),
    title = factCreateForm.title,
    description = factCreateForm.description,
    references = Some(references)
  )

  implicit val fmt: Format[Fact] = Json.format[Fact]

  implicit object FactBSONReader extends BSONDocumentReader[Fact] {
    def read(doc: BSONDocument): Fact = {
      Fact(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONDateTime]("_creationDate").map(dt => Instant.ofEpochMilli(dt.value)),
        doc.getAs[BSONDateTime]("_updateDate").map(dt => Instant.ofEpochMilli(dt.value)),
        doc.getAs[String]("title").get,
        doc.getAs[String]("description").get,
        doc.getAs[List[String]]("references").map(_.map(elem => elem.split(":")).map(arr => arr(0) -> arr(1)).toMap)
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
        "description" -> fact.description,
        "references" -> fact.references.map(_.map(tuple => s"${tuple._1}:${tuple._2}"))
      )
    }
  }
}