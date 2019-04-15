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

    val minesweeperService = new MinesweeperService(new DynamoMinesweepersProvider)
    Http().bindAndHandle(new RestAPI(minesweeperService).route, "0.0.0.0", 80)

    Await.ready(Future.never, Duration.Inf)
  }
}