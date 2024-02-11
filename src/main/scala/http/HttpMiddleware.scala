package http

import authentication.{Admin, Creator, Reader}
import infrastructure.{Configuration, Encryption, JwtClaimDto}
import zio._
import zio.http.Middleware.bearerAuthZIO
import zio.http._

object HttpMiddleware {
  def onlyAuthenticated: HandlerAspect[Configuration with Encryption, Unit] = bearerAuthZIO(authenticationLogic)
  private def authenticationLogic: String => ZIO[Configuration with Encryption, Response, Boolean] = Encryption
    .jwtDecode(_)
    .flatMap(ZIO.fromTry(_))
    .mapBoth(t => HttpError.httpErrorResponse(t.getMessage, Status.Unauthorized), _ => true)

  def onlyAdmin: HandlerAspect[Configuration with Encryption, Unit] = onlyAuthenticated ++ bearerAuthZIO(onlyAdminLogic)

  private def onlyAdminLogic: String => ZIO[Configuration with Encryption, Response, Boolean] = Encryption
    .jwtDecode(_)
    .flatMap(ZIO.fromTry(_))
    .flatMap(JwtClaimDto.fromClaimZIO)
    .map(person =>
      person.role match {
        case Reader  => false
        case Creator => false
        case Admin   => true
      }
    )
    .mapError(t => HttpError.httpErrorResponse(t.getMessage, Status.Unauthorized))

  def onlyCreator: HandlerAspect[Configuration with Encryption, Unit] =
    onlyAuthenticated ++ bearerAuthZIO(onlyCreatorLogic)

  private def onlyCreatorLogic: String => ZIO[Configuration with Encryption, Response, Boolean] = Encryption
    .jwtDecode(_)
    .flatMap(ZIO.fromTry(_))
    .flatMap(JwtClaimDto.fromClaimZIO)
    .map(person =>
      person.role match {
        case Reader  => false
        case Creator => true
        case Admin   => true
      }
    )
    .mapError(t => HttpError.httpErrorResponse(t.getMessage, Status.Unauthorized))
}
