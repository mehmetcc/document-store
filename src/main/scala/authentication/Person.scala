package authentication

import io.getquill.MappedEncoding
import zio.{IO, Task, ZIO}

case class PersonValidationException(message: String) extends Throwable(message)

sealed trait PersonRole

object PersonRole {
  implicit val encodePersonRole: MappedEncoding[PersonRole, String] = MappedEncoding[PersonRole, String] {
    case Reader  => "Reader"
    case Creator => "Creator"
    case Admin   => "Admin"
  }
  implicit val decodePersonRole: MappedEncoding[String, PersonRole] = MappedEncoding[String, PersonRole] {
    case "Reader"  => Reader
    case "Creator" => Creator
    case "Admin"   => Admin
  }
}

case object Reader  extends PersonRole
case object Creator extends PersonRole
case object Admin   extends PersonRole

case class Person(personId: Int = 0, username: String, email: String, password: String, role: PersonRole)

object Person {
  def from(personId: Int, username: String, email: String, password: String): Task[Person] = ZIO
    .collectAllPar(List(validateUsername(username), validateEmail(email), validatePassword(password)))
    .withParallelism(3)
    .map(details => Person(personId, details.head, details(1), details(2), Reader))
    .catchAll(errors => ZIO.fail(PersonValidationException(errors)))

  def fromRegisterDto(dto: RegisterDto): Task[Person] = ZIO
    .collectAllPar(
      List(
        validateUsername(dto.username),
        validateEmail(dto.email),
        validatePassword(dto.password)
      )
    )
    .withParallelism(3)
    .map(details => Person(0, details.head, details(1), details(2), Reader))
    .catchAll(errors => ZIO.fail(PersonValidationException(errors)))

  private def validateUsername(username: String): IO[String, String] =
    if (username.matches("^[a-zA-Z0-9]+")) ZIO.succeed(username)
    else ZIO.fail(s"$username should be alphanumeric")

  private def validateEmail(email: String): IO[String, String] = {
    // taken from https://stackoverflow.com/questions/13912597/validate-email-one-liner-in-scala
    val emailRegex =
      """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

    def check(e: String): Boolean = e match {
      case null                                          => false
      case e if e.trim.isEmpty                           => false
      case e if emailRegex.findFirstMatchIn(e).isDefined => true
      case _                                             => false
    }

    if (check(email)) ZIO.succeed(email)
    else ZIO.fail(s"$email should be a valid email")
  }

  private def validatePassword(password: String): IO[String, String] =
    if (password.length >= 6) ZIO.succeed(password)
    else ZIO.fail("password should be at least 6 digits")
}
