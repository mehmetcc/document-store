package infrastructure

import authentication.PersonRole
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio._
import zio.config.ReadError
import zio.json._

import java.security.MessageDigest
import java.time.Clock
import scala.util.Try

case class JwtClaimDto(email: String, role: PersonRole)

object JwtClaimDto {
  def fromClaim(claim: JwtClaim): Either[String, JwtClaimDto] = claim.content.fromJson[JwtClaimDto]

  def fromClaimZIO(claim: JwtClaim): Task[JwtClaimDto] = ZIO.fromEither(fromClaim(claim)).mapError(new Throwable(_))

  implicit val jwtClaimDtoEncoder: JsonEncoder[JwtClaimDto] = DeriveJsonEncoder.gen[JwtClaimDto]
  implicit val jwtClaimDtoDecoder: JsonDecoder[JwtClaimDto] = DeriveJsonDecoder.gen[JwtClaimDto]
}

object Encryption {
  val live: ULayer[Encryption] = ZLayer.succeed(Encryption())

  def sha256(text: String): ZIO[Encryption, Throwable, String] = ZIO.serviceWithZIO[Encryption](_.sha256(text))

  def jwtEncode(dto: JwtClaimDto): ZIO[Configuration with Encryption, ReadError[String], String] =
    ZIO.serviceWithZIO[Encryption](_.jwtEncode(dto))

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

  def jwtEncode(dto: JwtClaimDto): ZIO[Configuration, ReadError[String], String] = for {
    configuration <- Configuration.load
    json           = dto.toJson
    claim          = JwtClaim(json).issuedNow.expiresIn(configuration.securityConfiguration.expiryTime)
    jwt            = Jwt.encode(claim, configuration.securityConfiguration.secretKey, JwtAlgorithm.HS512)
  } yield jwt

  def jwtDecode(token: String): ZIO[Configuration, ReadError[String], Try[JwtClaim]] = for {
    configuration <- Configuration.load
    decoded        = Jwt.decode(token, configuration.securityConfiguration.secretKey, Seq(JwtAlgorithm.HS512))
  } yield decoded
}
