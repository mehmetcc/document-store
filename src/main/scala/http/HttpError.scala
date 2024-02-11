package http

import zio.http.{Response, Status}
import zio.json._

case class HttpError private (message: String)

object HttpError {
  implicit val httpErrorEncoder: JsonEncoder[HttpError] = DeriveJsonEncoder.gen[HttpError]
  implicit val httpErrorDecoder: JsonDecoder[HttpError] = DeriveJsonDecoder.gen[HttpError]

  def httpErrorResponse(message: String, status: Status): Response =
    Response.json(HttpError(message).toJson).status(status)
}
