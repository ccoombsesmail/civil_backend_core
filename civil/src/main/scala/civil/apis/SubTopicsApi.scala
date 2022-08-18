package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.baseEndpoint
import civil.models.{ErrorInfo, IncomingSubTopic, OutgoingSubTopic, SubTopics}
import sttp.tapir._



object SubTopicsApi {
  val newSubTopicEndpoint: Endpoint[IncomingSubTopic, ErrorInfo, SubTopics, Any] =
    baseEndpoint
      .post
      .in("subtopics")
      .in(jsonBody[IncomingSubTopic])
      .out(jsonBody[SubTopics])

  val getAllSubTopicsEndpoint: Endpoint[String, ErrorInfo, List[OutgoingSubTopic], Any] =
    baseEndpoint
      .get
      .in("subtopics")
      .in(query[String]("topicId"))
      .out(jsonBody[List[OutgoingSubTopic]])
  

  val getSubTopicEndpoint: Endpoint[String, ErrorInfo, OutgoingSubTopic, Any] =
    baseEndpoint
      .get
      .in("subtopics")
      .in(path[String]("subTopicId"))
      .out(jsonBody[OutgoingSubTopic])

}
