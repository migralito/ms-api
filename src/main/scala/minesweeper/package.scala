package object minesweeper {

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
}
