package minesweeper

import java.time.Instant

import scala.util.{Failure, Random, Success, Try}


case class Minesweeper(id: String,
                       creationDateTime: Instant = Instant.now(),
                       status: String = "new",
                       field: MinesweeperField = MinesweeperField(10, 10, 10))

case class MinesweeperField(matrix: Matrix) {

  def updated(c: Coordinates, newCell: Cell): MinesweeperField =
    this.copy(matrix.updated(c.x, matrix(c.x) updated (c.y, newCell)))

  def shovel(c: Coordinates): Try[(MinesweeperField, Seq[Coordinates])] =
    elementAt(c).shovel flatMap { cell: Cell ⇒
      val partialResult = Try((updated(c, cell), Seq.empty[Coordinates]))

      if (cell.bombsSurrounding > 0) {
        partialResult
      } else {
        contiguous(c).foldLeft(partialResult) {
          case (f @ Failure(_), _) ⇒ f
          case (Success((mutatingField, cs)), _c) ⇒
            if (mutatingField.elementAt(_c).concealed)
              mutatingField.shovel(_c) map { case (field, changedCells) ⇒ (field, cs ++ changedCells) }
            else
              Success((mutatingField,cs))
        }
      }
    }

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
    field.copy(matrix = field.matrix.map(_.map { case u: Underlying ⇒ Unknown(u) }))
}

sealed trait Cell {
  val concealed: Boolean
  def hasBomb: Boolean
  def bombsSurrounding: Int
  def markBomb: Try[Cell]
  def markQuestion: Try[Cell]
  def clearMark: Try[Cell]
  def shovel: Try[Cell]
  def visibleStatus: String
}

object Cell {
  def unapply(arg: Cell): Option[Int] = Some(arg.bombsSurrounding)
}

case class Underlying(hasBomb: Boolean = false, bombsSurrounding: Int = 0) extends Cell {
  def shovelledSpotFailure = Failure(new IllegalArgumentException("Shovelled spot"))

  override val concealed: Boolean = false

  override def markBomb: Try[Cell] = shovelledSpotFailure

  override def markQuestion: Try[Cell] = shovelledSpotFailure

  override def clearMark: Try[Cell] = shovelledSpotFailure

  override def shovel: Try[Cell] = shovelledSpotFailure

  override def visibleStatus: String = if (hasBomb) "bomb" else bombsSurrounding.toString
}

trait CellConcealment extends Cell {
  val decorated: Underlying
  override val concealed: Boolean = true
  override val hasBomb: Boolean = decorated.hasBomb
  override val bombsSurrounding: Int = decorated.bombsSurrounding
}

case class Unknown(decorated: Underlying) extends CellConcealment {
  override def markBomb: Try[Cell] = Success(BombMark(decorated))

  override def markQuestion: Try[Cell] = Success(QuestionMark(decorated))

  override def clearMark: Try[Cell] = Success(this)

  override def shovel: Try[Cell] = if (hasBomb) Failure(BombException) else Success(decorated)

  override def visibleStatus: String = "unknown"
}

case class BombMark(decorated: Underlying) extends CellConcealment {
  override def markBomb: Try[Cell] = Success(this)

  override def markQuestion: Try[Cell] = Success(QuestionMark(decorated))

  override def clearMark: Try[Cell] = Success(Unknown(decorated))

  override def shovel: Try[Cell] = Failure(new IllegalArgumentException("Spot marked"))

  override def visibleStatus: String = "bomb mark"
}

case class QuestionMark(decorated: Underlying) extends CellConcealment {

  override def markBomb: Try[Cell] = Success(BombMark(decorated))

  override def markQuestion: Try[Cell] = Success(this)

  override def clearMark: Try[Cell] = Success(Unknown(decorated))

  override def shovel: Try[Cell] = Failure(new IllegalArgumentException("Spot marked"))

  override def visibleStatus: String = "question mark"
}