package civil.services.comments

import civil.models.{CommentLiked, CommentLikes, ErrorInfo, UpdateCommentLikes}
import civil.models.ErrorInfo
import civil.repositories.comments.CommentLikesRepository
import civil.services.AuthenticationServiceLive
import io.scalaland.chimney.dsl.TransformerOps
import zio.{Has, ZIO, ZLayer}

trait CommentLikesService {
  def addRemoveCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Any, ErrorInfo, CommentLiked]

  def addRemoveTribunalCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Any, ErrorInfo, CommentLiked]
}

object CommentLikesService {
  def addRemoveCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Has[CommentLikesService], ErrorInfo, CommentLiked] =
    ZIO.serviceWith[CommentLikesService](
      _.addRemoveCommentLikeOrDislike(jwt, jwtType, commentLikeDislikeData)
    )

  def addRemoveTribunalCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Has[CommentLikesService], ErrorInfo, CommentLiked] =
    ZIO.serviceWith[CommentLikesService](
      _.addRemoveTribunalCommentLikeOrDislike(
        jwt,
        jwtType,
        commentLikeDislikeData
      )
    )
}

case class CommentLikesServiceLive(commentLikesRepo: CommentLikesRepository)
    extends CommentLikesService {
  val authenticationService = AuthenticationServiceLive()

  override def addRemoveCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Any, ErrorInfo, CommentLiked] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      likeData <- commentLikesRepo.addRemoveCommentLikeOrDislike(
        commentLikeDislikeData
          .into[CommentLikes]
          .withFieldConst(_.userId, userData.userId)
          .transform
      )
    } yield likeData
  }

  override def addRemoveTribunalCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Any, ErrorInfo, CommentLiked] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      likeData <- commentLikesRepo.addRemoveTribunalCommentLikeOrDislike(
        commentLikeDislikeData
          .into[CommentLikes]
          .withFieldConst(_.userId, userData.userId)
          .transform
      )
    } yield likeData

  }

}

object CommentLikesServiceLive {
  val live: ZLayer[Has[CommentLikesRepository], Nothing, Has[
    CommentLikesService
  ]] = {
    for {
      commentLikesRepo <- ZIO.service[CommentLikesRepository]
    } yield CommentLikesServiceLive(commentLikesRepo)
  }.toLayer
}
