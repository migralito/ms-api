package minesweeper

import akka.http.scaladsl.marshalling.PredefinedToResponseMarshallers.fromStatusCodeAndValue
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.json4s.Formats

/**
  * API description.
  *
  * <h3>Create new game</h3>
  * POST /minesweepers<br>
  * Success status code: 200
  *
  * <h3>Get current game state</h3>
  * GET /minesweepers/:id<br>
  * Success status code: 200<br>
  * Success Body:
  * {{{
  * {
  *   minefield: [
  *     [ cellstate, cellstate, cellstate, ..., cellstate ],
  *     [ cellstate, cellstate, cellstate, ..., cellstate ],
  *     ...
  *     [ cellstate, cellstate, cellstate, ..., cellstate ]
  *   ]
  * }
  * }}}
  * where
  * {{{ cellstate = unknown | 0..8 | bombmark | questionmark }}}
  *
  * <h3>Mark a cell with bomb</h3>
  * POST /minesweepers/:id/minefield/x,y/mark<br>
  * Success status code: 200
  *
  * <h3>Mark a cell with a question</h3>
  * POST /minesweepers/:id/minefield/x,y/mark?question<br>
  * Success status code: 200
  *
  * <h3>Mark a cell with bomb</h3>
  * DELETE /minesweepers/:id/minefield/x,y/mark<br>
  * Success status code: 200
  *
  * <h3>Shovel a spot</h3>
  * POST /minesweepers/:id/minefield/x,y/shovel<br>
  * Success status code: 200
  */
class RestAPI(minesweeperService: MinesweeperService) extends Json4sEntityMarshalling {
  implicit val formats: Formats = Json4sFormats.apiFormats

  def route: Route =
    pathPrefix("minesweepers") {
      // POST /minesweepers
      (post & pathEndOrSingleSlash) {
        complete {
          minesweeperService.create()
        }
      } ~
        pathPrefix(Segment) { id ⇒
          // GET /minesweepers/:id
          (get & pathEndOrSingleSlash) {
            complete {
              minesweeperService.get(id)
            }
          } ~
            pathPrefix("minefield" / IntNumber ~ "," ~ IntNumber) { (x,y) ⇒
              // POST /minesweepers/:id/x,y/shovel
              (post & path("shovel")) {
                complete {
                  minesweeperService.shovel(id, (x,y))
                }
              } ~
                // POST /minesweepers/:id/x,y/mark(?question)
                (post & path("mark")) {
                  parameter('question.?) { q ⇒
                    complete {
                      if (q.isEmpty)
                        minesweeperService.bombMark(id, (x,y))
                      else
                        minesweeperService.questionMark(id, (x,y))
                    }
                  }
                } ~
                (delete & path("mark")) {
                  complete {
                    minesweeperService.clearMark(id, (x,y))
                  }
                }
            }
        }
    }

  implicit def moveResultToResponseMarshaller: ToResponseMarshaller[ServiceResult] =
    fromStatusCodeAndValue[Int, ServiceResult] compose { t ⇒
      val status = t match {
        case _: SuccessMove ⇒ 200
        case _: IllegalMove ⇒ 400
        case _: GameAlreadyEnded ⇒ 409
        case MinesweeperNotFound ⇒ 404
      }
      (status, t)
    }
}
