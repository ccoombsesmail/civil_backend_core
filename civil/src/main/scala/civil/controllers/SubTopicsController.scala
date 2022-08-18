package civil.controllers

import java.util.UUID
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import civil.services.{SubTopicService, SubTopicServiceLive}
import civil.apis.SubTopicsApi._
import civil.models.SubTopics
import civil.repositories.topics.SubTopicRepositoryLive
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._

object SubTopicsController {
 
val newSubTopicEndpointRoute: Http[Has[SubTopicService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(newSubTopicEndpoint)(incomingSubTopic => {
      SubTopicService.insertSubTopic(incomingSubTopic)
        .map(subTopic => {
          Right(subTopic)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(SubTopicRepositoryLive.live >>> SubTopicServiceLive.live)
    }) 
  }


  val getAllSubTopicsEndpointRoute: Http[Has[SubTopicService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllSubTopicsEndpoint)(topicId => { 
      SubTopicService.getSubTopics(UUID.fromString((topicId)))
        .map(subTopics => {
          Right(subTopics)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(SubTopicRepositoryLive.live >>> SubTopicServiceLive.live)
    }) 
  }

 val getSubTopicEndpointRoute: Http[Has[SubTopicService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getSubTopicEndpoint)(id => { 
      SubTopicService.getSubTopic(UUID.fromString(id))
        .map(subTopic => {
          Right(subTopic)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(SubTopicRepositoryLive.live >>> SubTopicServiceLive.live)
    }) 
  }

}
