package civil.repositories.comments

import civil.models._
import civil.repositories.{QuillContextHelper, QuillContextQueries}
import civil.utils.CommentsTreeConstructor
import io.scalaland.chimney.dsl._
import zio._

import java.util.UUID

trait CommentsRepository {
  def insertComment(comment: Comments): ZIO[Any, ErrorInfo, CommentReply]
  def getComments(
      userId: String,
      subtopicId: UUID
  ): ZIO[Any, ErrorInfo, List[CommentNode]]
  def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentReply]

  def getAllCommentReplies(
      userId: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentWithReplies]
}

object CommentsRepository {
  def insertComment(
      comment: Comments
  ): ZIO[Has[CommentsRepository], ErrorInfo, CommentReply] =
    ZIO.serviceWith[CommentsRepository](_.insertComment(comment))
  def getComments(
      userId: String,
      subtopicId: UUID
  ): ZIO[Has[CommentsRepository], ErrorInfo, List[CommentNode]] =
    ZIO.serviceWith[CommentsRepository](_.getComments(userId, subtopicId))
  def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[Has[CommentsRepository], ErrorInfo, CommentReply] =
    ZIO.serviceWith[CommentsRepository](_.getComment(userId, commentId))
  def getAllCommentReplies(
      userId: String,
      commentId: UUID
  ): ZIO[Has[CommentsRepository], ErrorInfo, CommentWithReplies] =
    ZIO.serviceWith[CommentsRepository](
      _.getAllCommentReplies(userId, commentId)
    )
}

case class CommentsRepositoryLive() extends CommentsRepository {
  import QuillContextHelper.ctx._
  import QuillContextQueries.getCommentsWithReplies

  override def insertComment(
      comment: Comments
  ): ZIO[Any, ErrorInfo, CommentReply] = {
    val user = run(
      query[Users].filter(u => u.username == lift(comment.createdBy))
    ).head

    val q = quote {
      query[Comments]
        .insert(lift(comment))
        .returning(c => c)
    }
    val insertedComment = run(q)

    val commentReply = insertedComment
      .into[CommentReply]
      .withFieldConst(_.likeState, 0)
      .withFieldConst(_.createdById, user.userId)
      .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
      .withFieldConst(_.civility, 0.0f)
      .withFieldConst(_.createdByExperience, user.experience)
      .transform

    ZIO.succeed(commentReply)
  }

  override def getComments(
      userId: String,
      subtopicId: UUID
  ): ZIO[Any, ErrorInfo, List[CommentNode]] = {

    val commentsUsersJoin = run(
      query[Comments]
        .filter(r => r.subtopicId == lift(subtopicId))
        .join(query[Users])
        .on(_.createdBy == _.username)
    )

    val rootComments: List[(Comments, Users)] =
      commentsUsersJoin.filter(j => j._1.parentId.isEmpty)

    val commentToUserSrcMap = commentsUsersJoin.foldLeft(Map[UUID, String]()) {
      (m, t) =>
        m + (t._1.id -> t._2.iconSrc.getOrElse(""))
    }

    val likes = run(query[CommentLikes].filter(l => l.userId == lift(userId)))
    val likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
      m + (t.commentId -> t.value)
    }

    val civility = run(
      query[CommentCivility].filter(l => l.userId == lift(userId))
    )
    val civilityMap = civility.foldLeft(Map[UUID, Float]()) { (m, t) =>
      m + (t.commentId -> t.value)
    }

    val commentsWithReplies = rootComments
      .map(joined => {
        val c = joined._1
        val u = joined._2
        val comments = run(getCommentsWithReplies(lift(c.id)))

        val positiveCivility = civilityMap.getOrElse(c.id, None)

        val repliesWithDepth = comments
          .map(c =>
            Comments.commentToCommentReplyWithDepth(
              c,
              likesMap.getOrElse(c.id, 0),
              civilityMap.getOrElse(c.id, 0),
              commentToUserSrcMap(c.id),
              u.userId,
              u.experience
            )
          )
          .reverse
        val replies = comments
          .map(c =>
            Comments.commentToCommentReply(
              c,
              likesMap.getOrElse(c.id, 0),
              civilityMap.getOrElse(c.id, 0),
              commentToUserSrcMap(c.id),
              u.userId,
              u.experience
            )
          )

        val tc = CommentsTreeConstructor
        val replyTree = tc.construct(repliesWithDepth, replies).toList.head
        replyTree
      })
      .sortWith((t1, t2) => t2.data.createdAt.isBefore(t1.data.createdAt))

    ZIO.succeed(commentsWithReplies)
  }

  override def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentReply] = {
    for {
      comment <- ZIO
        .effect(
          run(
            query[Comments].filter(c => c.id == lift(commentId))
          ).head
        )
        .mapError(e => InternalServerError(e.toString))
      user <- ZIO
        .effect(run(query[Users].filter(u => u.userId == lift(userId))).head)
        .mapError(e => InternalServerError(e.toString))
    } yield comment
      .into[CommentReply]
      .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
      .withFieldConst(_.createdById, user.userId)
      .withFieldConst(_.createdByExperience, user.experience)
      .withFieldConst(_.likeState, 0)
      .withFieldConst(_.civility, 0f)
      .transform
  }

  override def getAllCommentReplies(
      userId: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentWithReplies] = {
    println(Console.BLUE + "HEHEHEHEHEHASDFASDVZXCVZXCV")
    println(Console.RESET)
    for {
      commentUser <- ZIO
        .effect(
          run(
            query[Comments]
              .filter(c => c.id == lift(commentId))
              .join(query[Users])
              .on(_.userId == _.userId)
          ).head
        )
        .mapError(e => InternalServerError(e.toString))
      _ = println("HERE Number 1")
      (comment, user) = commentUser
      commentsUsersJoin <- ZIO
        .effect(
          run(
            query[Comments]
              .filter(c => c.parentId == lift(Option(commentId)))
              .join(query[Users])
              .on(_.userId == _.userId)
          )
        )
        .mapError(e => InternalServerError(e.toString))
      _ = println("HERE Number 2")

      commentToUserSrcMap = commentsUsersJoin.foldLeft(Map[UUID, String]()) {
        (m, t) =>
          m + (t._1.id -> t._2.iconSrc.getOrElse(""))
      }
      likes <- ZIO
        .effect(run(query[CommentLikes].filter(l => l.userId == lift(userId))))
        .mapError(e => InternalServerError(e.toString))
      _ = println("HERE Number 3")

      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.commentId -> t.value)
      }
      civility <- ZIO
        .effect(
          run(
            query[CommentCivility].filter(l => l.userId == lift(userId))
          )
        )
        .mapError(e => InternalServerError(e.toString))
      _ = println("HERE Number 4")

      civilityMap = civility.foldLeft(Map[UUID, Float]()) { (m, t) =>
        m + (t.commentId -> t.value)
      }
      commentsWithReplies = commentsUsersJoin
        .map(joined => {
          val c = joined._1
          val u = joined._2
          val comments = run(getCommentsWithReplies(lift(c.id)))

          val repliesWithDepth = comments
            .map(c =>
              Comments.commentToCommentReplyWithDepth(
                c,
                likesMap.getOrElse(c.id, 0),
                civilityMap.getOrElse(c.id, 0),
                commentToUserSrcMap.getOrElse(c.id, ""),
                u.userId,
                u.experience
              )
            )
            .reverse
          val replies = comments
            .map(c =>
              Comments.commentToCommentReply(
                c,
                likesMap.getOrElse(c.id, 0),
                civilityMap.getOrElse(c.id, 0),
                commentToUserSrcMap.getOrElse(c.id, ""),
                u.userId,
                u.experience
              )
            )

          val tc = CommentsTreeConstructor
          val replyTree = tc.construct(repliesWithDepth, replies).toList.head
          replyTree
        })
        .sortWith((t1, t2) => t2.data.createdAt.isBefore(t1.data.createdAt))
    } yield CommentWithReplies(
      replies = commentsWithReplies,
      comment = comment
        .into[CommentReply]
        .withFieldConst(_.createdById, user.userId)
        .withFieldConst(_.createdByExperience, user.experience)
        .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
        .withFieldConst(_.likeState, likesMap.getOrElse(commentId, 0))
        .withFieldConst(_.civility, civilityMap.getOrElse(commentId, 0f))
        .transform
    )

  }
}

object CommentsRepositoryLive {
  val live: ZLayer[Any, Throwable, Has[CommentsRepository]] =
    ZLayer.succeed(CommentsRepositoryLive())
}
