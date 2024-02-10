import authentication.PersonHttp.PersonApp
import authentication.{PersonDao, PersonService}
import document.DocumentDao
import document.DocumentHttp.DocumentApp
import infrastructure.{Configuration, Encryption}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio._
import zio.http._

object Main extends ZIOAppDefault {
  private lazy val App = PersonApp ++ DocumentApp

  private lazy val program = for {
    configuration <- Configuration.load
    port           = configuration.httpConfiguration.port
    server        <- Server.serve(App)
  } yield ()

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    program.provide(
      Encryption.live,
      Configuration.live,
      PersonDao.live,
      PersonService.live,
      DocumentDao.live,
      Server.default,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("database-configuration")
    )
}
