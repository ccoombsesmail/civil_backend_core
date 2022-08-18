package civil.models

import java.util.UUID

case class Recommendations(
    targetContentId: UUID,
    recommendedContentId: UUID,
    similarityScore: Double
)

case class OutgoingRecommendations(
    subTopic: Option[SubTopics],
    topic: Option[Topics],
    similarityScore: Double
)

case class Recs(
    targetContentId: String,
    recommendedContentId: String,
    similarityScore: Double
)

case class IncomingRecommendations(
    recs: Array[Recs]
)
