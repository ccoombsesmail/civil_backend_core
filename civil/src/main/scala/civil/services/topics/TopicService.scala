package civil.services.topics

import civil.models.{ErrorInfo, IncomingTopic, OutgoingTopic, Topics}
import civil.directives.MetaData
import civil.directives.OutgoingHttp._
import civil.models.ErrorInfo
import civil.repositories.topics.TopicRepository
import civil.services.{AuthenticationServiceLive, HTMLSanitizerLive}
import io.scalaland.chimney.dsl._
import zio._

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

trait TopicService {
  def insertTopic(
      jwt: String,
      jwtType: String,
      incomingTopic: IncomingTopic
  ): ZIO[Any, ErrorInfo, OutgoingTopic]
  def getTopics: ZIO[Any, ErrorInfo, List[OutgoingTopic]]
  def getTopic(id: UUID, requestingUserID: String): ZIO[Any, ErrorInfo, OutgoingTopic]

}

object TopicService {
  def insertTopic(
      jwt: String,
      jwtType: String,
      incomingTopic: IncomingTopic
  ): ZIO[Has[TopicService], ErrorInfo, OutgoingTopic] =
    ZIO.serviceWith[TopicService](_.insertTopic(jwt, jwtType, incomingTopic))

  def getTopics(): ZIO[Has[TopicService], ErrorInfo, List[OutgoingTopic]] =
    ZIO.serviceWith[TopicService](_.getTopics)

  def getTopic(
      id: UUID,
      requestingUserID: String
  ): ZIO[Has[TopicService], ErrorInfo, OutgoingTopic] =
    ZIO.serviceWith[TopicService](_.getTopic(id, requestingUserID))


}

case class TopicServiceLive(topicRepository: TopicRepository)
    extends TopicService {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  override def insertTopic(
      jwt: String,
      jwtType: String,
      incomingTopic: IncomingTopic
  ): ZIO[Any, ErrorInfo, OutgoingTopic] = {
    val authenticationService = AuthenticationServiceLive()
    val HTMLSanitizer = HTMLSanitizerLive()

    val metaDataOpt = for {
      url <- incomingTopic.tweetUrl
      metaData <- Try(Await.result(getTweetInfo(url), 10 seconds)).toOption
    } yield metaData

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      insertedTopic <- topicRepository.insertTopic(
        incomingTopic
          .into[Topics]
          .withFieldConst(_.description, HTMLSanitizer.sanitize(incomingTopic.description))
          .withFieldConst(_.likes, 0)
          .withFieldConst(_.createdAt, LocalDateTime.now())
          .withFieldConst(_.id, UUID.randomUUID())
          .withFieldConst(_.tweetHtml, metaDataOpt.getOrElse(MetaData(None, "")).html)
          .withFieldConst(_.userId, userData.userId)
          .withFieldConst(_.createdBy, userData.username)
          .transform
      )
    } yield insertedTopic
  }

  override def getTopics: ZIO[Any, ErrorInfo, List[OutgoingTopic]] = {
    topicRepository.getTopics
  }

  override def getTopic(id: UUID, requestingUserID: String): ZIO[Any, ErrorInfo, OutgoingTopic] = {
    topicRepository.getTopic(id, requestingUserID)
  }


}

object TopicServiceLive {
  val live: ZLayer[Has[TopicRepository], Nothing, Has[TopicService]] = {
    for {
      topicRepo <- ZIO.service[TopicRepository]
    } yield TopicServiceLive(topicRepo)
  }.toLayer
}
