package minesweeper

import java.time.Instant

import org.json4s.JsonAST._
import org.json4s.{CustomSerializer, Extraction}

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

  object MinesweeperFieldSerializer extends CustomSerializer[MinesweeperField](implicit formats ⇒ (
    {
      case _ ⇒ ???
    },
    {
      case field: MinesweeperField ⇒ Extraction.decompose(field.matrix)
    }
  ))

  implicit val formats = org.json4s.DefaultFormats + CellSerializer + InstantSerializer + GameStatusSerializer + MinesweeperFieldSerializer
}
