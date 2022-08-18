package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.{baseEndpoint, baseEndpointAuthenticated}
import civil.models.{ErrorInfo, IncomingTopic, OutgoingTopic}
import sttp.tapir._
import zio._


object TopicsApi {
  
  val newTopicEndpoint: Endpoint[(String, String, IncomingTopic), ErrorInfo, OutgoingTopic, Any] =
    baseEndpointAuthenticated.post
      .in("topics")
      .in(jsonBody[IncomingTopic])
      .out(jsonBody[OutgoingTopic])


  val getAllTopicsEndpoint: Endpoint[Unit, ErrorInfo, List[OutgoingTopic], Any] =
    baseEndpoint.get
      .in("topics")
      .out(jsonBody[List[OutgoingTopic]])

  val getTopicEndpoint: Endpoint[(String, String), ErrorInfo, OutgoingTopic, Any] =
    baseEndpoint.get
      .in("topics")
      .in(path[String]("topicId"))
      .in(path[String]("userId"))
      .out(jsonBody[OutgoingTopic])


}
