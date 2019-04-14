package object minesweeper {
  type Matrix = Seq[Seq[Cell]]
  type MoveResult = Either[MoveFailure, (MinesweeperField, Seq[Coordinates])]

  sealed trait MoveType
  case class SuccessMove(minesweeper: Minesweeper, changedRows: Seq[Coordinates]) extends MoveType
  object NoChangeMove extends MoveType

  sealed trait MoveFailure
  case class BoomFailure[T](t: T) extends MoveFailure
  object ShovelledSpotFailure extends MoveFailure
  object MarkedSpotFailure extends MoveFailure

  case class Coordinates(x: Int, y: Int) {
    def adjacents: Seq[Coordinates] =
      Coordinates(x-1, y-1) :: Coordinates(x-1, y) :: Coordinates(x-1, y+1) ::
        Coordinates(x, y-1) :: Coordinates(x, y+1) ::
        Coordinates(x+1, y-1) :: Coordinates(x+1, y) :: Coordinates(x+1, y+1) :: Nil
  }

  object Coordinates {
    implicit def fromTuple(t: (Int, Int)): Coordinates = Coordinates(t._1, t._2)
  }
}
