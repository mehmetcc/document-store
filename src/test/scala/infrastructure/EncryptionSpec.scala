package infrastructure

import zio.{Scope, ZIO}
import zio.test._

object EncryptionSpec extends ZIOSpecDefault {
  private val email = "string@stringoglu.com"
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("Encryption Suite")(
    test("email given should generate a jwt token") {
      for {
        encrypted <- Encryption.jwtEncode(email)
      } yield assertTrue(encrypted.nonEmpty)
    },
    test("token given should decode to correct mail") {
      val token = Encryption.jwtEncode(email)
      for {
        extracted <- token
        decoded   <- Encryption.jwtDecode(extracted)
      } yield assertTrue(decoded.isSuccess)
    }
  )
    .provide(Configuration.live, Encryption.live)
}
