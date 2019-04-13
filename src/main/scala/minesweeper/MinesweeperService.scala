package minesweeper

import scala.collection.mutable
import scala.util.{Random, Try}

class MinesweeperService {

  private val random = new Random
  private val minesweepers = mutable.Map.empty[String, Minesweeper]

  def create(): Minesweeper = {
    val id = random.alphanumeric.take(10).mkString
    val minesweeper = Minesweeper(id)
    minesweepers.put(id, minesweeper)
    minesweeper
  }

  def bombMark(id: String, c: (Int, Int)): Try[Minesweeper] = ???

  def questionMark(id: String, c: (Int, Int)): Try[Minesweeper] = ???

  def shovel(id: String, c: Coordinates): Try[Minesweeper] = {
    val ms = minesweepers(id)
    ms.field.shovel(c) map { case (field, changedCoordinates) ⇒
      val newMs = ms.copy(field = field)
      minesweepers.update(id, newMs)
      newMs
    }
  }
}
