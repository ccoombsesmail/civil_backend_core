package civil.models.NotifcationEvents

import zio.ZIO
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.serde.Serde

import java.util.UUID

case class CommentCivilityGiven(
    eventType: String,
    value: Float,
    commentId: UUID,
    givingUserId: String,
    receivingUserId: String,
    username: String,
    iconSrc: Option[String]
)

object CommentCivilityGiven {
  implicit val decoder: JsonDecoder[CommentCivilityGiven] =
    DeriveJsonDecoder.gen[CommentCivilityGiven]
  implicit val encoder: JsonEncoder[CommentCivilityGiven] =
    DeriveJsonEncoder.gen[CommentCivilityGiven]

  val commentCivilityGivenSerde: Serde[Any, CommentCivilityGiven] = Serde.string.inmapM {
    newFollowerAsString =>
      ZIO.fromEither(
        newFollowerAsString
          .fromJson[CommentCivilityGiven]
          .left
          .map(new RuntimeException(_))
      )
  } { newFollowerAsObj =>
    ZIO.effect(newFollowerAsObj.toJson)
  }
}
