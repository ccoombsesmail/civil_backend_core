package civil.models.enums

// import scala.Enumeration

// object TopicCategories extends Enumeration  {
//   type TopicCategories = Value
//   val Technology, Medicine, Politics = Value
// }


import enumeratum._

sealed trait CivilityActions extends EnumEntry

case object CivilityActions extends Enum[CivilityActions] with CirceEnum[CivilityActions] {

  case object AddC  extends CivilityActions
  case object Medicine extends CivilityActions
  case object Politics  extends CivilityActions
  case object General extends CivilityActions

  val values = findValues

}