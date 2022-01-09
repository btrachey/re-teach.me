package models

import play.api.libs.json.{Format, Json}
import reactivemongo.bson.{BSONObjectID, _}

import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import scala.language.implicitConversions


case class User(
                 _id: Option[BSONObjectID] = None,
                 _creationDate: Option[Instant] = None,
                 _updateDate: Option[Instant] = None,
                 email: Option[String] = None,
                 password: Option[String] = None,
                 firstName: Option[String] = None,
                 middleName: Option[String] = None,
                 lastName: Option[String] = None,
                 role: String = User.UserRole.Contributor
               )

object User extends HasBSONObjectID {

  def apply(userForm: UserCreationForm): User = new User(
    email = Some(userForm.email.toLowerCase),
    password = Some(userForm.password),
    firstName = Some(userForm.firstName),
    middleName = userForm.middleName,
    lastName = Some(userForm.lastName),
    role = UserRole.Contributor
  )

  def genericUser: User = new User(role = UserRole.Generic)

  implicit val fmt: Format[User] = Json.format[User]

  implicit object UserBSONReader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument): User =
      User(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONDateTime]("_creationDate").map(dt => Instant.ofEpochMilli(dt.value)),
        doc.getAs[BSONDateTime]("_updateDate").map(dt => Instant.ofEpochMilli(dt.value)),
        doc.getAs[String]("email"),
        doc.getAs[String]("password"),
        doc.getAs[String]("firstName"),
        doc.getAs[String]("middleName"),
        doc.getAs[String]("lastName"),
        doc.getAs[String]("role").get
      )
  }

  implicit object UserBSONWriter extends BSONDocumentWriter[User] {
    def write(user: User): BSONDocument = {
      BSONDocument(
        "_id" -> user._id,
        "_creationDate" -> user._creationDate.map(date => BSONDateTime(date.toEpochMilli)),
        "_updateDate" -> user._updateDate.map(date => BSONDateTime(date.toEpochMilli)),
        "email" -> user.email,
        "password" -> user.password,
        "firstName" -> user.firstName,
        "middleName" -> user.middleName,
        "lastName" -> user.lastName,
        "role" -> user.role
      )
    }
  }

  object PwManager {
    val DefaultIterations = 10000
    val random = new SecureRandom()

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

        case _ => None
      }
    }

    private def pbkdf2(password: String, salt: Array[Byte], iterations: Int): Array[Byte] = {
      val keySpec = new PBEKeySpec(password.toCharArray, salt, iterations, 256)
      val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      keyFactory.generateSecret(keySpec).getEncoded
    }
  }

  object UserRole extends Enumeration {
    val Generic = "Generic"
    val Contributor = "Contributor"
    val Reviewer = "Reviewer"
    val Admin = "Admin"
  }
}