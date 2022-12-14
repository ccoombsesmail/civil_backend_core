package civil.repositories.recommendations

import civil.models.{ErrorInfo, OpposingRecommendations, OutGoingOpposingRecommendations, SubTopics, Topics, UrlsForTFIDFConversion}
import civil.directives.OutgoingHttp
import civil.models._
import civil.repositories.QuillContextHelper
import io.getquill.Ord
import zio._
import io.scalaland.chimney.dsl._

import java.util.UUID
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

trait OpposingRecommendationsRepository {
  def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Any, ErrorInfo, Unit]
  def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Any, ErrorInfo, List[OutGoingOpposingRecommendations]]
}

object OpposingRecommendationsRepository {
  def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Has[OpposingRecommendationsRepository], ErrorInfo, Unit] =
    ZIO.serviceWith[OpposingRecommendationsRepository](_.insertOpposingRecommendation(opposingRec))
  def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Has[OpposingRecommendationsRepository], ErrorInfo, List[OutGoingOpposingRecommendations]] =
    ZIO.serviceWith[OpposingRecommendationsRepository](_.getAllOpposingRecommendations(targetContentId))
}


case class OpposingRecommendationsRepositoryLive() extends OpposingRecommendationsRepository {
  import QuillContextHelper.ctx._
  import QuillContextHelper.ctx.extras._

  override def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Any, ErrorInfo, Unit] = {
    val recommendedContentIdIsSubTopic = opposingRec.recommendedContentId.map((recId) => {
      val isSubTopic = run(query[SubTopics].filter(st => st.id == lift(recId))).nonEmpty
      isSubTopic
    })
    opposingRec.recommendedContentId.foreach(recId => {
        val topic = run(query[Topics].filter(t => t.id == lift(opposingRec.targetContentId))).head
        val recTopic = run(query[Topics].filter(t => t.id == lift(recId))).head
        val fut = for {
          recContentUrl <- recTopic.contentUrl
          contentUrl <-  topic.contentUrl
          f = OutgoingHttp.sendHTTPToMLService("tfidf", UrlsForTFIDFConversion(contentUrl, recContentUrl))
        } yield f
        fut.foreach(f => {
          f onComplete {
            case Success(score) => {
              val recToBeInserted = opposingRec.copy(isSubTopic = recommendedContentIdIsSubTopic.getOrElse(false), similarityScore = score.score)
              run(query[OpposingRecommendations].insert(lift(recToBeInserted)))
            }
            case Failure(t) => println("An error has occurred: " + t.getMessage)
          }
        })
    })

    opposingRec.externalRecommendedContent.foreach((url) => {
      val topic = run(query[Topics].filter(t => t.id == lift(opposingRec.targetContentId))).head
      val fut = for {
        contentUrl <-  topic.contentUrl
        f = OutgoingHttp.sendHTTPToMLService("tfidf", UrlsForTFIDFConversion(contentUrl, url))
      } yield f
      fut.foreach(f => {
        f onComplete {
          case Success(score) => {
            val recToBeInserted = opposingRec.copy(isSubTopic = recommendedContentIdIsSubTopic.getOrElse(false), similarityScore = score.score)
            run(query[OpposingRecommendations].insert(lift(recToBeInserted)))
          }
          case Failure(t) => println("An error has occurred: " + t.getMessage)
        }
      })
    })



    ZIO.unit
  }

  override def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Any, ErrorInfo, List[OutGoingOpposingRecommendations]] = {


    val q = quote {
      for {
        rec <- query[OpposingRecommendations].filter(rec => rec.targetContentId == lift(targetContentId)).sortBy(r => r.similarityScore)(Ord.descNullsLast)
        t <- query[Topics].leftJoin(t => t.id === rec.recommendedContentId)
        st <- query[SubTopics].leftJoin(st => st.id === rec.recommendedContentId)
      } yield (rec, t, st)
    }

    val outgoingRecs = run(q).map({ case (rec, topic, subtopic) =>
      rec.into[OutGoingOpposingRecommendations]
      .withFieldConst(_.topic, topic)
      .withFieldConst(_.subTopic, subtopic)
      .withFieldConst(_.id, UUID.randomUUID())
      .transform
    })

    ZIO.succeed(outgoingRecs)

  }

}

object OpposingRecommendationsRepositoryLive {
  val live: ZLayer[Any, Throwable, Has[OpposingRecommendationsRepository]] =
    ZLayer.succeed(OpposingRecommendationsRepositoryLive())
}