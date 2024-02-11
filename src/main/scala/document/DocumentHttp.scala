package document

import document.DocumentHttp.Models.Implicits._
import document.DocumentHttp.Models._
import http.HttpMiddleware.{onlyAdmin, onlyAuthenticated, onlyCreator}
import http.{HttpError, HttpRequest}
import infrastructure.{Configuration, Encryption}
import zio.http._
import zio.json._

object DocumentHttp {
  private val DocumentRoutes = Routes(createDocument)

  val DocumentApp: HttpApp[Configuration with Encryption with DocumentDao] = DocumentRoutes.toHttpApp

  private def createDocument: Route[Configuration with Encryption with DocumentDao, Nothing] =
    Method.POST / "api" / "v1" / "document" -> handler { request: Request =>
      extractAndCreate(request).fold(success = createDocumentResponseOnSuccess, failure = responseOnFailure)
    } @@ onlyCreator

  private def extractAndCreate(request: Request) = for {
    parsed <- HttpRequest.fromRequestBody[CreateDocumentRequest](request)
    id     <- DocumentDao.create(DocumentDto(content = parsed.content))
  } yield id

  private def createDocumentResponseOnSuccess(id: Int): Response = Response.json(CreateDocumentResponse(id).toJson)

  private def responseOnFailure(throwable: Throwable): Response = throwable match {
    case other: Throwable => HttpError.httpErrorResponse(other.getMessage, Status.InternalServerError)
  }

  object Models {
    case class CreateDocumentRequest(content: String)
    case class CreateDocumentResponse(id: Int)

    object Implicits {
      implicit val createDocumentRequestEncoder: JsonEncoder[CreateDocumentRequest] =
        DeriveJsonEncoder.gen[CreateDocumentRequest]
      implicit val createDocumentRequestDecoder: JsonDecoder[CreateDocumentRequest] =
        DeriveJsonDecoder.gen[CreateDocumentRequest]

      implicit val createDocumentResponseEncoder: JsonEncoder[CreateDocumentResponse] =
        DeriveJsonEncoder.gen[CreateDocumentResponse]
      implicit val createDocumentResponseDecoder: JsonDecoder[CreateDocumentResponse] =
        DeriveJsonDecoder.gen[CreateDocumentResponse]

    }
  }
}
