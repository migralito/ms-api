package minesweeper

import java.time.Instant

import org.json4s.CustomSerializer
import org.json4s.JsonAST._

object Json4sFormats {

  object CellSerializer extends CustomSerializer[Cell](_ ⇒ (
    {
      case _ ⇒ ???
    },
    {
      case cell: Cell ⇒ JString(cell.visibleStatus)
    }
  ))

  object InstantSerializer extends CustomSerializer[Instant](_ ⇒ (
    {
      case _ ⇒ ???
    },
    {
      case instant: Instant ⇒ JString(instant.toString)
    }
  ))

  object GameStatusSerializer extends CustomSerializer[GameStatus](_ ⇒ (
    {
      case _ ⇒ ???
    },
    {
      case gameStatus: GameStatus ⇒ JString(gameStatus.name)
    }
  ))

  implicit val formats = org.json4s.DefaultFormats + CellSerializer + InstantSerializer + GameStatusSerializer

}
