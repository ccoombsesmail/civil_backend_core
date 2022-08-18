package civil.services

import civil.models.{BadRequest, ErrorInfo, Follows, IncomingUser, OutgoingUser, TagExists, UpdateUserBio, WebHookData}
import civil.models.enums.ClerkEventType

import java.util.UUID
import civil.repositories.{UsersRepository, UsersRepositoryLive}

import java.time.LocalDateTime
import zio._

import java.security.MessageDigest
import pdi.jwt.JwtClaim

import java.time.Instant
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtCirce

trait UsersService {
  def upsertDidUser(incomingUser: IncomingUser): ZIO[Any, ErrorInfo, OutgoingUser]

  def insertOrUpdateUserHook(webHookData: WebHookData, eventType: ClerkEventType):  ZIO[Any, ErrorInfo, Unit]

  def getUser(id: String, requesterId: String):  ZIO[Any, ErrorInfo, OutgoingUser]
  def updateUserIcon(username: String, iconSrc: String):  ZIO[Any, ErrorInfo, OutgoingUser]
  def updateUserBio(userId: String, bioInfo: UpdateUserBio): ZIO[Any, ErrorInfo, OutgoingUser]
  def createUserTag(jwt: String, jwtType: String, tag: String): ZIO[Any, ErrorInfo, OutgoingUser]
  def checkIfTagExists(tag: String): ZIO[Any, ErrorInfo, TagExists]

}


object UsersService {

  def upsertDidUser(incomingUser: IncomingUser): ZIO[Has[UsersService], ErrorInfo, OutgoingUser]=
    ZIO.serviceWith[UsersService](_.upsertDidUser(incomingUser))

  def insertOrUpdateUserHook(webHookData: WebHookData, eventType: ClerkEventType): ZIO[Has[UsersService], ErrorInfo, Unit] =
    ZIO.serviceWith[UsersService](_.insertOrUpdateUserHook(webHookData, eventType))
  
  def getUser(id: String, requesterId: String):  ZIO[Has[UsersService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersService](_.getUser(id, requesterId))

  def updateUserIcon(username: String, iconSrc: String):  ZIO[Has[UsersService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersService](_.updateUserIcon(username, iconSrc))

  def updateUserBio(userId: String, bioInfo: UpdateUserBio): ZIO[Has[UsersService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersService](_.updateUserBio(userId, bioInfo))

  def createUserTag(
     jwt: String,
     jwtType: String,
     tag: String
  ): ZIO[Has[UsersService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersService](_.createUserTag(jwt, jwtType, tag))

  def checkIfTagExists(tag: String): ZIO[Has[UsersService], ErrorInfo, TagExists] =
    ZIO.serviceWith[UsersService](_.checkIfTagExists(tag))
}



case class UsersServiceLive(usersRepository: UsersRepository) extends UsersService  {

  override def upsertDidUser(incomingUser: IncomingUser): ZIO[Any, ErrorInfo, OutgoingUser] = {
    usersRepository.upsertDidUser(incomingUser)
  }

  override def insertOrUpdateUserHook(webHookData: WebHookData, eventType: ClerkEventType): ZIO[Any, ErrorInfo, Unit] = {
    usersRepository.insertOrUpdateUserHook(webHookData, eventType)
  }

  override def getUser(id: String, requesterId: String): ZIO[Any, ErrorInfo, OutgoingUser] = {
    usersRepository.getUser(id, requesterId)
  }

  override def updateUserIcon(username: String, iconSrc: String): ZIO[Any, ErrorInfo, OutgoingUser] = {
    usersRepository.updateUserIcon(username, iconSrc)
  }

  override def updateUserBio(userId: String, bioInfo: UpdateUserBio): ZIO[Any, ErrorInfo, OutgoingUser] = {
    usersRepository.updateUserBio(userId, bioInfo)
  }

  override def createUserTag(jwt: String, jwtType: String, tag: String): ZIO[Any, ErrorInfo, OutgoingUser] = {
    val authenticationService = AuthenticationServiceLive()
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      outgoingUser <- usersRepository.createUserTag(userData.userId, tag)
    } yield outgoingUser
  }

  override def checkIfTagExists(tag: String): ZIO[Any, ErrorInfo, TagExists] = {
    usersRepository.checkIfTagExists(tag)
  }
}


object UsersServiceLive {
  val live: ZLayer[Has[UsersRepository], Throwable, Has[UsersService]] = {
    for {
      usersRepo <- ZIO.service[UsersRepository]
    } yield UsersServiceLive(usersRepo)
  }.toLayer
}