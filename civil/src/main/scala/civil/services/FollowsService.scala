package civil.services

import civil.models.{BadRequest, ErrorInfo, FollowedUserId, Follows, OutgoingUser}
import civil.models._
import civil.repositories.FollowsRepository
import io.scalaland.chimney.dsl.TransformerOps
import zio._

trait FollowsService {
  def insertFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Any, ErrorInfo, OutgoingUser]
  def deleteFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Any, ErrorInfo, OutgoingUser]
  def getAllFolowers(userId: String): Task[List[OutgoingUser]]
  def getAllFollowed(userId: String): Task[List[OutgoingUser]]
}

object FollowsService {
  def insertFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Has[FollowsService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[FollowsService](
      _.insertFollow(jwt, jwtType, followedUserId)
    )

  def deleteFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Has[FollowsService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[FollowsService](
      _.deleteFollow(jwt, jwtType, followedUserId)
    )

  def getAllFolowers(
      userId: String
  ): RIO[Has[FollowsService], List[OutgoingUser]] =
    ZIO.serviceWith[FollowsService](_.getAllFolowers(userId))

  def getAllFollowed(
      userId: String
  ): RIO[Has[FollowsService], List[OutgoingUser]] =
    ZIO.serviceWith[FollowsService](_.getAllFollowed(userId))
}

case class FollowsServiceLive(followsRepository: FollowsRepository)
    extends FollowsService {
  val authenticationService = AuthenticationServiceLive()

  override def insertFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Any, ErrorInfo, OutgoingUser] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- ZIO
        .fail(BadRequest("User can't follow self"))
        .when(userData.userId == followedUserId.followedUserId)
      _ = println(userData)
      outgoingUser <- followsRepository.insertFollow(
        Follows(userId = userData.userId, followedUserId = followedUserId.followedUserId)
      )
    } yield outgoingUser
  }
  override def deleteFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Any, ErrorInfo, OutgoingUser] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- ZIO
        .fail(BadRequest("User can't unfollow self"))
        .when(userData.userId == followedUserId.followedUserId)
      outgoingUser <- followsRepository.deleteFollow(
        Follows(userId = userData.userId, followedUserId = followedUserId.followedUserId)
      )
    } yield outgoingUser
  }
  override def getAllFolowers(userId: String): Task[List[OutgoingUser]] = {
    followsRepository.getAllFollowers(userId)
  }
  override def getAllFollowed(userId: String): Task[List[OutgoingUser]] = {
    followsRepository.getAllFollowed(userId)
  }

}

object FollowsServiceLive {
  val live: ZLayer[Has[FollowsRepository], Throwable, Has[FollowsService]] = {
    for {
      followsRepo <- ZIO.service[FollowsRepository]
    } yield FollowsServiceLive(followsRepo)
  }.toLayer
}
