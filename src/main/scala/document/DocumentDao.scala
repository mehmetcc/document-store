package document

import authentication.PersonRole
import infrastructure.Encryption
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import java.sql.SQLException

private[document] case class DocumentDto(content: String)

object DocumentDao {
  val live: ZLayer[Quill.Postgres[SnakeCase], Nothing, DocumentDao] = ZLayer.fromFunction(DocumentDao.apply _)

  def readById(id: Int): ZIO[DocumentDao, SQLException, Option[Document]] =
    ZIO.serviceWithZIO[DocumentDao](_.readById(id))

  def readAll: ZIO[DocumentDao, SQLException, List[Document]] = ZIO.serviceWithZIO[DocumentDao](_.readAll)

  def create(dto: DocumentDto): ZIO[Encryption with DocumentDao, Throwable, Int] =
    ZIO.serviceWithZIO[DocumentDao](_.create(dto))
}

case class DocumentDao(quill: Quill.Postgres[SnakeCase]) {
  import quill._

  def readById(id: Int): ZIO[Any, SQLException, Option[Document]] =
    run(query[Document].filter(_.documentId == lift(id))).map(_.headOption)

  def readAll: ZIO[Any, SQLException, List[Document]] = run(query[Document])

  def create(dto: DocumentDto): ZIO[Encryption, Throwable, Int] =
    run {
      query[Document]
        .insertValue(lift(Document(documentId = 0, content = dto.content)))
        .returningGenerated(generated => generated.documentId)
    }
}
