package civil.repositories

import civil.models.{ErrorInfo, InternalServerError, NotFound, ReportInfo, ReportTiming, Reports, Topics}
import civil.models.enums.ReportStatus
import civil.models._
import civil.models.NotifcationEvents._
import zio.duration.{Duration, durationInt}
import zio.{Fiber, Has, Schedule, ZIO, ZLayer, console}

import java.io.IOException
import java.time.Instant
import java.util.UUID
import io.getquill.Query
import civil.services.KafkaProducerServiceLive

trait ReportsRepository {
  def addReport(report: Reports): ZIO[Any, ErrorInfo, Unit]
  def getReport(
      contentId: UUID,
      userId: String
  ): ZIO[Any, ErrorInfo, ReportInfo]

}

object ReportsRepository {
  def addReport(
     report: Reports
  ): ZIO[Has[ReportsRepository], ErrorInfo, Unit] =
    ZIO.serviceWith[ReportsRepository](
      _.addReport(report)
    )

  def getReport(
      contentId: UUID,
      userId: String
  ): ZIO[Has[ReportsRepository], ErrorInfo, ReportInfo] =
    ZIO.serviceWith[ReportsRepository](
      _.getReport(contentId, userId)
    )
}

case class ReportsRepositoryLive() extends ReportsRepository {
  val runtime = zio.Runtime.default
  import QuillContextHelper.ctx._
  val kafka = KafkaProducerServiceLive()

  val VOTE_THRESHOLD = 2
  val VOTE_TIME = 600000
  // 24 hours 86400000
  override def addReport(
     report: Reports
  ): ZIO[Any, ErrorInfo, Unit] = {
    val res = for {
      topicOpt <- ZIO.effect(run(
        query[Topics].filter(t => t.id == lift(report.contentId))
      ).headOption).mapError(e => InternalServerError(e.toString))
      contentType = if (topicOpt.isDefined) "TOPIC" else "COMMENT"
      reportWithContentType = report.copy(contentType = contentType)
      _ <- ZIO
        .effect(
          run(
            query[Reports].insert(lift(reportWithContentType))
          )
        )
        .mapError(e => {
          println(e)
          InternalServerError(s"Error Inserting Report")
        })
      allReports <- ZIO
        .effect(
          run(
            query[Reports].filter(r =>
              r.contentId == lift(report.contentId)
            )
          )
        )
        .mapError(e => {
          println(e)
          InternalServerError(s"Error Getting All Reports")
        })
      _ = if (allReports.length >= 1) setupReviewProcess(report.contentId, contentType)
    } yield ()
    res

  }

  def setupReviewProcess(contentId: UUID, contentType: String) = {
    if (contentType == "TOPIC") {
      transaction {
        run(
          query[Topics]
            .filter(t => t.id == lift(contentId))
            .update(t => t.reportStatus -> "UNDER_REVIEW")
        )
        run(
          query[ReportTiming].insert(
            lift(ReportTiming(contentId, Instant.now().toEpochMilli + VOTE_TIME))
          )
        )
      }
    } else {
      transaction {
        run(
          query[Comments]
            .filter(c => c.id == lift(contentId))
            .update(c => c.reportStatus -> "UNDER_REVIEW")
        )
        run(
          query[ReportTiming].insert(
            lift(ReportTiming(contentId, Instant.now().toEpochMilli + VOTE_TIME))
          )
        )
      }
    }


    // val users =  run(
    //   infix""" 
    //   SELECT * FROM users TABLESAMPLE SYSTEM_ROWS(10) 
    //   """.as[Query[Users]]
    // )

    // println(Console.BLUE)
    // println(s"Users:")
    // println(users)

    // val selectedjuryMembers = users.map(u => TribunalJury(userId = u.userId, contentId = contentId, contentType = contentType))

    // val insertedJuryMembers = run(
    //     liftQuery(selectedjuryMembers).foreach(e => query[TribunalJury].insert(e).returning(tj => tj))
    // )


    for {
      users <- ZIO.effect(run(
          infix""" 
          SELECT * FROM users TABLESAMPLE SYSTEM_ROWS(10) 
          """.as[Query[Users]]
        )).mapError(e => {
        println(e)
        new Throwable(e.toString)
      })

      selectedjuryMembers = users.map(u => TribunalJury(userId = u.userId, contentId = contentId, contentType = contentType))
      // insertedJuryMembers <- ZIO.effect(run(
      //   liftQuery(selectedjuryMembers).foreach(e => query[TribunalJury].insert(e).returning(tj => tj))
      //   ))
      //   .mapError(e => {
      //     println(e)
      //     new Throwable(e.toString)
      //   })


        //  effectList = insertedJuryMembers.map(jm => {
      //   ZIO.effect(
      //   kafka.publish(
      //     NotifyTribunalJuryMember(
      //       eventType = "NotifyTribunalJuryMember",
      //       contentId = jm.contentId,
      //       userId = jm.userId,
      //       contentType = jm.contentType,
      //     ),
      //     jm.userId,
      //     NotifyTribunalJuryMember.notifyTribunalJuryMemberSerde
      //   )
      // )
      //  })
      _ = println(selectedjuryMembers)
      jm = selectedjuryMembers.head
      _ = println(Console.MAGENTA + jm + Console.RESET)
      _ <- ZIO.effect(
        kafka.publish(
          NotifyTribunalJuryMember(
            eventType = "NotifyTribunalJuryMember",
            contentId = jm.contentId,
            userId = jm.userId,
            contentType = jm.contentType,
          ),
          jm.userId,
          NotifyTribunalJuryMember.notifyTribunalJuryMemberSerde
        )
      ).fork
    //  _ <- ZIO.foreach(insertedJuryMembers)(jm => {
    //     ZIO.effect(
    //     kafka.publish(
    //       NotifyTribunalJuryMember(
    //         eventType = "NotifyTribunalJuryMember",
    //         contentId = jm.contentId,
    //         userId = jm.userId,
    //         contentType = jm.contentType,
    //       ),
    //       jm.userId,
    //       NotifyTribunalJuryMember.notifyTribunalJuryMemberSerde
    //     )
    //   ).mapError(e => {
    //       println(e)
    //       new Throwable(e.toString)
    //     }).fork
    //    })
    } yield (users)
   

      
      

    startVotingTimer(contentId, contentType)
  }

  def determineVoteResult(contentId: UUID, contentType: String) = {

    for {
      votes <- ZIO.effect(
        run(
          query[TribunalVotes].filter(tv => tv.contentId == lift(contentId))
        )
      )
      numFor = votes.count(tv => tv.voteFor.contains(true))
      numAgainst = votes.count(ttv => ttv.voteAgainst.contains(true))
      _ = println("Num for and num against")
      _ = println(numFor, numAgainst)
      reportStatus =
        if (numFor >= numAgainst) ReportStatus.Clean.entryName else ReportStatus.Removed.entryName

      _ <- ZIO.effect(
        transaction {
          run(
            query[ReportTiming].filter(rt => rt.contentId == lift(contentId)).delete
          )
          if (contentType == "TOPIC") {
            run(
              query[Topics]
                .filter(t => t.id == lift(contentId))
                .update(_.reportStatus -> lift(reportStatus))
            )
          } else {
            run(
              query[Comments]
                .filter(c => c.id == lift(contentId))
                .update(_.reportStatus -> lift(reportStatus))
            )
          }

        }
      )
    } yield true

  }

  def startVotingTimer(contentId: UUID, contentType: String): Fiber.Runtime[IOException, Duration] = {
    println(Console.BLUE + "STARTED TIMER!!!")
    println(Console.RESET + "")

    val endVoting = for {
      numVotes <- ZIO.effect(
        run(
          query[TribunalVotes].filter(tv => tv.contentId == lift(contentId))
        ).length
      )
      r <- numVotes match {
        case x if x < VOTE_THRESHOLD => {
          println(Console.BLUE + "RE-STARTED TIMER!!!")
          println(Console.RESET + "")
          ZIO.effect(
            run(
              query[ReportTiming]
                .filter(rt => rt.contentId == lift(contentId))
                .update(
                  _.reportPeriodEnd -> lift(Instant.now().toEpochMilli + VOTE_TIME)
                )
                .returning(_ => false)
            )
          )
        }
        case _ => determineVoteResult(contentId, contentType)
      }
      _ = if (!r) startVotingTimer(contentId, contentType)
    } yield r

    runtime.unsafeRun(for {
      f <- endVoting
        .catchAll(e => console.putStrLn(s"job failed with $e"))
        .schedule(Schedule.duration(60.minute))
        .forkDaemon
    } yield f)
  }



  override def getReport(
      contentId: UUID,
      userId: String
  ): ZIO[Any, ErrorInfo, ReportInfo] = {
    for {
      votes <- ZIO.effect(
        run(
          query[TribunalVotes].filter(tv => tv.contentId == lift(contentId))
        )
      ).mapError(e => InternalServerError(e.toString))

      numVotesFor = votes.count(tv => tv.voteFor.contains(true))
      numVotesAgainst = votes.count(tv => tv.voteAgainst.contains(true))

      voteOpt = votes.find(v => v.userId == userId)
      vote = voteOpt.getOrElse(
        TribunalVotes(
          userId = userId,
          contentId = contentId,
          voteAgainst = None,
          voteFor = None
        )
      )
      timing <- ZIO
        .fromOption(
          run(
            query[ReportTiming].filter(rt => rt.contentId == lift(contentId))
          ).headOption
        )
        .orElseFail(
          NotFound("Trouble Finding Report Data")
        )
      reports <- ZIO
        .effect(
          run(
            query[Reports].filter(tr => tr.contentId == lift(contentId))
          )
        )
        .mapError(e => InternalServerError(e.toString))
      reportsMap = reports.foldLeft(Map[String, Int]()) { (m, t) =>
        val m1 = if (t.toxic.isDefined) {
          if (m.contains("toxic"))
            m + ("toxic" -> (m.getOrElse("toxic", 0) + 1))
          else m + ("toxic" -> 1)
        } else m

        val m2 = if (t.personalAttack.isDefined) {
          if (m1.contains("personalAttack"))
            m1 + ("personalAttack" -> (m1.getOrElse("personalAttack", 0) + 1))
          else m1 + ("personalAttack" -> 1)
        } else m1

        val m3 = if (t.spam.isDefined) {
          if (m2.contains("spam"))
            m2 + ("spam" -> (m2.getOrElse("spam", 0) + 1))
          else m2 + ("spam" -> 1)
        } else m2
        m3
      }
    } yield ReportInfo(
      contentId,
      reportsMap.getOrElse("toxic", 0),
      reportsMap.getOrElse("personalAttack", 0),
      reportsMap.getOrElse("spam", 0),
      vote.voteAgainst,
      vote.voteFor,
      timing.reportPeriodEnd,
      contentType = "TOPIC"
    ).attachVotingResults(timing.reportPeriodEnd, numVotesAgainst, numVotesFor)
  }
}

object ReportsRepositoryLive {
  val live: ZLayer[Any, Nothing, Has[
    ReportsRepository
  ]] = ZLayer.succeed(ReportsRepositoryLive())
}
