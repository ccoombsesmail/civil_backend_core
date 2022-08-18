package civil.repositories.topics

import civil.models.{ErrorInfo, InternalServerError, OutgoingTopic, Recommendations, SubTopics, TopicLikes, TopicVods, Topics, Users}
import civil.directives.OutgoingHttp
import civil.models._
import civil.repositories.QuillContextHelper
import civil.repositories.recommendations.RecommendationsRepository
import io.scalaland.chimney.dsl._
import zio.{ZIO, _}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TopicRepoHelpers(
    recommendationsRepository: RecommendationsRepository
) {

  import QuillContextHelper.ctx._

  def getDefaultSubTopic(topic: Topics) = SubTopics(
    UUID.randomUUID(),
    "General",
    topic.createdBy,
    Some("Sub Topic For General Discussion"),
    "",
    None,
    None,
    topic.createdAt,
    None,
    0,
    None,
    None,
    None,
    topic.userId,
    None,
    Seq(),
    topic.id
  )

  def runTopicMlPipeline(url: String, insertedTopic: Topics): Future[Unit] = for {
    words <- OutgoingHttp.getTopicWordsFromMLService("get-topic-words", url)
    targetTopicKeyWords = run(
      query[Topics]
        .filter(t => t.id == lift(insertedTopic.id))
        .update(
          _.topicWords -> lift(words.topicWords)
        )
        .returning(t => (t.id, t.topicWords))
    )
    recommendedTopics <- getSimilarTopics(targetTopicKeyWords, url)

    recommendations = recommendedTopics.recs
      .map(r => {
        r.into[Recommendations]
          .withFieldConst(
            _.targetContentId,
            UUID.fromString(r.targetContentId)
          )
          .withFieldConst(
            _.recommendedContentId,
            UUID.fromString(r.recommendedContentId)
          )
          .transform
      })
      .toList
    _ = recommendationsRepository.batchInsertRecommendation(recommendations)
  } yield ()

  def getSimilarTopics(
      targetTopicData: (UUID, Seq[String]),
      contentUrl: String
  ) = {
    val targetTopicKeyWords = targetTopicData._2.toSet
    val allTopicData = run(
      query[Topics]
        .filter(t => t.id != lift(targetTopicData._1))
        .map(t => (t.id, t.topicWords, t.contentUrl))
    )
    val potentialRecommendationIds = allTopicData
      .filter({ case (id, words, url) =>
        val wordsAsSet = words.toSet
        val numWordsIntersecting =
          targetTopicKeyWords.intersect(wordsAsSet).size
        numWordsIntersecting > 2
      })
      .map(d => (d._1.toString, d._3.get))
      .toMap
    OutgoingHttp.getSimilarityScoresBatch(
      "tfidf-batch",
      contentUrl,
      targetTopicData._1,
      potentialRecommendationIds
    )
  }
}

trait TopicRepository {
  def insertTopic(topic: Topics): ZIO[Any, ErrorInfo, OutgoingTopic]
  def getTopics: ZIO[Any, ErrorInfo, List[OutgoingTopic]]
  def getTopic(id: UUID, requestingUserID: String): ZIO[Any, ErrorInfo, OutgoingTopic]
}

object TopicRepository {
  def insertTopic(topic: Topics): ZIO[Has[TopicRepository], ErrorInfo, OutgoingTopic] =
    ZIO.serviceWith[TopicRepository](_.insertTopic(topic))

  def getTopics: ZIO[Has[TopicRepository], ErrorInfo, List[OutgoingTopic]] =
    ZIO.serviceWith[TopicRepository](_.getTopics)

  def getTopic(
      id: UUID,
      requestingUserID: String
  ): ZIO[Has[TopicRepository], ErrorInfo, OutgoingTopic] =
    ZIO.serviceWith[TopicRepository](_.getTopic(id, requestingUserID))
}

case class TopicRepositoryLive(
    recommendationsRepository: RecommendationsRepository
) extends TopicRepository {
  import QuillContextHelper.ctx._

  val helpers = TopicRepoHelpers(recommendationsRepository)
  import helpers._

  override def insertTopic(
      topic: Topics
  ): ZIO[Any, ErrorInfo, OutgoingTopic] = {
    for {
      user <- ZIO
        .fromOption(
          run(
            query[Users].filter(u => u.userId == lift(topic.userId))
          ).headOption
        )
        .mapError(_ => InternalServerError("There Was A Problem Identifying The User"))
      t <- ZIO
        .effect(transaction {
          val insertedTopic = run(
            query[Topics].insert(lift(topic)).returning(inserted => inserted)
          )
          run(
            query[SubTopics].insert(lift(getDefaultSubTopic(insertedTopic)))
          )
          insertedTopic
        }).mapError(e => InternalServerError(e.toString))
      _ = t.vodUrl.map(url => run(query[TopicVods].insert(lift(TopicVods(t.userId, url, t.id)))))
      _ = t.contentUrl.map(url => runTopicMlPipeline(url, t))

      outgoingTopic <- ZIO.effect(
        t.into[OutgoingTopic]
          .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
          .withFieldConst(_.likeState, 0)
          .withFieldConst(_.createByTag, user.tag)
          .withFieldConst(_.topicCreatorIsDidUser, user.isDidUser)
          .transform
      ).mapError(_ => InternalServerError("There Was A Problem Identifying Saving The Topic"))
      _ = println(outgoingTopic)
    } yield outgoingTopic

  }

  override def getTopics: ZIO[Any, ErrorInfo, List[OutgoingTopic]] = {

    val joined = quote {
      query[Topics]
        .join(query[Users])
        .on(_.userId == _.userId)
        .leftJoin(query[TopicVods])
        .on(_._1.id == _.topicId)
    }

    for {
      likes <- ZIO.effect(run(query[TopicLikes])).mapError(e => InternalServerError(e.toString))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) => m + (t.topicId -> t.value) }
      topicsUsersVodsJoin <- ZIO.effect(run(joined).map { case (tu, v) => (tu._1, tu._2, v) }).mapError(e => InternalServerError(e.toString))
      outgoingTopics <- ZIO.foreach(topicsUsersVodsJoin)(row => {
        val topic = row._1
        val user = row._2
        val vod = row._3
        ZIO.effect(topic
          .into[OutgoingTopic]
          .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
          .withFieldConst(_.likeState, likesMap.getOrElse(topic.id, 0))
          .withFieldConst(_.vodUrl, vod.map(v => v.vodUrl))
          .withFieldConst(_.topicCreatorIsDidUser, user.isDidUser)
          .withFieldConst(_.createByTag, user.tag)
          .transform).mapError(_ => InternalServerError("ERROR"))
      })
    } yield outgoingTopics.sortWith((t1, t2) => t2.createdAt.isBefore(t1.createdAt))

  }

  override def getTopic(id: UUID, requestingUserID: String): ZIO[Any, ErrorInfo, OutgoingTopic] = {


    val joined = quote {
      query[Topics]
        .filter(_.id == lift(id))
        .join(query[Users])
        .on(_.userId == _.userId)
        .leftJoin(query[TopicVods])
        .on(_._1.id == _.topicId)
    }

    for {
      likes <- ZIO.effect(run(query[TopicLikes].filter(l =>
        l.userId == lift(requestingUserID) && l.topicId == lift(id)
      ))).mapError(e => InternalServerError(e.toString))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) => m + (t.topicId -> t.value) }
      topicsUsersVodsJoin <- ZIO.effect(run(joined).map { case (tu, v) => (tu._1, tu._2, v) }).mapError(e => InternalServerError(e.toString))
      _ <- ZIO.fail(
        InternalServerError("Can't find topic details")
      ).unless(topicsUsersVodsJoin.nonEmpty)
    } yield topicsUsersVodsJoin.map(joined => {
        val t = joined._1
        val u = joined._2
        t.into[OutgoingTopic]
          .withFieldConst(_.createdByIconSrc, u.iconSrc.get)
          .withFieldConst(_.likeState, likesMap.getOrElse(t.id, 0))
          .withFieldConst(_.vodUrl, None)
          .withFieldConst(_.topicCreatorIsDidUser, u.isDidUser)
          .withFieldConst(_.createByTag, u.tag)
          .transform
      }).head

  }

}

object TopicRepositoryLive {
  val live: ZLayer[Has[RecommendationsRepository], Nothing, Has[
    TopicRepository
  ]] = {
    for {
      topicRepo <- ZIO.service[RecommendationsRepository]
    } yield TopicRepositoryLive(topicRepo)
  }.toLayer
}
