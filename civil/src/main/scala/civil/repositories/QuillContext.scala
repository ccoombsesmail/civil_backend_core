package civil.repositories
import civil.config.Config
import civil.models.{CommentWithDepth, TribunalCommentWithDepth}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{PostgresJdbcContext, PostgresZioJdbcContext, Query, SnakeCase}

import java.util.UUID

class QuillContext extends PostgresZioJdbcContext(SnakeCase) with QuillCodecs

object QuillContextHelper extends QuillContext {
  lazy val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  pgDataSource.setUser("postgres")
  pgDataSource.setPassword("postgres")
  pgDataSource.setUrl(Config().getString("civil.databaseUrl"))
  val config = new HikariConfig()
  config.setDataSource(pgDataSource)
  lazy val ctx = new PostgresJdbcContext(SnakeCase, new HikariDataSource(config))
}

object QuillContextQueries extends QuillContext {
  val getCommentsWithReplies = quote { (id: UUID) =>
    infix"""
     WITH RECURSIVE comments_tree as (
      select
        c1.id,
        c1.content,
        c1.created_by,
        c1.sentiment,
        c1.subtopic_id,
        c1.parent_id,
        c1.created_at,
        c1.likes,
        c1.root_id,
        c1.source,
        c1.report_status,
        0 as depth
      from comments c1
       where c1.id = $id

       union all

      select
        c2.id,
        c2.content,
        c2.created_by,
        c2.sentiment,
        c2.subtopic_id ,
        c2.parent_id,
        c2.created_at,
        c2.likes,
        c2.root_id,
        c2.source,
        c2.report_status,
        depth + 1
      from comments c2
      join comments_tree ct on ct.id = c2.parent_id

    ) select * from comments_tree
      """.as[Query[CommentWithDepth]]
  }

  val getTribunalCommentsWithReplies = quote { (id: UUID) =>
    infix"""
     WITH RECURSIVE comments_tree as (
      select
        c1.id,
        c1.content,
        c1.created_by,
        c1.sentiment,
        c1.reported_content_id,
        c1.parent_id,
        c1.created_at,
        c1.likes,
        c1.root_id,
        c1.source,
        c1.comment_type,
        0 as depth
      from tribunal_comments c1
       where c1.id = $id

       union all

      select
        c2.id,
        c2.content,
        c2.created_by,
        c2.sentiment,
        c2.reported_content_id,
        c2.parent_id,
        c2.created_at,
        c2.likes,
        c2.root_id,
        c2.source,
        c2.comment_type,
        depth + 1
      from tribunal_comments c2
      join comments_tree ct on ct.id = c2.parent_id

    ) select * from comments_tree
      """.as[Query[TribunalCommentWithDepth]]
  }
}
