package infrastructure

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.config.ReadError
import zio.http._
import zio._

import java.security.MessageDigest
import java.time.Clock
import scala.util.Try

object EncryptionUtils {
  def authenticationLogic: String => ZIO[Configuration with Encryption, Response, Boolean] = Encryption
    .jwtDecode(_)
    .flatMap(ZIO.fromTry(_))
    .mapBoth(t => HttpError.httpErrorResponse(t.getMessage, Status.Unauthorized), _ => true)
}

object Encryption {
  val live: ULayer[Encryption] = ZLayer.succeed(Encryption())

  def sha256(text: String): ZIO[Encryption, Throwable, String] = ZIO.serviceWithZIO[Encryption](_.sha256(text))

  def jwtEncode(email: String): ZIO[Configuration with Encryption, ReadError[String], String] =
    ZIO.serviceWithZIO[Encryption](_.jwtEncode(email))

  def jwtDecode(token: String): ZIO[Configuration with Encryption, ReadError[String], Try[JwtClaim]] =
    ZIO.serviceWithZIO[Encryption](_.jwtDecode(token))
}

case class Encryption() {
  implicit val clock: Clock = Clock.systemUTC // TODO find a way to work with ZIO-built-in clock instead of this?

  def sha256(password: String): Task[String] = ZIO.attempt(
    MessageDigest
      .getInstance("SHA-256")
      .digest(password.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
  )

  def jwtEncode(email: String): ZIO[Configuration, ReadError[String], String] = for {
    configuration <- Configuration.load
    json           = s"""{"email": "${email}"}"""
    claim          = JwtClaim(json).issuedNow.expiresIn(configuration.securityConfiguration.expiryTime)
    jwt            = Jwt.encode(claim, configuration.securityConfiguration.secretKey, JwtAlgorithm.HS512)
  } yield jwt

  def jwtDecode(token: String): ZIO[Configuration, ReadError[String], Try[JwtClaim]] = for {
    configuration <- Configuration.load
    decoded        = Jwt.decode(token, configuration.securityConfiguration.secretKey, Seq(JwtAlgorithm.HS512))
  } yield decoded
}
