package civil.services.topics

import civil.models.{ErrorInfo, TopicLiked, UpdateTopicLikes}
import civil.models.ErrorInfo
import civil.repositories.topics.TopicLikesRepository
import zio.{Has, ZIO, ZLayer}

trait TopicLikesService {
  def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes
  ): ZIO[Any, ErrorInfo, TopicLiked]
}

object TopicLikesService {
  def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes
  ): ZIO[Has[TopicLikesService], ErrorInfo, TopicLiked] =
    ZIO.serviceWith[TopicLikesService](
      _.addRemoveTopicLikeOrDislike(topicLikeDislikeData)
    )

}

case class TopicLikesServiceLive(topicLikesRep: TopicLikesRepository)
    extends TopicLikesService {

  override def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes
  ): ZIO[Any, ErrorInfo, TopicLiked] = {
    topicLikesRep.addRemoveTopicLikeOrDislike(topicLikeDislikeData)
  }

}

object TopicLikesServiceLive {
  val live: ZLayer[Has[TopicLikesRepository], Nothing, Has[
    TopicLikesService
  ]] = {
    for {
      topicLikesRepo <- ZIO.service[TopicLikesRepository]
    } yield TopicLikesServiceLive(topicLikesRepo)
  }.toLayer
}
