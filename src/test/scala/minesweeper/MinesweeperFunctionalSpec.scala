package minesweeper

import akka.http.scaladsl.model.StatusCodes.{BadRequest, Conflict, OK}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import minesweeper.Json4sFormats.apiFormats
import org.json4s.jackson.JsonMethods
import org.scalatest.{Matchers, WordSpec}

import scala.collection.mutable

class MinesweeperFunctionalSpec extends WordSpec with Matchers with ScalatestRouteTest {
  private val service = new MinesweeperService(new MinesweeperProvider {
    private val minesweepers = mutable.Map.empty[String, Minesweeper]
    override def put(id: String, minesweeper: Minesweeper): Unit = minesweepers.put(id, minesweeper)
    override def update(minesweeper: Minesweeper): Unit = minesweepers.update(minesweeper.id, minesweeper)
    override def get(id: String): Option[Minesweeper] = minesweepers.get(id)
  })
  private val route = Route.seal(new RestAPI(service).route)

  "Happy path" should {

    // 0 0 0 1 1
    // 0 1 1 2 *
    // 1 2 * 2 1
    // * 2 1 2 1
    // 1 1 0 1 *
    val id = service.create(Some(MinesweeperField(5, 5, Seq((1,4), (2,2), (3,0), (4,4))))).id
    def post(coordinatesAndMove: String) = Post(s"/minesweepers/$id/minefield$coordinatesAndMove")

    "Happy game!" in {

      post("/0,0/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"      , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"      , "2"      , "unknown"),
          Seq("1"      , "2"      , "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown")
        )
      }

      // shovelling again same spot should return 400
      post("/0,0/shovel") ~> route ~> check {
        status shouldBe BadRequest

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "failure").extract[String] shouldBe "spot already shovelled"
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"      , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"      , "2"      , "unknown"),
          Seq("1"      , "2"      , "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown")
        )
      }

      // just checking board
      Get(s"/minesweepers/$id") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "status").extract[String] shouldBe "playing"
        (ast \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"      , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"      , "2"      , "unknown"),
          Seq("1"      , "2"      , "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown")
        )
      }

      // put some bomb mark
      post("/2,2/mark") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown")
        )
      }

      // shovelling a marked spot should return 400
      post("/2,2/shovel") ~> route ~> check {
        status shouldBe BadRequest

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "failure").extract[String] shouldBe "can't shovel a marked spot"
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown")
        )
      }

      // put a question mark
      post("/3,1/mark?question") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"            , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"            , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"            , "bomb mark", "unknown", "unknown"),
          Seq("unknown", "question mark", "unknown"  , "unknown", "unknown"),
          Seq("unknown", "unknown"      , "unknown"  , "unknown", "unknown")
        )
      }

      // better remove that question mark
      Delete(s"/minesweepers/$id/minefield/3,1/mark?question") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown")
        )
      }

      post("/2,3/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "2"      , "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown")
        )
      }

      post("/2,4/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "2"      , "1"      ),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown")
        )
      }

      post("/3,2/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "2"      , "1"      ),
          Seq("unknown", "unknown", "1"        , "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown")
        )
      }

      post("/3,1/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "2"      , "1"      ),
          Seq("unknown", "2"      , "1"        , "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown")
        )
      }

      post("/3,3/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "2"      , "1"      ),
          Seq("unknown", "2"      , "1"        , "2"      , "unknown"),
          Seq("unknown", "unknown", "unknown"  , "unknown", "unknown")
        )
      }

      post("/4,1/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "2"      , "1"      ),
          Seq("unknown", "2"      , "1"        , "2"      , "unknown"),
          Seq("unknown", "1"      , "unknown"  , "unknown", "unknown")
        )
      }

      post("/4,2/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"      , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"      , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"      , "2"      , "bomb mark", "2"      , "1"      ),
          Seq("unknown", "2"      , "1"        , "2"      , "unknown"),
          Seq("unknown", "1"      , "0"        , "1"      , "unknown")
        )
      }

      post("/3,0/mark") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"        , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"        , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"        , "2"      , "bomb mark", "2"      , "1"      ),
          Seq("bomb mark", "2"      , "1"        , "2"      , "unknown"),
          Seq("unknown"  , "1"      , "0"        , "1"      , "unknown")
        )
      }

      post("/4,0/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"        , "0"      , "0"        , "1"      , "unknown"),
          Seq("0"        , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"        , "2"      , "bomb mark", "2"      , "1"      ),
          Seq("bomb mark", "2"      , "1"        , "2"      , "unknown"),
          Seq("1"        , "1"      , "0"        , "1"      , "unknown")
        )
      }

      post("/0,4/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"        , "0"      , "0"        , "1"      , "1"      ),
          Seq("0"        , "1"      , "1"        , "2"      , "unknown"),
          Seq("1"        , "2"      , "bomb mark", "2"      , "1"      ),
          Seq("bomb mark", "2"      , "1"        , "2"      , "unknown"),
          Seq("1"        , "1"      , "0"        , "1"      , "unknown")
        )
      }

      post("/1,4/mark") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"        , "0"      , "0"        , "1"      , "1"        ),
          Seq("0"        , "1"      , "1"        , "2"      , "bomb mark"),
          Seq("1"        , "2"      , "bomb mark", "2"      , "1"        ),
          Seq("bomb mark", "2"      , "1"        , "2"      , "unknown"  ),
          Seq("1"        , "1"      , "0"        , "1"      , "unknown"  )
        )
      }

      post("/3,4/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "won"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0"        , "0"      , "0"        , "1"      , "1"        ),
          Seq("0"        , "1"      , "1"        , "2"      , "bomb mark"),
          Seq("1"        , "2"      , "bomb mark", "2"      , "1"        ),
          Seq("bomb mark", "2"      , "1"        , "2"      , "1"        ),
          Seq("1"        , "1"      , "0"        , "1"      , "unknown"  )
        )
      }
    }
  }

  "Invalid moves" should {

    // 0 0 0 1 1
    // 0 1 1 2 *
    // 1 2 * 2 1
    // * 2 1 2 1
    // 1 1 0 1 *
    val id = service.create(Some(MinesweeperField(5, 5, Seq((1, 4), (2, 2), (3, 0), (4, 4))))).id

    def post(coordinatesAndMove: String) = Post(s"/minesweepers/$id/minefield$coordinatesAndMove")
    val pause = Post(s"/minesweepers/$id/pause")
    val resume = Post(s"/minesweepers/$id/resume")

    "respond with 409 (Conflict)" in {

      resume ~> route ~> check {
        status shouldBe Conflict

        responseAs[String] shouldBe """{"reason":"game not paused"}"""
      }

      post("/0,0/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0", "0", "0", "1", "unknown"),
          Seq("0", "1", "1", "2", "unknown"),
          Seq("1", "2", "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown")
        )
      }

      resume ~> route ~> check {
        status shouldBe Conflict

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "reason").extract[String] shouldBe "game not paused"
      }

      // /////////////////////////////////////////////////////
      // PAUSE AND CHECK REJECTIONS
      // /////////////////////////////////////////////////////

      pause ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "paused"
      }

      post("/0,0/shovel") ~> route ~> check {
        status shouldBe Conflict

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "reason").extract[String] shouldBe "game paused"
      }

      post("/0,0/mark") ~> route ~> check {
        status shouldBe Conflict

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "reason").extract[String] shouldBe "game paused"
      }

      post("/0,0/mark?question") ~> route ~> check {
        status shouldBe Conflict

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "reason").extract[String] shouldBe "game paused"
      }

      pause ~> route ~> check {
        status shouldBe Conflict

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "reason").extract[String] shouldBe "game paused"
      }

      resume ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "playing"
        (ast \ "minesweeper" \ "pauses").extract[Seq[String]].size shouldBe 1
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0", "0", "0", "1", "unknown"),
          Seq("0", "1", "1", "2", "unknown"),
          Seq("1", "2", "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown")
        )
      }

      // /////////////////////////////////////////////////////
      // LETS DIE AND SEE IF WE GET REJECTED ON PLAYS
      // /////////////////////////////////////////////////////

      post("/1,4/shovel") ~> route ~> check {
        status shouldBe OK

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "minesweeper" \ "status").extract[String] shouldBe "killed"
        (ast \ "minesweeper" \ "field").extract[Seq[Seq[String]]] shouldBe Seq(
          Seq("0", "0", "0", "1", "unknown"),
          Seq("0", "1", "1", "2", "bomb"),
          Seq("1", "2", "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown"),
          Seq("unknown", "unknown", "unknown", "unknown", "unknown")
        )
      }

      post("/0,4/shovel") ~> route ~> check {
        status shouldBe Conflict

        val ast = JsonMethods.parse(responseAs[String])
        (ast \ "reason").extract[String] shouldBe "game over"
      }
    }
  }
}