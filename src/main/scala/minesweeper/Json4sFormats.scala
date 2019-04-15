package minesweeper

import java.time.{Duration, Instant}

import org.json4s.JsonAST._
import org.json4s.{CustomSerializer, Extraction, ShortTypeHints}

object Json4sSerializers {

  object CellSerializer extends CustomSerializer[Cell](_ ⇒ ( {
    case _ ⇒ ???
  }, {
    case cell: Cell ⇒ JString(cell.visibleStatus)
  }
  ))

  object InstantSerializer extends CustomSerializer[Instant](_ ⇒ ( {
    case JString(s) ⇒ Instant.parse(s)
  }, {
    case instant: Instant ⇒ JString(instant.toString)
  }
  ))

  object DurationSerializer extends CustomSerializer[Duration](_ ⇒ ( {
    case JString(s) ⇒ Duration.parse(s)
  }, {
    case duration: Duration ⇒ JString(duration.toString)
  }
  ))

  object GameStatusSerializer extends CustomSerializer[GameStatus](_ ⇒ ( {
    case JString(s) ⇒ GameStatus(s)
  }, {
    case gameStatus: GameStatus ⇒ JString(gameStatus.name)
  }
  ))

  object MinesweeperFieldSerializer extends CustomSerializer[MinesweeperField](implicit formats ⇒ ( {
    case _ ⇒ ???
  }, {
    case field: MinesweeperField ⇒ Extraction.decompose(field.matrix)
  }
  ))
}

object Json4sFormats {
  import Json4sSerializers._

  implicit val apiFormats = org.json4s.DefaultFormats + CellSerializer + InstantSerializer + DurationSerializer +
    GameStatusSerializer + MinesweeperFieldSerializer

  implicit val persistenceFormats = org.json4s.DefaultFormats
    .withHints(ShortTypeHints(List(
      classOf[Underlying],
      classOf[Unknown],
      classOf[BombMark],
      classOf[QuestionMark]
    ))) + InstantSerializer + DurationSerializer + GameStatusSerializer
}
