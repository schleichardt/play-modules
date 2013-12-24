package info.schleichardt.play2.embed.mongo

import org.specs2.mutable._
import EmbedMongoPlugin.ConfigKeys._
import com.mongodb.{BasicDBObject, MongoClient}
import play.api.Play
import play.api.test.FakeApplication
import play.api.test.WithApplication

class ModuleSpec extends Specification {
  "The play-embed-mongo module" should {
    "be able to connect with a MongoDB on a fixed port" in new WithMongoApp {
      val user = usersCollection.findOne()
      user.get("firstname") === "Max"
    }

    "can be disabled" in new WithMongoApp(KeyPort -> EmbedMongoPlugin.freePort, KeyEnabled -> "false") {
      val collection = MongoUtils.collection("test")("not-there")
      collection.insert(new BasicDBObject) must throwA[com.mongodb.MongoException]
    }

    "be able to start MongoDB with a free port" in new WithMongoApp(KeyPort -> "0") {
      //test with sbt -Dembed.mongo.enabled=true -Dembed.mongo.port=0 ~run
      true === false
    }.pendingUntilFixed("play refuses to change the port")
  }
}


object MongoUtils {
  def mongoPort = {
    val port = Play.current.configuration.getInt(KeyPort).get
    println("port=" + port)
    port
  }

  def newMongoClient = new MongoClient("localhost", mongoPort)

  def collection(db: String)(collection: String) = newMongoClient.getDB(db).getCollection(collection)
}

abstract class WithMongoApp(config: Pair[String, Any]*) extends
WithApplication(
  FakeApplication(
    additionalConfiguration = (Map(KeyEnabled -> "true", KeyPort -> EmbedMongoPlugin.freePort) ++ config.toMap),
    additionalPlugins = Seq("info.schleichardt.play2.embed.mongo.EmbedMongoPlugin")
  )
) {
  lazy val port = WithMongoApp.getConfiguredPort
  lazy val client = new MongoClient("localhost", port)
  lazy val db = client.getDB("test")
  lazy val usersCollection = {
    println(port)
    val coll = db.getCollection("users")
    val document = new BasicDBObject("firstname", "Max").append("lastname", "Mustermann")
    coll.insert(document)
    coll
  }
}

object WithMongoApp {
  def getConfiguredPort = Play.current.configuration.getInt(KeyPort).getOrElse(throw new RuntimeException(s"no port specified with $KeyPort"))
}