package models

import play.api.libs.json.{Format, Json}
import reactivemongo.bson.{BSONObjectID, _}

import java.security.SecureRandom
import javax.crypto.spec.PBEKeySpec
import javax.crypto.{SecretKeyFactory, spec}
import java.time.Instant
import java.util.Base64

case class ApprovalUser(
                         _id: Option[BSONObjectID],
                         _creationDate: Option[Instant],
                         _updateDate: Option[Instant],
                         email: String,
                         password: Option[String],
                         firstName: String,
                         middleName: Option[String],
                         lastName: String,
                       )

object ApprovalUser extends HasBSONObjectID {

  def apply(userForm: UserCreationForm): ApprovalUser = new ApprovalUser(
    _id = None,
    _creationDate = None,
    _updateDate = None,
    email = userForm.email.toLowerCase,
    password = Some(userForm.password),
    firstName = userForm.firstName,
    middleName = userForm.middleName,
    lastName = userForm.lastName
  )

  implicit val fmt: Format[ApprovalUser] = Json.format[ApprovalUser]

  implicit object ApprovalUserBSONReader extends BSONDocumentReader[ApprovalUser] {
    def read(doc: BSONDocument): ApprovalUser = {
      ApprovalUser(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONDateTime]("_creationDate").map(dt => Instant.ofEpochMilli(dt.value)),
        doc.getAs[BSONDateTime]("_updateDate").map(dt => Instant.ofEpochMilli(dt.value)),
        doc.getAs[String]("email").get,
        doc.getAs[String]("password"),
        doc.getAs[String]("firstName").get,
        doc.getAs[String]("middleName"),
        doc.getAs[String]("lastName").get,
      )
    }
  }

  implicit object ApprovalUserBSONWriter extends BSONDocumentWriter[ApprovalUser] {
    def write(user: ApprovalUser): BSONDocument = {
      BSONDocument(
        "_id" -> user._id,
        "_creationDate" -> user._creationDate.map(date => BSONDateTime(date.toEpochMilli)),
        "_updateDate" -> user._updateDate.map(date => BSONDateTime(date.toEpochMilli)),
        "email" -> user.email,
        "password" -> user.password,
        "firstName" -> user.firstName,
        "middleName" -> user.middleName,
        "lastName" -> user.lastName
      )
    }
  }

  object PwManager {
    val DefaultIterations = 10000
    val random = new SecureRandom()

    private def pbkdf2(password: String, salt: Array[Byte], iterations: Int): Array[Byte] = {
      val keySpec = new PBEKeySpec(password.toCharArray, salt, iterations, 256)
      val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      keyFactory.generateSecret(keySpec).getEncoded
    }

    def hashPassword(password: String): String = {
      val salt = new Array[Byte](16)
      random.nextBytes(salt)
      val hash = pbkdf2(password, salt, DefaultIterations)
      val salt64 = Base64.getEncoder.encodeToString(salt)
      val hash64 = Base64.getEncoder.encodeToString(hash)

      s"$DefaultIterations:$hash64:$salt64"
    }

    def checkPassword(password: String, passwordHash: String): Option[Boolean] = {
      passwordHash.split(":") match {
        case Array(it, hash64, salt64) if it.forall(_.isDigit) =>
          val hash = Base64.getDecoder.decode(hash64)
          val salt = Base64.getDecoder.decode(salt64)

          val calculatedHash = pbkdf2(password, salt, it.toInt)
          Some(calculatedHash.sameElements(hash))

        case other => None
      }
    }
  }
}