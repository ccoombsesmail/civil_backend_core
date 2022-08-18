package civil.models

import civil.models.enums.ReportStatus.Clean
import civil.models.enums.{ReportStatus, TopicCategories}

import java.time.LocalDateTime
import java.util.UUID

case class Topics(
    id: UUID,
    title: String,
    createdBy: String,
    summary: String,
    description: String,
    tweetHtml: Option[String],
    ytUrl: Option[String],
    createdAt: LocalDateTime,
    evidenceLinks: Option[List[String]],
    likes: Int,
    category: TopicCategories,
    imageUrl: Option[String],
    vodUrl: Option[String],
    contentUrl: Option[String],
    userId: String,
    thumbImgUrl: Option[String],
    topicWords: Seq[String] = Seq(),
    reportStatus: String = Clean.entryName
)

case class IncomingTopic(
    title: String,
    summary: String,
    description: String,
    tweetUrl: Option[String],
    ytUrl: Option[String],
    contentUrl: Option[String],
    evidenceLinks: Option[List[String]],
    category: TopicCategories,
    imageUrl: Option[String],
    vodUrl: Option[String],
    thumbImgUrl: Option[String]
)

case class OutgoingTopic(
    id: UUID,
    userId: String,
    title: String,
    createdBy: String,
    createByTag: Option[String],
    summary: String,
    description: String,
    tweetHtml: Option[String],
    ytUrl: Option[String],
    createdAt: LocalDateTime,
    evidenceLinks: Option[List[String]],
    createdByIconSrc: String,
    likes: Int,
    likeState: Int,
    category: TopicCategories,
    imageUrl: Option[String],
    contentUrl: Option[String],
    vodUrl: Option[String],
    thumbImgUrl: Option[String],
    reportStatus: String = ReportStatus.Clean.entryName,
    topicCreatorIsDidUser: Boolean
)

case class Words(topicWords: Seq[String])
