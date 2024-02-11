package infrastructure

import authentication.Reader
import zio.{Scope, ZIO}
import zio.test._
import zio.json._

object EncryptionSpec extends ZIOSpecDefault {
  private val email = "selaminaleykum@stringoglu.com"
  private val role  = Reader

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("Encryption Suite")(
    test("email given should generate a jwt token") {
      for {
        encrypted <- Encryption.jwtEncode(JwtClaimDto(email, role))
      } yield assertTrue(encrypted.nonEmpty)
    },
    test("token given should decode") {
      val token = Encryption.jwtEncode(JwtClaimDto(email, role))
      for {
        extracted <- token
        decoded   <- Encryption.jwtDecode(extracted)
      } yield assertTrue(decoded.isSuccess)
    }
  )
    .provide(Configuration.live, Encryption.live)
}
