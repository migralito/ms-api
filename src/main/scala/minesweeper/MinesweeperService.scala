package minesweeper

import scala.collection.mutable
import scala.util.Random

class MinesweeperService {

  private val random = new Random
  private val minesweepers = mutable.Map.empty[String, Minesweeper]

  def create(maybeMinesweeperField: Option[MinesweeperField] = None): Minesweeper = {
    val id = random.alphanumeric.take(10).mkString
    val minesweeper = maybeMinesweeperField.fold(Minesweeper(id)) { field ⇒ Minesweeper(id, field = field) }
    minesweepers.put(id, minesweeper)
    minesweeper
  }

  def get(id: String): Option[Minesweeper] = minesweepers.get(id)

  def bombMark(id: String, c: Coordinates): MoveType = move(id, _.field.markBomb(c))

  def questionMark(id: String, c: Coordinates): MoveType = move(id, _.field.markQuestion(c))

  def clearMark(id: String, c: Coordinates): MoveType = move(id, _.field.clearMark(c))

  def shovel(id: String, c: Coordinates): MoveType = move(id, _.field.shovel(c))

  def move(id: String, f: Minesweeper ⇒ MoveResult): MoveType = {
    val minesweeper = minesweepers(id)
    f(minesweeper)
      .map { case (field, changedCoordinates) ⇒
        val updatedMinesweeper = minesweeper.update(field)
        minesweepers.update(id, updatedMinesweeper)
        (updatedMinesweeper, changedCoordinates)
      }
      .fold ({
        case ShovelledSpotFailure ⇒ NoChangeMove
        case MarkedSpotFailure ⇒ NoChangeMove
        case BoomFailure(field: MinesweeperField) ⇒
          val updatedMinesweeper = minesweeper.update(field, Killed)
          minesweepers.update(id, updatedMinesweeper)
          SuccessMove(updatedMinesweeper, Seq.empty)
        case f ⇒ throw new RuntimeException(s"Unexpected failure $f")
      }, {
        case (ms, cs) ⇒ SuccessMove(ms, cs)
      })
  }
}
