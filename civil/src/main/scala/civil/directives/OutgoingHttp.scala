package civil.directives

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.javadsl.model.headers.HttpCredentials.createOAuth2BearerToken
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{FormData, HttpEntity, HttpMethods, HttpRequest, HttpResponse, MediaRange, MediaTypes}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import civil.models.IncomingRecommendations

import scala.concurrent.Future
import org.json4s._
import org.json4s.jackson.JsonMethods
import org.json4s.jackson.Serialization
import civil.config.Config
import civil.models.ClerkModels.CreateClerkUser
import civil.models.{IncomingRecommendations, Score, UrlsForTFIDFConversion, Words}
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._

import java.util.UUID



case class MetaData(html: Option[String], author_name: String)
//JObject(List((score,JArray(List(JObject(List((recommendedContentId,JString(af16e9f6-bb58-489c-b34f-30703bc5d403)), (similarityScore,JDouble(1.0000000000000002)), (targetContentId,JString(08c12ac7-30f2-4de8-b50c-08f24f3cbd5c)))))))))


//object RecommendationsSerializer extends CustomSerializer[Seq[Recommendations]](formats => ( {
//  case JObject(List(JField("name", JString(name)) :: JField("age", JInt(age)) :: Nil)) => Person(name, age.toInt, None)
//  case JObject(JField("name", JString(name)) :: JField("age", JInt(age)) :: JField("children", JArray(children)) :: Nil) => Person(name, age.toInt, Some(children map (child => formats.customDeserializer(formats).apply(TypeInfo(classOf[Person], None), child).asInstanceOf[Person])))
//  case JObject(JField("name", JString(name)) :: Nil) => Person(name, Person.DefaultAge, None)
//  case JObject(JField("name", JString(name)) :: JField("children", JArray(children)) :: Nil) => Person(name, Person.DefaultAge, Some(children map (child => formats.customDeserializer(formats).apply(TypeInfo(classOf[Person], None), child).asInstanceOf[Person])))
//}, {
//  case Person(name, age, None) => JObject(JField("name", JString(name)) :: JField("age", JInt(age)) :: Nil)
//  case Person(name, age, Some(children)) => JObject(JField("name", JString(name)) :: JField("age", JInt(age)) :: JField("children", formats.customSerializer(formats).apply(children)) :: Nil)
//}))


object OutgoingHttp {
  val AcceptJson = Accept(MediaRange(MediaTypes.`application/json`))
  implicit val formats: Formats = DefaultFormats
  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "alpakka-samples")
  import actorSystem.executionContext
 

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(OK, _, entity, _) => entity.dataBytes
      case notOkResponse =>
        Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
    }


    def getTweetInfo(url: String) = {
      val httpRequest = HttpRequest(uri = s"https://publish.twitter.com/oembed?url=$url&theme=dark&chrome=nofooter")
        .withHeaders(AcceptJson)

      val future: Future[MetaData] =
       Source
        .single(httpRequest) //: HttpRequest
        .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
        .flatMapConcat(extractEntityData) //: ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
          JsonMethods.parse(result).extract[MetaData]
        }
      future
  }
    def sendHTTPToMLService(path: String, urls: UrlsForTFIDFConversion) = {
      val httpRequest = HttpRequest(
        uri = s"${Config().getString("civil.ml_service")}/internal/$path",
        method = HttpMethods.POST,
        entity = HttpEntity(ByteString(s"""{"targetUrl":"${urls.targetUrl}", "compareUrl":"${urls.compareUrl}"}"""))
        )
        .withHeaders(AcceptJson)
      val future =
        Source
          .single(httpRequest) //: HttpRequest
          .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
          .flatMapConcat(extractEntityData) //: ByteString
          .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
          JsonMethods.parse(result).extract[Score]
        }
      future
    }

  def getSimilarityScoresBatch(path: String, targetUrl: String, targetId: UUID, urlsById: Map[String, String]) = {
    val httpRequest = HttpRequest(
      uri = s"${Config().getString("civil.ml_service")}/internal/$path",
      method = HttpMethods.POST,
      entity = HttpEntity(ByteString(s"""{"urlsById":${Serialization.write(urlsById)}, "targetUrl":"${targetUrl}", "targetId":"${targetId}"}"""))
    )
      .withHeaders(AcceptJson)
    val future =
      Source
        .single(httpRequest) //: HttpRequest
        .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
        .flatMapConcat(extractEntityData) //: ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result => {
          Serialization.read[IncomingRecommendations](result)
        }
      }
    future
  }

  def getTopicWordsFromMLService(path: String, url: String) = {
    val httpRequest = HttpRequest(
      uri = s"${Config().getString("civil.ml_service")}/internal/$path",
      method = HttpMethods.POST,
      entity = HttpEntity(ByteString(s"""{"targetUrl":"${url}"}"""))
    )
      .withHeaders(AcceptJson)
    val future =
      Source
        .single(httpRequest) //: HttpRequest
        .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
        .flatMapConcat(extractEntityData) //: ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
        JsonMethods.parse(result).extract[Words]
      }
    future
  }


  def createClerkUser(user: CreateClerkUser) = {
    val form = FormData(user.toMap)
    val httpRequest = HttpRequest(
      uri = s"https://api.clerk.dev/v1/users",
      method = HttpMethods.POST,
      entity = form.toEntity
    )
      .withHeaders(AcceptJson)
      .addCredentials(createOAuth2BearerToken("test_NVpuvoQPIVbP5ALMSCBxeIAnwMHV5L8gmt"))
    println(Console.RED + httpRequest.headers)
    println(Console.BLUE + user.asJson.toString)
    println(Console.RESET)
    val future =
      Source
        .single(httpRequest) //: HttpRequest
        .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
        .flatMapConcat(extractEntityData) //: ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
        JsonMethods.parse(result).extract[String]
      }
    future
  }
}
