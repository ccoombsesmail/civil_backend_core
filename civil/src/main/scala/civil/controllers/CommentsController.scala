package civil.controllers

import civil.apis.CommentsApi._
import civil.repositories.UsersRepositoryLive
import civil.repositories.comments.CommentsRepositoryLive
import civil.services.comments.{CommentsService, CommentsServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._

import java.util.UUID

object CommentsController {
  lazy val newCommentEndpointRoute: Http[Has[
    CommentsService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(newCommentEndpoint) {
      case (jwt, jwtType, incomingComment) =>
        CommentsService
          .insertComment(jwt, jwtType, incomingComment)
          .map(comment => {
            Right(comment)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(
            (CommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> CommentsServiceLive.live
          )
    }
  }

  lazy val getAllCommentsEndpointRoute: Http[Has[
    CommentsService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllCommentsEndpoint) {
      case (jwt, jwtType, subtopicId) =>
        CommentsService
          .getComments(jwt, jwtType, UUID.fromString(subtopicId))
          .map(comment => {
            Right(comment)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(
            (CommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> CommentsServiceLive.live
          )
    }
  }

  lazy val getAllCommentRepliesEndpointRoute: Http[Has[
    CommentsService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllCommentRepliesEndpoint) {
      case (jwt, jwtType, commentId) => {
        CommentsService
          .getAllCommentReplies(jwt, jwtType, UUID.fromString(commentId))
          .map(commentWithReplies => {
            Right(commentWithReplies)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(
            (CommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> CommentsServiceLive.live
          )
      }
    }
  }

  val getCommentEndpointRoute: Http[Has[
    CommentsService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getCommentEndpoint) {
      case (jwt, jwtType, commentId) =>
        CommentsService
          .getComment(jwt, jwtType, UUID.fromString(commentId))
          .map(comment => {
            Right(comment)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(
            (CommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> CommentsServiceLive.live
          )
    }
  }

}
