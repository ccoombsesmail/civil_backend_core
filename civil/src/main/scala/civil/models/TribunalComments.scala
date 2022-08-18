package civil.models

import civil.models.enums.TribunalCommentType
import io.scalaland.chimney.dsl.TransformerOps

import java.time.LocalDateTime
import java.util.UUID

case class TribunalCommentNode(
    data: TribunalCommentsReply,
    children: Seq[TribunalCommentNode]
)
case class TribunalEntryWithDepth(comment: TribunalCommentsReply, depth: Int)

object TribunalComments {
  def withDepthToReplyWithDepth(
      commentWithDepth: TribunalCommentWithDepth,
      likeState: Int,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String]
  ) =
    TribunalEntryWithDepth(
      commentWithDepth
        .into[TribunalCommentsReply]
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
      comment: TribunalCommentWithDepth,
      likeState: Int,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String]
  ) =
    comment
      .into[TribunalCommentsReply]
      .withFieldConst(_.likeState, likeState)
      .withFieldConst(_.civility, civility)
      .withFieldConst(_.createdByIconSrc, iconSrc)
      .withFieldConst(_.createdById, userId)
      .withFieldConst(_.createdByExperience, createdByExperience)
      .withFieldConst(_.source, comment.source)
      .transform
}

case class TribunalComments(
    id: UUID,
    content: String,
    userId: String,
    createdBy: String,
    sentiment: String,
    reportedContentId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    source: Option[String],
    commentType: TribunalCommentType = TribunalCommentType.General
)

case class TribunalCommentsReply(
    id: UUID,
    content: String,
    createdBy: String,
    sentiment: String,
    reportedContentId: UUID,
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
    commentType: TribunalCommentType
)

case class TribunalCommentWithDepth(
    id: UUID,
    content: String,
    createdBy: String,
    sentiment: String,
    reportedContentId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    depth: Int,
    source: Option[String],
    commentType: TribunalCommentType
)

case class TribunalCommentsBatchResponse(
    Reporter: List[TribunalCommentNode],
    Defendant: List[TribunalCommentNode],
    Jury: List[TribunalCommentNode],
    General: List[TribunalCommentNode]
)
