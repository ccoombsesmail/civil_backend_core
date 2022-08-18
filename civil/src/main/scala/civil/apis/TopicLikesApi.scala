package civil.apis

import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.baseEndpoint
import civil.models.{ErrorInfo, TopicLiked, UpdateTopicLikes}
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.jsonBody

object TopicLikesApi {
  val updateTopicLikesEndpoint: Endpoint[UpdateTopicLikes, ErrorInfo, TopicLiked, Any] =
    baseEndpoint.put
      .in("topics")
      .in(jsonBody[UpdateTopicLikes])
      .out(jsonBody[TopicLiked])
}
