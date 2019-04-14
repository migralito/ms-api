package minesweeper

import java.time.Instant

import scala.util.Random

sealed abstract case class GameStatus(name: String)
object New extends GameStatus("new")
object Playing extends GameStatus("playing")
object Killed extends GameStatus("killed")
object Won extends GameStatus("won")

case class Minesweeper(id: String,
                       creationDateTime: Instant = Instant.now(),
                       status: GameStatus = New,
                       field: MinesweeperField = MinesweeperField(10, 10, 10)) {

  def update(field: MinesweeperField, status: GameStatus = Playing): Minesweeper = copy(
    creationDateTime = Instant.now(),
    field = field,
    status = status)
}

case class MinesweeperField(matrix: Matrix) {

  def updated(c: Coordinates, newCell: Cell): MinesweeperField =
    this.copy(matrix.updated(c.x, matrix(c.x) updated (c.y, newCell)))

  def markBomb(c: Coordinates): MoveResult =
    elementAt(c).markBomb map (cell ⇒ (updated(c, cell), Seq.empty[Coordinates]))

  def markQuestion(c: Coordinates): MoveResult =
    elementAt(c).markQuestion map (cell ⇒ (updated(c, cell), Seq.empty[Coordinates]))

  def clearMark(c: Coordinates): MoveResult =
    elementAt(c).clearMark map (cell ⇒ (updated(c, cell), Seq.empty[Coordinates]))

  def shovel(c: Coordinates): MoveResult =
    elementAt(c).shovel.fold({
      case BoomFailure(cell: Cell) ⇒ Left(BoomFailure(updated(c, cell)))
      case f ⇒ Left(f)
    }, { cell: Cell ⇒
      val partialResult: MoveResult = Right((updated(c, cell), Seq.empty[Coordinates]))

      if (cell.bombsSurrounding > 0) {
        partialResult
      } else {
        contiguous(c).foldLeft(partialResult) {
          case (f @ Left(_), _) ⇒ f
          case (Right((mutatingField, cs)), _c) ⇒
            if (mutatingField.elementAt(_c).concealed)
              mutatingField.shovel(_c) map { case (field, changedCells) ⇒ (field, cs ++ changedCells) }
            else
              Right((mutatingField,cs))
        }
      }
    })

  private def elementAt(coordinates: Coordinates): Cell = matrix(coordinates.x)(coordinates.y)

  private def contiguous(coordinates: Coordinates): Seq[Coordinates] =
    coordinates.adjacents.filter { c ⇒
      c.x >= 0 && c.x < matrix.length &&
        c.y >= 0 && c.y < matrix.head.length
    }
}

object MinesweeperField {
  val random = new Random()

  implicit def fromMatrix(matrix: Matrix): MinesweeperField = MinesweeperField(matrix)

  def apply(x: Int, y: Int, bombs: Int): MinesweeperField =
    concealed(addBombs(virgin(x, y), bombs))

  def addBombs(field: MinesweeperField, bombs: Int): MinesweeperField =
    Range(1, bombs).foldLeft(field) { case (_field, _) ⇒
      addBomb(_field)
    }

  def addBomb(field: MinesweeperField): MinesweeperField = {
    val width = field.matrix.length
    val height = field.matrix.head.length

    val bombCoordinates: Coordinates = (random.nextInt(width), random.nextInt(height))
    val neighbourCoordinates: Seq[Coordinates] = field contiguous bombCoordinates

    val previousCell = field.elementAt(bombCoordinates).asInstanceOf[Underlying]

    neighbourCoordinates.foldLeft(
      field updated (bombCoordinates, previousCell.copy(hasBomb = true))
    ) { case (_field, _c) ⇒
      val currentNeighbour = _field.elementAt(_c).asInstanceOf[Underlying]
      val updatedNeighbour = currentNeighbour.copy(bombsSurrounding = currentNeighbour.bombsSurrounding + 1)
      _field updated (_c, updatedNeighbour)
    }
  }

  def virgin(width: Int, height: Int): MinesweeperField = Seq.fill(width, height)(Underlying())

  def concealed(field: MinesweeperField): MinesweeperField =
    field.copy(matrix = field.matrix.map(_.collect { case u: Underlying ⇒ Unknown(u) }))
}

sealed trait Cell {
  val concealed: Boolean
  def hasBomb: Boolean
  def bombsSurrounding: Int
  def markBomb: Either[MoveFailure, Cell]
  def markQuestion: Either[MoveFailure, Cell]
  def clearMark: Either[MoveFailure, Cell]
  def shovel: Either[MoveFailure, Cell]
  def visibleStatus: String
}

object Cell {
  def unapply(arg: Cell): Option[Int] = Some(arg.bombsSurrounding)
}

case class Underlying(hasBomb: Boolean = false, bombsSurrounding: Int = 0) extends Cell {
  def shovelledSpotFailure = Left(ShovelledSpotFailure)

  override val concealed: Boolean = false

  override def markBomb: Either[MoveFailure, Cell] = shovelledSpotFailure

  override def markQuestion: Either[MoveFailure, Cell] = shovelledSpotFailure

  override def clearMark: Either[MoveFailure, Cell] = shovelledSpotFailure

  override def shovel: Either[MoveFailure, Cell] = shovelledSpotFailure

  override def visibleStatus: String = if (hasBomb) "bomb" else bombsSurrounding.toString
}

trait CellConcealment extends Cell {
  val decorated: Underlying
  override val concealed: Boolean = true
  override val hasBomb: Boolean = decorated.hasBomb
  override val bombsSurrounding: Int = decorated.bombsSurrounding
}

case class Unknown(decorated: Underlying) extends CellConcealment {
  override def markBomb: Either[MoveFailure, Cell] = Right(BombMark(decorated))

  override def markQuestion: Either[MoveFailure, Cell] = Right(QuestionMark(decorated))

  override def clearMark: Either[MoveFailure, Cell] = Right(this)

  override def shovel: Either[MoveFailure, Cell] = if (hasBomb) Left(BoomFailure(decorated)) else Right(decorated)

  override def visibleStatus: String = "unknown"
}

case class BombMark(decorated: Underlying) extends CellConcealment {
  override def markBomb: Either[MoveFailure, Cell] = Right(this)

  override def markQuestion: Either[MoveFailure, Cell] = Right(QuestionMark(decorated))

  override def clearMark: Either[MoveFailure, Cell] = Right(Unknown(decorated))

  override def shovel: Either[MoveFailure, Cell] = Left(MarkedSpotFailure)

  override def visibleStatus: String = "bomb mark"
}

case class QuestionMark(decorated: Underlying) extends CellConcealment {

  override def markBomb: Either[MoveFailure, Cell] = Right(BombMark(decorated))

  override def markQuestion: Either[MoveFailure, Cell] = Right(this)

  override def clearMark: Either[MoveFailure, Cell] = Right(Unknown(decorated))

  override def shovel: Either[MoveFailure, Cell] = Left(MarkedSpotFailure)

  override def visibleStatus: String = "question mark"
}