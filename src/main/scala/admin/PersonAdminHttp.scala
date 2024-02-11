package admin

import admin.PersonAdminHttp.Models.Implicits._
import admin.PersonAdminHttp.Models._
import authentication.{Person, PersonDao}
import http.CommonExceptions.PaginationException
import http.HttpError
import http.HttpMiddleware.onlyAdmin
import infrastructure.{Configuration, Encryption}
import zio.ZIO
import zio.http._
import zio.json._

object PersonAdminHttp {
  private val PersonAdminRoutes: Routes[Configuration with Encryption with PersonDao, Nothing] =
    Routes(getPersonsPaginated) @@ onlyAdmin

  val PersonAdminApp: HttpApp[Configuration with Encryption with PersonDao] = PersonAdminRoutes.toHttpApp

  private def getPersonsPaginated: Route[PersonDao, Nothing] =
    Method.GET / "api" / "v1" / "admin" / "person" -> handler {
      request: Request =>
        paginatePersons(request).fold(success = getPersonsPaginatedResponseOnSuccess, failure = responseOnFailure)
    }

  private def paginatePersons(request: Request): ZIO[PersonDao, Throwable, List[Person]] =
    if (request.url.queryParams.nonEmpty)
      ZIO
        .fromOption(extractPaginationQueries(request.url.queryParams))
        .orElseFail(PaginationException)
        .flatMap(result => PersonDao.readFirstN(result.limit, result.page))
        .map(_.map(_.copy(password = "******")))
    else ZIO.fail(PaginationException)

  private case class PageAndLimit(page: Int, limit: Int)

  private def extractPaginationQueries(parameters: QueryParams): Option[PageAndLimit] = for {
    page  <- parameters.get("page")
    limit <- parameters.get("limit")
  } yield PageAndLimit(page.toInt, limit.toInt)

  private def getPersonsPaginatedResponseOnSuccess(persons: List[Person]) =
    Response.json(GetPersonsPaginatedResponse(persons).toJson)

  private def responseOnFailure(throwable: Throwable): Response = throwable match {
    case PaginationException => HttpError.httpErrorResponse(PaginationException.getMessage, Status.BadRequest)
    case other: Throwable    => HttpError.httpErrorResponse(other.getMessage, Status.InternalServerError)
  }

  object Models {
    case class GetPersonsPaginatedResponse(persons: List[Person])

    object Implicits {
      implicit val getPersonsPaginatedResponseEncoder: JsonEncoder[GetPersonsPaginatedResponse] =
        DeriveJsonEncoder.gen[GetPersonsPaginatedResponse]
      implicit val getPersonsPaginatedResponseDecoder: JsonDecoder[GetPersonsPaginatedResponse] =
        DeriveJsonDecoder.gen[GetPersonsPaginatedResponse]
    }
  }
}
