package minesweeper

import scala.util.Random

class MinesweeperService(minesweeperProvider: MinesweeperProvider) {

  private val random = new Random

  def create(maybeMinesweeperField: Option[MinesweeperField] = None): Minesweeper = {
    val id = random.alphanumeric.take(10).mkString
    val minesweeper = maybeMinesweeperField.fold(Minesweeper(id)) { field ⇒ Minesweeper(id, field = field) }
    minesweeperProvider.put(id, minesweeper)
    minesweeper
  }

  def get(id: String): Option[Minesweeper] = minesweeperProvider.get(id)

  def bombMark(id: String, c: Coordinates): ServiceResult = move(id, c, _.field.markBomb(c))

  def questionMark(id: String, c: Coordinates): ServiceResult = move(id, c, _.field.markQuestion(c))

  def clearMark(id: String, c: Coordinates): ServiceResult = move(id, c, _.field.clearMark(c))

  def shovel(id: String, c: Coordinates): ServiceResult = move(id, c, _.field.shovel(c))

  // /////////////////////////////////////////////////////
  // INTERNAL METHODS
  // /////////////////////////////////////////////////////

  private def move(id: String, c: Coordinates, f: Minesweeper ⇒ MoveResult): ServiceResult = {
    minesweeperProvider.get(id).map { minesweeper ⇒
      f(minesweeper)
        .map { case (field, changedCoordinates) ⇒
          val updatedMinesweeper = minesweeper.update(field)
          minesweeperProvider.update(id, updatedMinesweeper)
          (updatedMinesweeper, changedCoordinates)
        }
        .fold({
          case ShovelledSpotFailure ⇒ IllegalMove(ShovelledSpotFailure, minesweeper)
          case MarkedSpotFailure ⇒ IllegalMove(MarkedSpotFailure, minesweeper)
          case BoomFailure(field: MinesweeperField) ⇒
            val updatedMinesweeper = minesweeper.update(field, Killed)
            minesweeperProvider.update(id, updatedMinesweeper)
            SuccessMove(updatedMinesweeper, Seq((c, field elementAt c)))
          case failure ⇒ throw new RuntimeException(s"Unexpected failure $failure")
        }, {
          case (ms, cs) ⇒ SuccessMove(ms, cs)
        })
    }.getOrElse {
      MinesweeperNotFound
    }
  }
}
