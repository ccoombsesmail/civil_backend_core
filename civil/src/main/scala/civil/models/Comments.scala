package civil.models

import civil.models.enums.ReportStatus.Clean

import java.time.LocalDateTime
import java.util.UUID
import io.scalaland.chimney.dsl._



case class CommentNode(data: CommentReply, children: Seq[CommentNode])
case class EntryWithDepth(comment: CommentReply, depth: Int)

object Comments {
  def commentToCommentReplyWithDepth(
      commentWithDepth: CommentWithDepth,
      likeState: Int,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String],
  ) =
    EntryWithDepth(
      commentWithDepth.into[CommentReply]
        .withFieldConst(_.likeState, likeState)
        .withFieldConst(_.civility, civility)
        .withFieldConst(_.createdByIconSrc, iconSrc)
        .withFieldConst(_.createdById, userId)
        .withFieldConst(_.createdByExperience, createdByExperience)
        .withFieldConst(_.source, commentWithDepth.source)
        .transform,
      commentWithDepth.depth
    )

  def commentToCommentReply(
      comment: CommentWithDepth,
      likeState: Int,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String],
  ) =
     comment.into[CommentReply]
       .withFieldConst(_.likeState, 0)
       .withFieldConst(_.civility, civility)
       .withFieldConst(_.createdByIconSrc, iconSrc)
       .withFieldConst(_.createdById, userId)
       .withFieldConst(_.createdByExperience, createdByExperience)
       .withFieldConst(_.source, comment.source)
       .transform
}

case class Comments(
    id: UUID,
    content: String,
    userId: String,
    createdBy: String,
    sentiment: String,
    subtopicId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    source: Option[String],
    reportStatus: String = Clean.entryName
)

case class IncomingComment(
    content: String,
    createdBy: String,
    contentId: UUID,
    parentId: Option[UUID],
    rootId: Option[UUID],
    rawText: String,
    source: Option[String]
)

case class OutgoingComment(
    id: UUID,
    content: String,
    createdBy: String,
    sentiment: String,
    subtopicId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    likeState: Int,
    civility: Float,
    source: Option[String],
    createdById: UUID,
    createdByIconSrc: String,
    rootId: Option[UUID],
    reportStatus: String
)

case class CommentReply(
    id: UUID,
    content: String,
    createdBy: String,
    sentiment: String,
    subtopicId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    likeState: Int,
    civility: Float,
    source: Option[String],
    createdByIconSrc: String,
    createdById: String,
    createdByExperience: Option[String],
    reportStatus: String = Clean.entryName
)

case class CommentWithDepth(
    id: UUID,
    content: String,
    createdBy: String,
    sentiment: String,
    subtopicId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    depth: Int,
    source: Option[String],
    reportStatus: String = Clean.entryName
)


case class CommentWithReplies(
    replies: List[CommentNode],
    comment: CommentReply
 )

case class UpdateLikes(id: UUID, userId: String, increment: Boolean)

case class Liked(
    commentId: UUID,
    likes: Int,
    liked: Boolean,
    rootId: Option[UUID]
)

case class CivilityGiven(
    civility: Float,
    commentId: UUID,
    rootId: Option[UUID]
)
