package civil.repositories.topics

import civil.models.{Comments, ErrorInfo, InternalServerError, OutgoingSubTopic, SubTopics, Users}
import civil.models._
import civil.repositories.QuillContextHelper
import zio._
import io.scalaland.chimney.dsl._

import java.util.UUID

trait SubTopicRepository {
  def insertSubTopic(subTopic: SubTopics): ZIO[Any, ErrorInfo, SubTopics]
  def getSubTopics(topicId: UUID): ZIO[Any, ErrorInfo, List[OutgoingSubTopic]]
  def getSubTopic(id: UUID): ZIO[Any, ErrorInfo, OutgoingSubTopic]
}


object SubTopicRepository {
  def insertSubTopic(subTopic: SubTopics): ZIO[Has[SubTopicRepository], ErrorInfo, SubTopics]
  = ZIO.serviceWith[SubTopicRepository](_.insertSubTopic(subTopic))

  def getSubTopics(topicId: UUID): ZIO[Has[SubTopicRepository], ErrorInfo, List[OutgoingSubTopic]]
  = ZIO.serviceWith[SubTopicRepository](_.getSubTopics(topicId))

  def getSubTopic(id: UUID): ZIO[Has[SubTopicRepository], ErrorInfo, OutgoingSubTopic]
  = ZIO.serviceWith[SubTopicRepository](_.getSubTopic(id))
}


case class SubTopicRepositoryLive() extends SubTopicRepository {
  import QuillContextHelper.ctx._

  override def insertSubTopic(subTopic: SubTopics): ZIO[Any, ErrorInfo, SubTopics] = {

    for {
      subtopic <- ZIO.effect(
        run(
          query[SubTopics].insert(lift(subTopic))
            .returning(r => r)
        )
      ).mapError(e => InternalServerError(e.toString))
    } yield subtopic
  }

  override def getSubTopics(topicId: UUID): ZIO[Any, ErrorInfo, List[OutgoingSubTopic]] = {

    for {
      subtopicsUsersJoin <- ZIO.effect(
        run(query[SubTopics].join(query[Users]).on(_.userId == _.userId).filter(t => t._1.topicId == lift(topicId)))
      ).mapError(e => InternalServerError(e.toString))
      subTopics <- ZIO.effect( subtopicsUsersJoin.map { case (st, u) =>
        val createdByIconSrc = u.iconSrc
        val commentNumbers = run(
          query[Comments].filter(c => c.subtopicId == lift(st.id) && c.parentId.isEmpty).groupBy(c => c.sentiment).map {
            case (sentiment, comments) =>
              (sentiment, comments.size)
          }
        ).toMap

        val totalCommentsAndReplies = run(query[Comments].filter(c => c.subtopicId == lift(st.id)).size)
        val positiveComments = commentNumbers.getOrElse("POSITIVE", 0L)
        val neutralComments = commentNumbers.getOrElse("NEUTRAL", 0L)
        val negativeComments = commentNumbers.getOrElse("NEGATIVE", 0L)
        st.into[OutgoingSubTopic]
          .withFieldConst(_.liked, false)
          .withFieldConst(_.createdByIconSrc, createdByIconSrc.getOrElse(""))
          .withFieldConst(_.positiveComments, positiveComments)
          .withFieldConst(_.neutralComments, neutralComments)
          .withFieldConst(_.negativeComments, negativeComments)
          .withFieldConst(_.allComments, negativeComments + neutralComments + positiveComments)
          .withFieldConst(_.totalCommentsAndReplies, totalCommentsAndReplies)
          .transform
      }).mapError(e => InternalServerError(e.toString))
    } yield subTopics
  }

   override def getSubTopic(id: UUID): ZIO[Any, ErrorInfo, OutgoingSubTopic] = {

     for {
       subtopicsUsersJoin <- ZIO.effect(
         run(query[SubTopics].join(query[Users]).on(_.userId == _.userId).filter(t => t._1.id == lift(id)))
       ).mapError(e => InternalServerError(e.toString))
       subtopicUser <- ZIO.fromOption(subtopicsUsersJoin.headOption).orElseFail(InternalServerError("Can't Find SubTopic"))
       iconSrc = subtopicUser._2
       subtopic = subtopicUser._1
       commentNumbers <- ZIO.effect(
         run(
           query[Comments].filter(c => c.subtopicId == lift(id) && c.parentId.isEmpty).groupBy(c => c.sentiment).map {
             case (sentiment, comments) =>
               (sentiment, comments.size)
           }
         ).toMap
       ).mapError(e => InternalServerError(e.toString))
       numCommentsAndReplies <- ZIO.effect(run(query[Comments].filter(c => c.subtopicId == lift(id))).size).mapError(e => InternalServerError(e.toString))
       positiveComments = commentNumbers.getOrElse("POSITIVE", 0L)
       neutralComments = commentNumbers.getOrElse("NEUTRAL", 0L)
       negativeComments = commentNumbers.getOrElse("NEGATIVE", 0L)
     } yield subtopic.into[OutgoingSubTopic]
       .withFieldConst(_.liked, false)
       .withFieldConst(_.createdByIconSrc, iconSrc.iconSrc.getOrElse(""))
       .withFieldConst(_.positiveComments, positiveComments)
       .withFieldConst(_.neutralComments, neutralComments)
       .withFieldConst(_.negativeComments, negativeComments)
       .withFieldConst(_.allComments, negativeComments + neutralComments + positiveComments)
       .withFieldConst(_.totalCommentsAndReplies, numCommentsAndReplies.toLong)
       .transform
  }
}


object SubTopicRepositoryLive {
  val live: ZLayer[Any, Nothing, Has[SubTopicRepository]] =
    ZLayer.succeed(SubTopicRepositoryLive())
}