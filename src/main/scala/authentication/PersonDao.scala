package authentication

import authentication.PersonRole._
import infrastructure.Encryption
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import java.sql.SQLException

object PersonDao {
  val live: ZLayer[Quill.Postgres[SnakeCase], Nothing, PersonDao] = ZLayer.fromFunction(PersonDao.apply _)

  def readById(id: Int): ZIO[PersonDao, SQLException, Option[Person]] = ZIO.serviceWithZIO[PersonDao](_.readById(id))

  def readByUsername(username: String): ZIO[PersonDao, SQLException, Option[Person]] =
    ZIO.serviceWithZIO[PersonDao](_.readByUsername(username))

  def readByEmail(email: String): ZIO[PersonDao, SQLException, Option[Person]] =
    ZIO.serviceWithZIO[PersonDao](_.readByEmail(email))

  def readAll: ZIO[PersonDao, SQLException, List[Person]] = ZIO.serviceWithZIO[PersonDao](_.readAll)

  def create(dto: RegisterDto): ZIO[Encryption with PersonDao, Throwable, Int] =
    ZIO.serviceWithZIO[PersonDao](_.create(dto))
}

case class PersonDao(quill: Quill.Postgres[SnakeCase]) {
  import quill._
  def readById(id: Int): ZIO[Any, SQLException, Option[Person]] =
    run(query[Person].filter(_.personId == lift(id))).map(_.headOption)

  def readByUsername(username: String): ZIO[Any, SQLException, Option[Person]] = run {
    query[Person].filter(_.username == lift(username))
  }.map(_.headOption)

  def readByEmail(email: String): ZIO[Any, SQLException, Option[Person]] = run {
    query[Person].filter(_.email == lift(email))
  }.map(_.headOption)

  def readAll: ZIO[Any, SQLException, List[Person]] = run(query[Person])

  def create(dto: RegisterDto): ZIO[Encryption, Throwable, Int] =
    for {
      person    <- Person.fromRegisterDto(dto)
      encrypted <- Encryption.sha256(dto.password)
      updated    = person.copy(password = encrypted)
      id <- run(
              query[Person]
                .insertValue(lift(updated))
                .returningGenerated(generated => generated.personId)
            )
    } yield id
}
