import reactivemongo.play.json.BSONObjectIDFormat
import reactivemongo.bson.BSONObjectID
import play.api.libs.json.Format

package object models {
  trait HasBSONObjectID {
    implicit val bsonObjectIdFormat: Format[BSONObjectID] = BSONObjectIDFormat
  }
  case class UserCreationForm(email: String, password: String, firstName: String, middleName: Option[String], lastName: String)
  case class LoginForm(email: String, password: String)
  case class FactCreateForm(year: Int, title: String, description: String, references: Seq[String])
}
