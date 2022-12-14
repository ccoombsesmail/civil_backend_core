package civil.repositories

import civil.models.enums.{ReportStatus, TopicCategories}
import civil.models.enums.TopicCategories

import java.sql.Types.{OTHER, VARCHAR}
import java.util.UUID
import io.getquill.context.jdbc.PostgresJdbcRunContext
import org.postgresql.util.PGobject

import java.sql.Types


trait QuillCodecs {

  this: PostgresJdbcRunContext[_] =>

//    implicit val topicCategoriesEncoder: Encoder[TopicCategories] = encoder[TopicCategories](
//      Types.VARCHAR,
//      (index: Index, value: TopicCategories, row: PrepareRow) => {
//        val pgObj = new PGobject()
//        pgObj.setType("topic_categories")
//        pgObj.setValue(value.entryName)
//        row.setObject(index, pgObj, Types.VARCHAR)
//      }
//    )
  implicit val topicCategoriesDecoder: Decoder[TopicCategories] =
    decoder(row => index => TopicCategories.withNameInsensitive(row.getObject(index).toString))
  implicit val topicCategoriesEncoder: Encoder[TopicCategories] =
    encoder(VARCHAR, (index, value, row) => row.setObject(index, value, OTHER))

  implicit val reportStatusDecoder: Decoder[ReportStatus] =
    decoder(row => index => ReportStatus.withNameInsensitive(row.getObject(index).toString))
  implicit val reportStatusEncoder: Encoder[ReportStatus] =
    encoder(VARCHAR, (index, value, row) => row.setObject(index, value, OTHER))
}
