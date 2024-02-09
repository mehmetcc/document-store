import authentication.PersonHttp.PersonApp
import authentication.{PersonDao, PersonService}
import infrastructure.{Configuration, Encryption}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio._
import zio.http.Server

object Main extends ZIOAppDefault {
  def app = PersonApp

  private lazy val program = for {
    configuration <- Configuration.load
    port           = configuration.httpConfiguration.port
    server        <- Server.serve(app)
  } yield ()

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    program.provide(
      Encryption.live,
      Configuration.live,
      PersonDao.live,
      PersonService.live,
      Server.default,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("database-configuration")
    )
}
