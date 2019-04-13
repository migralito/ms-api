package minesweeper

import java.time.Instant

import org.json4s.CustomSerializer
import org.json4s.JsonAST._

object Json4sFormats {

  object CellSerializer extends CustomSerializer[Cell](_ ⇒ (
    {
      case JString(text) ⇒ ???
    },
    {
      case cell: Cell ⇒ JString(cell.visibleStatus)
    }
  ))

  object InstantSerializer extends CustomSerializer[Instant](_ ⇒ (
    {
      case JString(text) ⇒ Instant.parse(text)
    },
    {
      case instant: Instant ⇒ JString(instant.toString)
    }
  ))

  implicit val formats = org.json4s.DefaultFormats + CellSerializer + InstantSerializer

}
