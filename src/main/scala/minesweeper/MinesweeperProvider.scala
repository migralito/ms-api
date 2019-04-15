package minesweeper

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}
import org.json4s.Extraction
import org.json4s.jackson.{JsonMethods, Serialization}

trait MinesweeperProvider {
  def put(id: String, minesweeper: Minesweeper): Unit
  def update(id: String, minesweeper: Minesweeper): Unit
  def get(id: String): Option[Minesweeper]
}

class DynamoMinesweepersProvider extends MinesweeperProvider {
  import Json4sFormats.persistenceFormats

  private val table = new DynamoDB(AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).build())
    .getTable("minesweepers")

  override def put(id: String, minesweeper: Minesweeper) =
    table.putItem(new Item()
      .withPrimaryKey("id", minesweeper.id)
      .withString("json", Serialization write minesweeper))

  override def get(id: String): Option[Minesweeper] =
    Option {
      table.getItem(new GetItemSpec().withPrimaryKey("id", id))
    } map { item ⇒
      val ast = JsonMethods.parse(item getString "json")
      Extraction.extract[Minesweeper](ast)
    }

  override def update(id: String, minesweeper: Minesweeper): Unit = put(id, minesweeper)
}