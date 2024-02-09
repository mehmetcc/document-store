package infrastructure

import zio.config._
import zio.config.magnolia._
import zio.config.typesafe._
import zio.{IO, ULayer, ZIO, ZLayer}

final case class ApplicationConfiguration(
  httpConfiguration: HttpConfiguration,
  securityConfiguration: SecurityConfiguration
)

final case class HttpConfiguration(port: Int)

final case class SecurityConfiguration(secretKey: String, expiryTime: Int)

object Configuration {
  val live: ULayer[Configuration] = ZLayer.succeed(Configuration())

  def load: ZIO[Configuration, ReadError[String], ApplicationConfiguration] = ZIO.serviceWithZIO[Configuration](_.load)
}

case class Configuration() {
  def load: IO[ReadError[String], ApplicationConfiguration] = read {
    descriptor[ApplicationConfiguration].mapKey(toKebabCase).from(TypesafeConfigSource.fromResourcePath)
  }
}
