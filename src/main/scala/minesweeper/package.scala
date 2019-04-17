import minesweeper.MinesweeperField.random

package object minesweeper {

  // /////////////////////////////////////////////////////
  // SERVICE LAYER OUTPUT: ServiceResult
  // /////////////////////////////////////////////////////

  sealed trait ServiceResult
  case class SuccessMove(minesweeper: Minesweeper, cellChanges: Seq[CellChange] = Seq.empty) extends ServiceResult
  case class IllegalMove(failure: MoveFailure, minesweeper: Minesweeper) extends ServiceResult
  object MinesweeperNotFound extends ServiceResult
  sealed trait InvalidStateResult {
    val reason: String
  }
  case class GameAlreadyEnded(minesweeper: Minesweeper, override val reason: String = "game over") extends ServiceResult with InvalidStateResult
  object GamePaused extends ServiceResult with InvalidStateResult {
    override val reason: String = "game paused"
  }
  object GameNotPaused extends ServiceResult with InvalidStateResult {
    override val reason: String = "game not paused"
  }

  case class CellChange(coordinates: Coordinates, cell: Cell)
  object CellChange {
    implicit def fromTuple(t: (Coordinates, Cell)): CellChange = CellChange(t._1, t._2)
  }

  // /////////////////////////////////////////////////////
  // MODEL LAYER OUTPUT: MoveResult
  // /////////////////////////////////////////////////////

  type MoveResult = Either[MoveFailure, (MinesweeperField, Seq[CellChange])]

  sealed trait MoveFailure
  case class BoomFailure[T](t: T) extends MoveFailure
  object ShovelledSpotFailure extends MoveFailure
  object MarkedSpotFailure extends MoveFailure

  // /////////////////////////////////////////////////////
  // GENERAL
  // /////////////////////////////////////////////////////

  type Matrix = Seq[Seq[Cell]]

  case class Coordinates(x: Int, y: Int) {
    def adjacents: Seq[Coordinates] =
      Coordinates(x-1, y-1) :: Coordinates(x-1, y) :: Coordinates(x-1, y+1) ::
        Coordinates(x, y-1) :: Coordinates(x, y+1) ::
        Coordinates(x+1, y-1) :: Coordinates(x+1, y) :: Coordinates(x+1, y+1) :: Nil
  }

  object Coordinates {
    implicit def fromTuple(t: (Int, Int)): Coordinates = Coordinates(t._1, t._2)
  }

  def buildRandomBombsCoordinates(width: Int, height: Int, q: Int): Seq[Coordinates] = {
    Range(1, q).foldLeft(Seq.empty[Coordinates]) { case (xs, _) ⇒
      xs :+ Coordinates(random.nextInt(width), random.nextInt(height))
    }
  }
}
