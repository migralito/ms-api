package minesweeper

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.json4s.Formats

class RestAPI(minesweeperService: MinesweeperService) extends Json4sEntityMarshalling {
  implicit val formats: Formats = Json4sFormats.formats

  val coordinatesRegex = "([0-9])-([0-9])".r

  def route: Route =
    pathPrefix("minesweepers") {
      (post & pathEndOrSingleSlash) {
        complete {
          minesweeperService.create()
        }
      } ~
        pathPrefix(Segment / "minefield" / Segment) { (id, coord) ⇒
          val c = coord match { case coordinatesRegex(x,y) ⇒ (x.toInt, y.toInt) }
          (post & path("shovel")) {
            complete {
              minesweeperService.shovel(id, c)
            }
          } ~
            (post & path("bomb-mark")) {
              complete {
                minesweeperService.bombMark(id, c)
              }
            } ~
            (post & path("question-mark")) {
              complete {
                minesweeperService.questionMark(id, c)
              }
            }
        }
    }
}
