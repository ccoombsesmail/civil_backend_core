package civil.repositories
import civil.models.{ErrorInfo, Follows, InternalServerError, NotFound, OutgoingUser, Users}
import civil.models.NotifcationEvents.NewFollower
import civil.models._
import civil.services.KafkaProducerServiceLive
import io.scalaland.chimney.dsl._
import zio._

trait FollowsRepository {
  def insertFollow(follow: Follows): ZIO[Any, ErrorInfo, OutgoingUser]
  def deleteFollow(follow: Follows): ZIO[Any, ErrorInfo, OutgoingUser]
  def getAllFollowers(userId: String): Task[List[OutgoingUser]]
  def getAllFollowed(userId: String): Task[List[OutgoingUser]]
}

object FollowsRepository {
  def insertFollow(
      follow: Follows
  ): ZIO[Has[FollowsRepository], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[FollowsRepository](_.insertFollow(follow))

  def deleteFollow(
      follow: Follows
  ): ZIO[Has[FollowsRepository], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[FollowsRepository](_.deleteFollow(follow))

  def getAllFollowers(
      userId: String
  ): RIO[Has[FollowsRepository], List[OutgoingUser]] =
    ZIO.serviceWith[FollowsRepository](_.getAllFollowers(userId))

  def getAllFollowed(
      userId: String
  ): RIO[Has[FollowsRepository], List[OutgoingUser]] =
    ZIO.serviceWith[FollowsRepository](_.getAllFollowed(userId))
}

case class FollowsRepositoryLive() extends FollowsRepository {

  import QuillContextHelper.ctx._
  val kafka = KafkaProducerServiceLive()

  override def insertFollow(
      follow: Follows
  ): ZIO[Any, ErrorInfo, OutgoingUser] = {
    println("RIGHT HERHERHASDFASDFASDFASDF")
    for {
      _ <- ZIO
        .effect(run(query[Follows].insert(lift(follow))))
        .mapError(e => InternalServerError(e.toString))
      users <- ZIO
        .effect(
          run(
            query[Users].filter(u => u.userId == lift(follow.userId) || u.userId == lift(follow.followedUserId))
          )
        )
        .mapError(e => NotFound(e.toString))
      user <- ZIO.fromOption(users.find(u => u.userId == follow.userId)).orElseFail(NotFound("User Not Found"))
      followedUser <-  ZIO.fromOption(users.find(u => u.userId == follow.followedUserId)).orElseFail(NotFound("User Not Found"))
      _ = println("Start...")
      _ = println(Console.RED + System.nanoTime())
      _ <- ZIO
        .effect(
          kafka.publish(
            NewFollower(
              eventType = "NewFollower",
              follow.userId,
              follow.followedUserId,
              user.iconSrc,
              user.username
            ),
            follow.followedUserId,
            NewFollower.newFollowerSerde
          )
        )
        .fork
      _ = println("Stop....")
      _ = println(System.nanoTime() + Console.RESET)
    } yield followedUser
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(true))
      .transform

  }
  override def deleteFollow(
      follow: Follows
  ): ZIO[Any, ErrorInfo, OutgoingUser] = {
    for {
      _ <- ZIO
        .effect(
          run(
            query[Follows]
              .filter(f =>
                f.userId == lift(follow.userId) && f.followedUserId == lift(
                  follow.followedUserId
                )
              )
              .delete
          )
        )
        .mapError(e => InternalServerError(e.toString))
      users <- ZIO
        .effect(
          run(
            query[Users].filter(u => u.userId == lift(follow.userId) || u.userId == lift(follow.followedUserId))
          )
        )
        .mapError(e => NotFound(e.toString))
      user <- ZIO.fromOption(users.find(u => u.userId == follow.userId)).orElseFail(NotFound("User Not Found"))
      followedUser <-  ZIO.fromOption(users.find(u => u.userId == follow.followedUserId)).orElseFail(NotFound("User Not Found"))
      _ <- ZIO
        .effect(
          kafka.publish(
            NewFollower(
              eventType = "Unfollow",
              follow.userId,
              follow.followedUserId,
              user.iconSrc,
              user.username
            ),
            follow.followedUserId,
            NewFollower.newFollowerSerde
          )
        )
        .fork
    } yield followedUser
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(false))
      .transform

  }
  override def getAllFollowers(userId: String): Task[List[OutgoingUser]] = {
    val users = run(
      query[Follows]
        .leftJoin(query[Users])
        .on((f, u) => f.userId == u.userId)
        .filter(row => row._1.followedUserId == lift(userId))
    )
    ZIO.succeed(users.map { case (f, u) =>
      u.get.into[OutgoingUser].withFieldConst(_.isFollowing, None).transform
    })
  }
  override def getAllFollowed(userId: String): Task[List[OutgoingUser]] = {
    val users = run(
      query[Follows]
        .leftJoin(query[Users])
        .on((f, u) => f.followedUserId == u.userId)
        .filter(row => row._1.userId == lift(userId))
    )
    ZIO.succeed(users.map { case (f, u) =>
      u.get.into[OutgoingUser].withFieldConst(_.isFollowing, None).transform
    })
  }

}

object FollowsRepositoryLive {
  val live: ZLayer[Any, Throwable, Has[FollowsRepository]] =
    ZLayer.succeed(FollowsRepositoryLive())
}
