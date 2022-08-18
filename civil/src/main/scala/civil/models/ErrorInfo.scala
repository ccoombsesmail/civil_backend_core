package civil.models

sealed trait ErrorInfo extends Product with Serializable

case class NotFound(what: String) extends ErrorInfo
case class Unauthorized(realm: String) extends ErrorInfo
case class Unknown(code: Int, msg: String) extends ErrorInfo
case class InternalServerError(msg: String) extends ErrorInfo
case class BadRequest(msg: String) extends ErrorInfo
case object NoContent extends ErrorInfo
