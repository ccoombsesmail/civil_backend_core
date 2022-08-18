package civil

import civil.controllers._
import civil.directives.OutgoingHttp.createClerkUser
import civil.models.ClerkModels.CreateClerkUser
import civil.repositories._
import civil.repositories.comments.{CommentCivilityRepositoryLive, CommentLikesRepositoryLive, CommentsRepositoryLive}
import civil.repositories.recommendations.{OpposingRecommendationsRepositoryLive, RecommendationsRepositoryLive}
import civil.repositories.topics.{SubTopicRepositoryLive, TopicLikesRepositoryLive, TopicRepositoryLive}
import civil.services._
import civil.services.comments._
import civil.services.topics.{TopicLikesService, TopicLikesServiceLive, TopicService, TopicServiceLive}
import zhttp.http._
import zhttp.service._
import zhttp.service.server.ServerChannelFactory
import zio._

import scala.util.{Failure, Success}

object Civil extends zio.App {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  val PORT = 8090
  val routes =
      UsersController.upsertDidUserEndpointRoute <>
      UsersController.getUserEndpointRoute <>
      UsersController.updateUserIconEndpointRoute <>
      UsersController.uploadUserIconEndpointRoute <>
      UsersController.receiveWebHookEndpointRoute <>
      UsersController.updateUserBioInformationEndpointRoute <>
      UsersController.createUserTagEndpointRoute <>
      UsersController.checkIfTagExistsEndpointRoute <>
      TopicsController.newTopicEndpointRoute <>
      TopicsController.getTopicsEndpointRoute <>
      TopicsController.getTopicEndpointRoute <>
      TopicLikesController.updateTopicLikesEndpointRoute <>
      SubTopicsController.newSubTopicEndpointRoute <>
      SubTopicsController.getAllSubTopicsEndpointRoute <>
      SubTopicsController.getSubTopicEndpointRoute <>
      CommentsController.newCommentEndpointRoute <>
      CommentsController.getAllCommentsEndpointRoute <>
      CommentsController.getCommentEndpointRoute <>
      CommentsController.getAllCommentRepliesEndpointRoute <>
      CommentCivilityController.updateCivilityEndpointRoute <>
      CommentCivilityController.updateTribunalCommentCivilityEndpointRoute <>
      CommentLikesController.updateCommentLikesEndpointRoute <>
      CommentLikesController.updateTribunalCommentLikesEndpointRoute <>
      EnumsController.getAllEnumsEndpointRoute <>
      FollowsController.newFollowEndpointRoute <>
      FollowsController.deleteFollowEndpointRoute <>
      FollowsController.getAllFollowersEndpointRoute <>
      FollowsController.getAllFollowedEndpointRoute <>
      OpposingRecommendationsController.getAllOpposingRecommendationEndpointRoute <>
      OpposingRecommendationsController.newOpposingRecommendationEndpointRoute <>
      RecommendationsController.getAllRecommendationEndpointRoute <>
      ReportsController.newReportEndpointRoute <>
      ReportsController.getReportEndpointRoute <>
      TribunalVotesController.newTribunalVoteEndpointRoute <>
      TribunalCommentsController.newTopicTribunalVoteEndpointRoute <>
      TribunalCommentsController.getTribunalCommentsEndpointRoute <>
      TribunalCommentsController.getTribunalCommentsBatchEndpointRoute




  val app: HttpApp[ZEnv with Has[TopicService] with Has[TribunalCommentsService] with Has[
    SubTopicService
  ] with Has[UsersService] with Has[CommentCivilityService] with Has[
    TopicLikesService] with Has[CommentLikesService]
   with Has[CommentsService] with Has[FollowsService] with Has[OpposingRecommendationsService
  ] with Has[RecommendationsService] with Has[ReportsService] with Has[TribunalVotesService], Throwable] =
    CORS(routes, config = CORSConfig(anyOrigin = true))

  val topicLayer =
    (RecommendationsRepositoryLive.live >>> TopicRepositoryLive.live) >>> TopicServiceLive.live
  val subTopicLayer = SubTopicRepositoryLive.live >>> SubTopicServiceLive.live
  val userLayer = UsersRepositoryLive.live >>> UsersServiceLive.live
  val commentsLayer =
    (CommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> CommentsServiceLive.live
  val followsLayer = FollowsRepositoryLive.live >>> FollowsServiceLive.live
  val opposingRecsLayer =
    OpposingRecommendationsRepositoryLive.live >>> OpposingRecommendationsServiceLive.live
  val recsLayer =
    RecommendationsRepositoryLive.live >>> RecommendationsServiceLive.live
  val commentCivilityLayer =
    CommentCivilityRepositoryLive.live >>> CommentCivilityServiceLive.live
  val commentLikesLayer =
    CommentLikesRepositoryLive.live >>> CommentLikesServiceLive.live
  val topicLikesLayer =
    TopicLikesRepositoryLive.live >>> TopicLikesServiceLive.live
  val reportsLayer = {
    ReportsRepositoryLive.live >>> ReportsServiceLive.live
  }
  val tribunalVotesLayer = {
    TribunalVotesRepositoryLive.live >>> TribunalVotesServiceLive.live
  }
  val tribunalCommentsLayer = {
    (TribunalCommentsRepositoryLive.live ++  UsersRepositoryLive.live) >>> TribunalCommentsServiceLive.live
  }
  val fullLayer =
    topicLayer ++ subTopicLayer ++ userLayer ++ commentsLayer ++ followsLayer ++ opposingRecsLayer ++ commentLikesLayer ++
      recsLayer ++ commentCivilityLayer ++ topicLikesLayer ++ reportsLayer ++ tribunalVotesLayer ++ tribunalCommentsLayer

  // val appServer =
  //   Server.port(PORT) ++
  //     Server.maxRequestSize(4 * 1024) ++
  //     Server.app(app) ++
  //     Server.simpleLeakDetection

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val ONE_MB = 1000000
    val appServer =
      Server.port(PORT) ++
        Server.app(app) ++
        Server.simpleLeakDetection ++
        Server.maxRequestSize(ONE_MB)

    appServer.make.useForever
      .provideCustomLayer(
        fullLayer ++ EventLoopGroup.auto(5) ++ ServerChannelFactory.auto
      )
      .exitCode
    // Server.start(8092, app)
    //   .provideCustomLayer(fullLayer)
    //   .exitCode

  }
}
