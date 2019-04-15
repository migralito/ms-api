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

  def bombMark(id: String, c: Coordinates): ServiceResult = findAndApply(id) {
    onlyWhenPlaying(_) { minesweeper ⇒
      move(minesweeper, c, _.field.markBomb(c))
    }
  }

  def questionMark(id: String, c: Coordinates): ServiceResult = findAndApply(id) {
    onlyWhenPlaying(_) { minesweeper ⇒
      move(minesweeper, c, _.field.markQuestion(c))
    }
  }

  def clearMark(id: String, c: Coordinates): ServiceResult = findAndApply(id) {
    onlyWhenPlaying(_) { minesweeper ⇒
      move(minesweeper, c, _.field.clearMark(c))
    }
  }

  def shovel(id: String, c: Coordinates): ServiceResult = findAndApply(id) {
    onlyWhenPlaying(_) { minesweeper ⇒
      move(minesweeper, c, _.field.shovel(c))
    }
  }

  def pause(id: String): ServiceResult = findAndApply(id) {
    onlyWhenPlaying(_) { minesweeper ⇒
      SuccessMove(update(minesweeper.pause()))
    }
  }

  def resume(id: String): ServiceResult = findAndApply(id) {
    onlyWhenPaused(_) { minesweeper ⇒
      SuccessMove(update(minesweeper.resume()))
    }
  }

  // /////////////////////////////////////////////////////
  // INTERNAL METHODS
  // /////////////////////////////////////////////////////

  private def findAndApply(id: String)(action: Minesweeper ⇒ ServiceResult) =
    minesweeperProvider.get(id).map(action).getOrElse(MinesweeperNotFound)

  private def onlyWhenPlaying(minesweeper: Minesweeper)(action: Minesweeper ⇒ ServiceResult) =
    minesweeper.status match {
      case New | Playing ⇒ action(minesweeper)
      case Won | Killed ⇒ GameAlreadyEnded(minesweeper)
      case Paused ⇒ GamePaused
    }

  private def onlyWhenPaused(minesweeper: Minesweeper)(action: Minesweeper ⇒ ServiceResult) =
    minesweeper.status match {
      case Paused ⇒ action(minesweeper)
      case Won | Killed ⇒ GameAlreadyEnded(minesweeper)
      case New | Playing ⇒ GameNotPaused
    }

  private def move(minesweeper: Minesweeper, c: Coordinates, move: Minesweeper ⇒ MoveResult): ServiceResult =
    move(minesweeper)
      .map(updateGame(minesweeper))
      .fold(
        handleFailures(minesweeper, c),
        t ⇒ SuccessMove(t._1, t._2)
      )

  private def update(minesweeper: Minesweeper) = {
    minesweeperProvider update minesweeper
    minesweeper
  }

  private def updateGame(minesweeper: Minesweeper): ((MinesweeperField, Seq[CellChange])) ⇒ (Minesweeper, Seq[CellChange]) = {
    case (field, changedCoordinates) ⇒
      val gameStatus = if (field.onlyBombsLeft) Won else Playing
      val updatedMinesweeper = minesweeper.update(field, gameStatus)
      minesweeperProvider update updatedMinesweeper
      (updatedMinesweeper, changedCoordinates)
  }

  private def handleFailures(minesweeper: Minesweeper, c: Coordinates): PartialFunction[MoveFailure, ServiceResult] = {
    case ShovelledSpotFailure ⇒ IllegalMove(ShovelledSpotFailure, minesweeper)
    case MarkedSpotFailure ⇒ IllegalMove(MarkedSpotFailure, minesweeper)
    case BoomFailure(field: MinesweeperField) ⇒
      val updatedMinesweeper = minesweeper.update(field, Killed)
      minesweeperProvider update updatedMinesweeper
      SuccessMove(updatedMinesweeper, Seq((c, field elementAt c)))
    case failure ⇒ throw new RuntimeException(s"Unexpected failure $failure")
  }
}
