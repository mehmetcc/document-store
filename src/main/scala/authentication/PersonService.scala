package authentication

import infrastructure.{Configuration, Encryption}
import zio.{Task, ZIO, ZLayer}

private[authentication] case object PersonNotFoundException
    extends Throwable("User with given email cannot be found!")

private[authentication] case object FaultyPasswordException extends Throwable("Given password is not right!")

private[authentication] case object UsernameAlreadyExistsException extends Throwable("Username already exists!")

private[authentication] case object EmailAlreadyExistsException extends Throwable("Email already exists!")

private[authentication] case class RegisterDto(username: String, email: String, password: String)

private[authentication] case class LoginDto(email: String, password: String)

object PersonService {
  val live: ZLayer[PersonDao, Nothing, PersonService] = ZLayer.fromFunction(PersonService(_))

  def register(dto: RegisterDto): ZIO[PersonDao with Encryption with PersonService, Throwable, Int] =
    ZIO.serviceWithZIO[PersonService](_.register(dto))

  def login(dto: LoginDto): ZIO[Configuration with Encryption with PersonService, Throwable, String] =
    ZIO.serviceWithZIO[PersonService](_.login(dto))
}

case class PersonService(dao: PersonDao) {
  def register(dto: RegisterDto): ZIO[PersonDao with Encryption, Throwable, Int] = for {
    maybeExistingUsername <- dao.readByUsername(dto.username)
    maybeExistingEmail    <- dao.readByEmail(dto.email)
    id <- if (maybeExistingEmail.isEmpty && maybeExistingEmail.isEmpty) createNewUser(dto)
          else if (maybeExistingEmail.nonEmpty) ZIO.fail(EmailAlreadyExistsException)
          else if (maybeExistingUsername.nonEmpty) ZIO.fail(UsernameAlreadyExistsException)
          else ZIO.fail(new Throwable("Something went wrong"))
  } yield id

  private def createNewUser(dto: RegisterDto): ZIO[PersonDao with Encryption, Throwable, Int] =
    PersonDao.create(dto)

  def login(dto: LoginDto): ZIO[Configuration with Encryption, Throwable, String] = for {
    found     <- dao.readByEmail(dto.email)
    extracted <- extractUser(found)
    encrypted <- Encryption.sha256(dto.password)
    encoded   <- Encryption.jwtEncode(dto.email)
    result <- if (extracted.password == encrypted) ZIO.succeed(encoded)
              else ZIO.fail(FaultyPasswordException)
  } yield result

  private def extractUser(maybeUser: Option[Person]): Task[Person] = maybeUser match {
    case Some(value) => ZIO.succeed(value)
    case None        => ZIO.fail(PersonNotFoundException)
  }
}
