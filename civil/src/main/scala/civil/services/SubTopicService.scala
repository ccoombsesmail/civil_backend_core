package civil.services

import civil.models.{ErrorInfo, IncomingSubTopic, OutgoingSubTopic, SubTopics}
import civil.directives.MetaData
import civil.directives.OutgoingHttp.getTweetInfo

import java.util.UUID
import civil.models.ErrorInfo
import civil.repositories.topics.SubTopicRepository

import scala.concurrent.duration._
import io.scalaland.chimney.dsl._

import java.time.LocalDateTime
import zio._

import scala.concurrent.Await
import scala.util.Try

trait SubTopicService {
  def insertSubTopic(incomingSubTopic: IncomingSubTopic): ZIO[Any, ErrorInfo, SubTopics]
  def getSubTopics(topicId: UUID):  ZIO[Any, ErrorInfo, List[OutgoingSubTopic]]
  def getSubTopic(id: UUID):  ZIO[Any, ErrorInfo, OutgoingSubTopic]
}


object SubTopicService {
  def insertSubTopic(incomingSubTopic: IncomingSubTopic): ZIO[Has[SubTopicService], ErrorInfo, SubTopics]
  = ZIO.serviceWith[SubTopicService](_.insertSubTopic(incomingSubTopic))

  def getSubTopics(topicId: UUID): ZIO[Has[SubTopicService], ErrorInfo, List[OutgoingSubTopic]]
  = ZIO.serviceWith[SubTopicService](_.getSubTopics(topicId))

  def getSubTopic(id: UUID): ZIO[Has[SubTopicService], ErrorInfo, OutgoingSubTopic]
  = ZIO.serviceWith[SubTopicService](_.getSubTopic(id))
}


case class SubTopicServiceLive(subTopicRepository: SubTopicRepository) extends SubTopicService {
  override def insertSubTopic(incomingSubTopic: IncomingSubTopic): ZIO[Any, ErrorInfo, SubTopics] = {
    val uuid = UUID.randomUUID()
    subTopicRepository.insertSubTopic(
      incomingSubTopic
        .into[SubTopics]
        .withFieldConst(_.likes, 0)
        .withFieldConst(_.createdAt, LocalDateTime.now())
        .withFieldConst(_.id, uuid)
        .withFieldConst(_.tweetHtml, None)
        .withFieldConst(_.topicId, UUID.fromString(incomingSubTopic.topicId))
        .transform
    )
  }

  override def getSubTopics(topicId: UUID): ZIO[Any, ErrorInfo, List[OutgoingSubTopic]] = {
    subTopicRepository.getSubTopics(topicId)
  }

  override def getSubTopic(id: UUID): ZIO[Any, ErrorInfo, OutgoingSubTopic] = {
    subTopicRepository.getSubTopic(id)
  }

}

object SubTopicServiceLive {
  val live: ZLayer[Has[SubTopicRepository], Throwable, Has[SubTopicService]] = {
    for {
      subTopicRepo <- ZIO.service[SubTopicRepository]
    } yield SubTopicServiceLive(subTopicRepo)
  }.toLayer
}
