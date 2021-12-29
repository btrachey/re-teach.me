package models

import play.api.libs.json.{Format, Json}
import reactivemongo.bson._

import java.time.Instant

case class CustomSession(
                    _id: Option[BSONObjectID],
                    token: Option[String],
                    email: String,
                    expiration: Option[Instant]
                  ) {
  def isExpired: Boolean = expiration.forall(_.isBefore(Instant.now))
}

object CustomSession extends HasBSONObjectID {
  def apply(email: String): CustomSession = new CustomSession(
    _id = None,
    token = None,
    email = email,
    expiration = None
  )

  implicit val fmt: Format[CustomSession] = Json.format[CustomSession]

  implicit object SessionBSONReader extends BSONDocumentReader[CustomSession] {
    def read(doc: BSONDocument): CustomSession = {
      CustomSession(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("token"),
        doc.getAs[String]("email").get,
        doc.getAs[BSONDateTime]("expiration").map(dt => Instant.ofEpochMilli(dt.value))
      )
    }
  }

  implicit object SessionBSONWriter extends BSONDocumentWriter[CustomSession] {
    def write(session: CustomSession): BSONDocument = {
      BSONDocument(
        "_id" -> session._id,
        "token" -> session.token,
        "email" -> session.email,
        "expiration" -> session.expiration.map(date => BSONDateTime(date.toEpochMilli))
      )
    }
  }
}
