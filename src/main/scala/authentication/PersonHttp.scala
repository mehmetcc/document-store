package authentication

import authentication.PersonHttp.Models.Implicits._
import authentication.PersonHttp.Models._
import infrastructure.{Configuration, Encryption, HttpError, HttpRequest}
import zio.http._
import zio.json._

object PersonHttp {

  val PersonApp: HttpApp[PersonDao with Encryption with PersonService with Configuration] =
    Routes(registerPerson, loginPerson).toHttpApp

  private def registerPerson: Route[PersonDao with Encryption with PersonService, Nothing] =
    Method.POST / "api" / "v1" / "person" / "register" -> handler { request: Request =>
      extractAndRegister(request).fold(success = registerResponseOnSuccess, failure = responseOnFailure)
    }

  private def extractAndRegister(request: Request) =
    for {
      parsed <- HttpRequest.fromRequestBody[RegisterPersonRequest](request)
      id <- PersonService.register(
              RegisterDto(email = parsed.email, username = parsed.username, password = parsed.password)
            )
    } yield id

  private def loginPerson: Route[Configuration with Encryption with PersonService, Nothing] =
    Method.POST / "api" / "v1" / "person" / "login" -> handler { (request: Request) =>
      extractAndLogin(request).fold(success = loginResponseOnSuccess, failure = responseOnFailure)
    }

  private def extractAndLogin(request: Request) =
    for {
      parsed <- HttpRequest.fromRequestBody[LoginPersonRequest](request)
      token  <- PersonService.login(LoginDto(email = parsed.email, password = parsed.password))
    } yield token

  private def registerResponseOnSuccess(id: Int): Response = Response.json(RegisterPersonResponse(id).toJson)

  private def loginResponseOnSuccess(token: String): Response = Response.json(LoginPersonResponse(token).toJson)

  private def responseOnFailure(throwable: Throwable): Response = throwable match {
    case PersonNotFoundException => HttpError.httpErrorResponse(PersonNotFoundException.getMessage, Status.NotFound)
    case FaultyPasswordException => HttpError.httpErrorResponse(FaultyPasswordException.getMessage, Status.Unauthorized)
    case UsernameAlreadyExistsException =>
      HttpError.httpErrorResponse(UsernameAlreadyExistsException.getMessage, Status.Conflict)
    case EmailAlreadyExistsException =>
      HttpError.httpErrorResponse(EmailAlreadyExistsException.getMessage, Status.Conflict)
    case other: Throwable => HttpError.httpErrorResponse(other.getMessage, Status.InternalServerError)
  }

  object Models {
    case class RegisterPersonRequest(username: String, email: String, password: String)
    case class RegisterPersonResponse(id: Int)

    case class LoginPersonRequest(email: String, password: String)
    case class LoginPersonResponse(token: String)

    object Implicits {
      implicit val registerPersonRequestEncoder: JsonEncoder[RegisterPersonRequest] =
        DeriveJsonEncoder.gen[RegisterPersonRequest]
      implicit val registerPersonRequestDecoder: JsonDecoder[RegisterPersonRequest] =
        DeriveJsonDecoder.gen[RegisterPersonRequest]

      implicit val registerPersonResponseEncoder: JsonEncoder[RegisterPersonResponse] =
        DeriveJsonEncoder.gen[RegisterPersonResponse]
      implicit val registerPersonResponseDecoder: JsonDecoder[RegisterPersonResponse] =
        DeriveJsonDecoder.gen[RegisterPersonResponse]

      implicit val loginPersonRequestEncoder: JsonEncoder[LoginPersonRequest] =
        DeriveJsonEncoder.gen[LoginPersonRequest]
      implicit val loginPersonRequestDecoder: JsonDecoder[LoginPersonRequest] =
        DeriveJsonDecoder.gen[LoginPersonRequest]

      implicit val loginPersonResponseEncoder: JsonEncoder[LoginPersonResponse] =
        DeriveJsonEncoder.gen[LoginPersonResponse]
      implicit val loginPersonResponseDecoder: JsonDecoder[LoginPersonResponse] =
        DeriveJsonDecoder.gen[LoginPersonResponse]
    }
  }
}
