package civil.models

import civil.models.enums.ClerkEventType

import java.util.UUID
import sttp.tapir.generic.auto._
import sttp.tapir.Schema


case class Users(
    userId: String,
    email: Option[String],
    username: String,
    tag: Option[String],
    iconSrc: Option[String],
    civility: Float,
    createdAt: Long,
    consortiumMember: Boolean = false,
    bio: Option[String],
    experience: Option[String],
    isDidUser: Boolean
)

case class OutgoingUser(
    userId: String,
    email: Option[String],
    username: String,
    tag: Option[String],
    iconSrc: Option[String],
    civility: Float,
    createdAt: Long,
    consortiumMember: Boolean = false,
    isFollowing: Option[Boolean],
    bio: Option[String],
    experience: Option[String],
    isDidUser: Boolean
)

case class IncomingUser(userId: String, username: String, iconSrc: Option[String]) {}

case class UpdateUserIcon(username: String, iconSrc: String) {}

case class UpdateUserBio(bio: Option[String], experience: Option[String])

case class TagData(tag: String)

case class TagExists(tagExists: Boolean)

case class WebHookEvent(
    data: WebHookData,
    `object`: String,
    `type`: ClerkEventType
)



case class WebHookData(
    birthday: String,
    created_at: Long,
    email_addresses: Seq[EmailData],
    external_accounts: Seq[ExternalAccountsData],
    external_id: Option[String],
    first_name: Option[String],
    gender: String,
    id: String,
    last_name: Option[String],
    `object`: String,
    password_enabled: Boolean,
    phone_numbers: Seq[String],
    primary_email_address_id: String,
    primary_phone_number_id: Option[String],
    primary_web3_wallet_id: Option[String],
    private_metadata: PrivateMetadata,
    profile_image_url: String,
    public_metadata: PublicMetadata,
    two_factor_enabled: Boolean,
    unsafe_metadata: UnsafeMetadata,
    updated_at: Long,
    username: Option[String],
    web3_wallets: Seq[Web3Wallet]
)

case class EmailData(
    email_address: String,
    id: String,
    linked_to: Seq[LinkedToData],
    `object`: String,
    verification: VerificationData
)

case class LinkedToData(
    id: Option[String],
    `type`: Option[String]
)

case class VerificationData(
    attempts: Option[Int],
    expire_at: Option[Long],
    status: Option[String],
    strategy: Option[String]
)

case class ExternalAccountsData(
    approved_scopes: Option[String],
    email_address: Option[String],
    family_name: Option[String],
    given_name: Option[String],
    google_id: Option[String],
    id: Option[String],
    `object`: Option[String],
    picture: Option[String]
)


case class Web3Wallet(
      id: String,
      `object`: String,
      verification: Option[Web3WalletVerification],
      web3_wallet: String      
)

case class Web3WalletVerification(
    attempts: Int,
    expire_at: Long,
    nonce: String,
    status: String,
    strategy: String
)

case class PrivateMetadata()
case class PublicMetadata(consortiumMember: Option[Boolean])
case class UnsafeMetadata()


case class JwtUserClaimsData(userId: String, username: String)