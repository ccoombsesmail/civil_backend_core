package civil.services.comments

import civil.models.enums.Sentiment
import civil.models._
import civil.repositories.comments.CommentsRepository
import civil.services.{AuthenticationServiceLive, HTMLSanitizerLive}
import zio._
// import civil.models.enums.{Sentiment}
import civil.repositories.UsersRepository
import io.scalaland.chimney.dsl._

import java.time.LocalDateTime
import java.util.UUID
// import civil.directives.SentimentAnalyzer

trait CommentsService {
  def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Any, ErrorInfo, CommentReply]
  def getComments(
      jwt: String,
      jwtType: String,
      subtopicId: UUID
  ): ZIO[Any, ErrorInfo, List[CommentNode]]
  def getComment(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentReply]
  def getAllCommentReplies(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentWithReplies]

}

object CommentsService {
  def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Has[CommentsService], ErrorInfo, CommentReply] =
    ZIO.serviceWith[CommentsService](
      _.insertComment(jwt, jwtType, incomingComment)
    )
  def getComments(
      jwt: String,
      jwtType: String,
      subtopicId: UUID
  ): ZIO[Has[CommentsService], ErrorInfo, List[CommentNode]] =
    ZIO.serviceWith[CommentsService](_.getComments(jwt, jwtType, subtopicId))
  def getComment(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Has[CommentsService], ErrorInfo, CommentReply] =
    ZIO.serviceWith[CommentsService](_.getComment(jwt, jwtType, commentId))
  def getAllCommentReplies(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Has[CommentsService], ErrorInfo, CommentWithReplies] =
    ZIO.serviceWith[CommentsService](
      _.getAllCommentReplies(jwt, jwtType, commentId)
    )

}

case class CommentsServiceLive(
    commentsRepo: CommentsRepository,
    usersRepo: UsersRepository
) extends CommentsService {
  val authenticationService = AuthenticationServiceLive()

  override def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Any, ErrorInfo, CommentReply] = {
    // val sentiment = SentimentAnalyzer.mainSentiment(incommingComment.rawText)
    val HTMLSanitizer = HTMLSanitizerLive()

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      insertedComment <- commentsRepo.insertComment(
        incomingComment
          .into[Comments]
          .withFieldConst(
            _.content,
            HTMLSanitizer.sanitize(incomingComment.content)
          )
          .withFieldConst(_.id, UUID.randomUUID())
          .withFieldConst(_.createdAt, LocalDateTime.now())
          .withFieldConst(_.likes, 0)
          .withFieldConst(_.sentiment, Sentiment.POSITIVE.toString)
          .withFieldConst(_.subtopicId, incomingComment.contentId)
          .withFieldConst(_.userId, userData.userId)
          .transform
      )
    } yield insertedComment

  }

  override def getComments(
      jwt: String,
      jwtType: String,
      subtopicId: UUID
  ): ZIO[Any, ErrorInfo, List[CommentNode]] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- commentsRepo.getComments(userData.userId, subtopicId)
    } yield comments
  }

  override def getComment(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentReply] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- commentsRepo.getComment(userData.userId, commentId)
    } yield comments
  }

  override def getAllCommentReplies(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentWithReplies] = {
    println(Console.RED + "HEHEHEHEHEHASDFASDVZXCVZXCV")
    println(Console.RESET)
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      commentWithReplies <- commentsRepo.getAllCommentReplies(
        userData.userId,
        commentId
      )
    } yield commentWithReplies

  }

}

object CommentsServiceLive {
  val live: ZLayer[Has[CommentsRepository] with Has[
    UsersRepository
  ], Throwable, Has[CommentsService]] = {
    for {
      commentsRepo <- ZIO.service[CommentsRepository]
      usersRepo <- ZIO.service[UsersRepository]
    } yield CommentsServiceLive(commentsRepo, usersRepo)
  }.toLayer
}
