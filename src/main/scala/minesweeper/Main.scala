package minesweeper

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main {

  def main(args: Array[String]) {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val minesweeperService = new MinesweeperService()
    Http().bindAndHandle(new RestAPI(minesweeperService).route, "localhost", 8080)

    Await.ready(Future.never, Duration.Inf)
  }

  // dynamo AKIAYYB7IBPRJUSJN5QT:OehSYnVdSSm9WM9sXYyto0uxD6NYVMps/CazZ0z/
}