package civil.controllers

import java.util.UUID
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import civil.services.{AuthenticationService, AuthenticationServiceLive}
import civil.apis.TopicsApi._
import civil.config.Config
import civil.models.Topics
import civil.repositories.recommendations.RecommendationsRepositoryLive
import civil.repositories.topics.TopicRepositoryLive
import civil.services.topics.{TopicService, TopicServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._

import scala.util.{Failure, Success}

object TopicsController {

//  val topicRepoLayer = RecommendationsRepositoryLive.live >>> TopicRepositoryLive.live
//  val topicServiceLayer: ZLayer[Any with Unit, Throwable, Has[TopicServiceLive]] = (topicRepoLayer ++ AuthenticationServiceLive.live) >>> TopicServiceLive.live

  val newTopicEndpointRoute: Http[Any, Throwable, Request, Response[Any, Throwable]] = {

    ZioHttpInterpreter().toHttp(newTopicEndpoint){ case (jwt, jwtType, incomingTopic) =>
      TopicService.insertTopic(jwt, jwtType, incomingTopic)
        .map(topic => {
          Right(topic)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer((RecommendationsRepositoryLive.live >>> TopicRepositoryLive.live) >>> TopicServiceLive.live)
    }
  }


  val getTopicsEndpointRoute: Http[Has[TopicService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllTopicsEndpoint){ case () => {
      TopicService.getTopics()
        .map(topics => {
          Right(topics)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer((RecommendationsRepositoryLive.live >>> TopicRepositoryLive.live) >>> TopicServiceLive.live)
    }}
  }

 val getTopicEndpointRoute: Http[Has[TopicService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getTopicEndpoint)(ids => {
      TopicService.getTopic(UUID.fromString(ids._1), ids._2)
        .map(topic => {
          Right(topic)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer((RecommendationsRepositoryLive.live >>> TopicRepositoryLive.live) >>> TopicServiceLive.live)
    }) 
  }

}
