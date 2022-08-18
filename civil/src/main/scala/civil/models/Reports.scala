package civil.models

import java.util.UUID
import java.time.Instant

case class Report(
    contentId: UUID,
    toxic: Option[Boolean],
    spam: Option[Boolean],
    personalAttack: Option[Boolean]
)

case class Reports(
    userId: String,
    contentId: UUID,
    toxic: Option[Boolean],
    spam: Option[Boolean],
    personalAttack: Option[Boolean],
    contentType: String
)


case class ReportInfo(
    contentId: UUID,
    numToxicReports: Int,
    numPersonalAttackReports: Int,
    numSpamReports: Int,
    voteAgainst: Option[Boolean],
    voteFor: Option[Boolean],
    reportPeriodEnd: Long,
    contentType: String,
    numVotesAgainst: Option[Int] = None,
    numVotesFor: Option[Int] = None
) {

  def attachVotingResults(votingPeriodEnd: Long, votesFor: Int, votesAgainst: Int): ReportInfo = {
    if (votingPeriodEnd < Instant.now().toEpochMilli) this.copy(numVotesAgainst = Some(votesAgainst), numVotesFor = Some(votesFor))
    else this
  }
}