package civil.repositories.comments

import civil.models.{Civility, CivilityGiven, CommentCivility, Comments, ErrorInfo, InternalServerError, TribunalComments, Unknown, Users}
import civil.models.NotifcationEvents.CommentCivilityGiven
import civil.models._
import civil.repositories.{QuillContextHelper, QuillContextQueries}
import civil.repositories.QuillContextQueries.getCommentsWithReplies
import civil.services.KafkaProducerServiceLive
import zio._

import java.util.UUID

trait CommentCivilityRepository {
  def addOrRemoveCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven]
  def addOrRemoveTribunalCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven]

}

object CommentCivilityRepository {
  def addOrRemoveCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: Civility
  ): ZIO[Has[CommentCivilityRepository], ErrorInfo, CivilityGiven] =
    ZIO.serviceWith[CommentCivilityRepository](
      _.addOrRemoveCommentCivility(
        givingUserId,
        givingUserUsername,
        civilityData
      )
    )
  def addOrRemoveTribunalCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: Civility
  ): ZIO[Has[CommentCivilityRepository], ErrorInfo, CivilityGiven] =
    ZIO.serviceWith[CommentCivilityRepository](
      _.addOrRemoveCommentCivility(
        givingUserId,
        givingUserUsername,
        civilityData
      )
    )

}

case class CommentCivilityRepositoryLive() extends CommentCivilityRepository {
  val kafka = KafkaProducerServiceLive()
  import QuillContextHelper.ctx._

  def addCivility(
      rootId: Option[UUID],
      givingUserId: String,
      givingUserUsername: String,
      civilityData: Civility
  ) =
    for {
      user <- ZIO
        .effect(
          transaction {
            run(
              query[CommentCivility]
                .insert(
                  lift(
                    CommentCivility(
                      givingUserId,
                      civilityData.commentId,
                      civilityData.value
                    )
                  )
                )
                .onConflictUpdate(_.commentId, _.userId)((t, e) =>
                  t.value -> e.value
                )
                .returning(c =>
                  CivilityGiven(
                    c.value,
                    lift(civilityData.commentId),
                    lift(rootId)
                  )
                )
            )
            run(
              updateUserCivilityQuery(
                civilityData.receivingUserId,
                civilityData.value
              )
            )
          }
        )
        .mapError(e => InternalServerError(e.toString))
      _ <- ZIO
        .effect(
          kafka.publish(
            CommentCivilityGiven(
              eventType = "CommentCivilityGiven",
              value = civilityData.value,
              commentId = civilityData.commentId,
              givingUserId = givingUserId,
              receivingUserId = civilityData.receivingUserId,
              iconSrc = user.iconSrc,
              username = givingUserUsername
            ),
            civilityData.receivingUserId,
            CommentCivilityGiven.commentCivilityGivenSerde
          )
        )
        .fork
    } yield CivilityGiven(
      civility = civilityData.value,
      commentId = civilityData.commentId,
      rootId = rootId
    )

  override def addOrRemoveCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven] = {

    for {
      comment <- ZIO
        .fromOption(
          run(
            query[Comments].filter(c => c.id == lift(civilityData.commentId))
          ).headOption
        )
        .orElseFail(Unknown(400, "Can't Find Comment"))
      civilityGiven <- addCivility(comment.rootId, givingUserId, givingUserUsername, civilityData)
    } yield civilityGiven

  }

  def updateUserCivilityQuery(receivingUserId: String, civility: Float) =
    quote {
      query[Users]
        .filter(u => u.userId == lift(receivingUserId))
        .update(user => user.civility -> (user.civility + lift(civility)))
        .returning(r => r)
    }

  override def addOrRemoveTribunalCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven] = {
    for {
      comment <- ZIO
        .fromOption(
          run(
            query[TribunalComments].filter(c =>
              c.id == lift(civilityData.commentId)
            )
          ).headOption
        )
        .orElseFail(Unknown(400, "Can't Find Comment"))
      civilityGiven <- addCivility(comment.rootId, givingUserId, givingUserUsername, civilityData)

    } yield civilityGiven
  }

}

object CommentCivilityRepositoryLive {
  val live: ZLayer[Any, Throwable, Has[CommentCivilityRepository]] =
    ZLayer.succeed(CommentCivilityRepositoryLive())
}

//object CommentCivilityRepository {
//  val ctx = QuillContext.ctx
//  import ctx._
//
//  def insertCivility(userId: String, commentId: UUID, civility: Int) = {
//    val q = quote {
//      query[CommentCivility].insert(lift(CommentCivility(userId, commentId, civility)))
//      .onConflictUpdate(_.commentId, _.userId)((t, e) => t.civility -> e.civility)
//    }
//    ctx.run(q)
//  }
//
//   def deleteCivility(userId: String, commentId: java.util.UUID) = {
//    ctx.run(query[CommentCivility].filter(l => l.commentId == lift(commentId) && l.userId == lift(userId)).delete)
//  }
//
//
//}
