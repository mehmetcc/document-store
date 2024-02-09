package infrastructure

import zio.http._
import zio.json._
import zio.{Task, ZIO}

object HttpRequest {
  def fromRequestBody[A](request: Request)(implicit decoder: JsonDecoder[A]): Task[A] =
    extractFromRequestBody(request.body).flatMap {
      case Left(failure) => ZIO.fail(new Throwable(failure))
      case Right(value)  => ZIO.succeed(value)
    }

  private def extractFromRequestBody[A](body: Body)(implicit decoder: JsonDecoder[A]): Task[Either[String, A]] = for {
    body   <- body.asString(Charsets.Utf8)
    request = body.fromJson[A]
  } yield request
}
