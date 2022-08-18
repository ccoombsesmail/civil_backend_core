package civil.models

import java.util.UUID

case class TribunalJury(
    userId: String,
    contentId: UUID,
    contentType: String
)
