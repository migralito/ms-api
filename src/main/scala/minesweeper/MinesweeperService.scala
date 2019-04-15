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

  private def move(id: String, c: Coordinates, theMove: Minesweeper ⇒ MoveResult): ServiceResult = {
    minesweeperProvider.get(id).map { minesweeper ⇒
      asServiceResult(id, minesweeper, c) {
        theMove(minesweeper) map updateGame(id, minesweeper)
      }
    }.getOrElse {
      MinesweeperNotFound
    }
  }

  private def updateGame(id: String, minesweeper: Minesweeper): ((MinesweeperField, Seq[CellChange])) ⇒ (Minesweeper, Seq[CellChange]) = {
    case (field, changedCoordinates) ⇒
      val gameStatus = if (field.onlyBombsLeft) Won else Playing
      val updatedMinesweeper = minesweeper.update(field, gameStatus)
      minesweeperProvider.update(id, updatedMinesweeper)
      (updatedMinesweeper, changedCoordinates)
  }

  private def asServiceResult(id: String, minesweeper: Minesweeper, c: Coordinates)
                             (partialResult: Either[MoveFailure, (Minesweeper, Seq[CellChange])]): ServiceResult =
    partialResult.fold({
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

}
