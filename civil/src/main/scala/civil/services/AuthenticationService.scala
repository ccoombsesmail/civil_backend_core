package civil.services

import civil.config.Config
import civil.models.{JwtUserClaimsData, Unauthorized}
import org.elastos.did.{DID, DIDBackend, DefaultDIDAdapter}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import org.json4s.jackson.JsonMethods

import scala.util.{Failure, Success}
import org.json4s.{DefaultFormats, Formats}
import zio._

import reflect.ClassTag

trait AuthenticationService {
  def decodeClerkJWT(jwt: String): Option[JwtUserClaimsData]
  def decodeDIDJWT(jwt: String, did: String): Option[JwtUserClaimsData]
}

//object AuthenticationService {
//  def decodeClerkJWT(jwt: String): JwtUserClaimsData =
//    ZIO.serviceWith[AuthenticationService](_.decodeClerkJWT(jwt))
//
//  def decodeDIDJWT(jwt: String, did: String): RIO[Has[AuthenticationService], JwtUserClaimsData] =
//    ZIO.serviceWith[AuthenticationService](_.decodeDIDJWT(jwt, did))
//}

case class AuthenticationServiceLive() extends AuthenticationService {
  implicit val formats: Formats = DefaultFormats

  val clerk_jwt_key = Config().getString("civil.clerk_jwt_key")

  def convert[T: ClassTag](
      method: String => java.lang.Object,
      key: String
  ): Option[T] = {
    val ct = implicitly[ClassTag[T]]
    method(key) match {
      case ct(x) => Some(x)
      case _     => None
    }
  }

  def extractUserData(jwt: String, jwtType: String): ZIO[Any, Unauthorized, JwtUserClaimsData] = for {
    jwtClaimsData <- ZIO.effect(
      jwtType match {
        case s"DID ${didString}" => decodeDIDJWT(jwt, didString)
        case "CLERK" => decodeClerkJWT(jwt)
        case _ => None
      }
    ).mapError(e => Unauthorized(e.toString))
    userData <- ZIO.fromOption(jwtClaimsData).orElseFail(Unauthorized("Unrecognized JWT Type"))
  } yield userData


  override def decodeClerkJWT(jwt: String): Option[JwtUserClaimsData] = {
    val decodedJwt = jwt match {
      case s"Bearer $encodedJwt" => {
        JwtCirce.decode(
          encodedJwt,
          clerk_jwt_key,
          Seq(JwtAlgorithm.RS256)
        ) match {
          case Success(value)     => value
          case Failure(exception) => { 
            println(exception)
          JwtClaim()
          }
        }
      }
      case _ => JwtClaim()
    }
    val claimsContent = JsonMethods.parse(decodedJwt.content).extract[JwtUserClaimsData]
    Some(claimsContent)
  }

  override def decodeDIDJWT(
      jwt: String,
      didString: String
  ): Option[JwtUserClaimsData] = {
    DIDBackend.initialize(new DefaultDIDAdapter("testnet"))
    val did = new DID(didString)
    println(did)
    val signer = did.resolve()

    if (!signer.isValid()) println("BAD SIGNER")

    val parser = signer.jwtParserBuilder.build()
    val decodedJwt = jwt match {
      case s"Bearer $encodedJwt" => parser.parseClaimsJws(encodedJwt)
      case _                     => parser.parseClaimsJwt(jwt)
    }
    println(Console.RED + decodedJwt + Console.RESET)
    val claims = decodedJwt.getBody()
    println(claims)
    if (claims.containsKey("userId") && claims.containsKey("username")) {
      Some(
        JwtUserClaimsData(
          claims.get("userId").toString,
          claims.get("username").toString
        )
      )
    } else None
  }

}

object AuthenticationServiceLive {
  val live: ZLayer[Unit, Throwable, Has[AuthenticationService]] =
    ZLayer.succeed(AuthenticationServiceLive())
}
