package civil.services.comments

import civil.models.{Civility, CivilityGiven, ErrorInfo, InternalServerError}
import civil.models._
import civil.repositories.comments.CommentCivilityRepository
import civil.services.AuthenticationServiceLive
import zio._
// import civil.directives.SentimentAnalyzer

trait CommentCivilityService {
  def addOrRemoveCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven]
  def addOrRemoveTribunalCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven]

}

object CommentCivilityService {
  def addOrRemoveCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Has[CommentCivilityService], ErrorInfo, CivilityGiven] =
    ZIO.serviceWith[CommentCivilityService](
      _.addOrRemoveCommentCivility(
        jwt,
        jwtType,
        civilityData
      )
    )
  def addOrRemoveTribunalCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Has[CommentCivilityService], ErrorInfo, CivilityGiven] =
    ZIO.serviceWith[CommentCivilityService](
      _.addOrRemoveTribunalCommentCivility(
        jwt,
        jwtType,
        civilityData
      )
    )
}

case class CommentCivilityServiceLive(
    commentCivilityRepo: CommentCivilityRepository
) extends CommentCivilityService {
  val authenticationService = AuthenticationServiceLive()

  override def addOrRemoveCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      res <- commentCivilityRepo
        .addOrRemoveCommentCivility(
          givingUserId = userData.userId,
          givingUserUsername = userData.username,
          civilityData: Civility
        )
        .mapError(e => InternalServerError(e.toString))
    } yield res

  }

  override def addOrRemoveTribunalCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      res <- commentCivilityRepo
        .addOrRemoveTribunalCommentCivility(
          givingUserId = userData.userId,
          givingUserUsername = userData.username,
          civilityData: Civility
        )
        .mapError(e => InternalServerError(e.toString))
    } yield res
  }

}

object CommentCivilityServiceLive {
  val live: ZLayer[Has[CommentCivilityRepository], Throwable, Has[
    CommentCivilityService
  ]] = {
    for {
      commentCivilityRepo <- ZIO.service[CommentCivilityRepository]
    } yield CommentCivilityServiceLive(commentCivilityRepo)
  }.toLayer
}
