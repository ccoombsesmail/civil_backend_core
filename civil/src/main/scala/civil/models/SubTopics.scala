package civil.models

import java.time.LocalDateTime;
import java.util.UUID

case class SubTopics(
    id: UUID,
    title: String,
    createdBy: String,
    summary: Option[String],
    description: String,
    tweetHtml: Option[String],
    ytUrl: Option[String],
    createdAt: LocalDateTime,
    evidenceLinks: Option[List[String]],
    likes: Int,
    imageUrl: Option[String],
    vodUrl: Option[String],
    contentUrl: Option[String],
    userId: String,
    thumbImgUrl: Option[String],
    subTopicKeyWords: Seq[String] = Seq(),
    topicId: UUID
)

case class IncomingSubTopic(
    title: String,
    createdBy: String,
    summary: Option[String],
    description: String,
    tweetUrl: Option[String],
    ytUrl: Option[String],
    contentUrl: Option[String],
    evidenceLinks: Option[List[String]],
    imageUrl: Option[String],
    vodUrl: Option[String],
    userId: String,
    thumbImgUrl: Option[String],
    topicId: String
)

case class OutgoingSubTopic(
    id: UUID,
    title: String,
    createdBy: String,
    topicId: UUID,
    createdAt: LocalDateTime,
    userId: String,
    summary: Option[String],
    description: String,
    tweetHtml: Option[String],
    ytUrl: Option[String],
    evidenceLinks: Option[List[String]],
    createdByIconSrc: String,
    imageUrl: Option[String],
    contentUrl: Option[String],
    vodUrl: Option[String],
    thumbImgUrl: Option[String],
    likes: Int,
    liked: Boolean,
    allComments: Long,
    positiveComments: Long,
    neutralComments: Long,
    negativeComments: Long,
    totalCommentsAndReplies: Long
)

// case class Topic(id: java.util.UUID, name: String, Seq[SubTopic])
