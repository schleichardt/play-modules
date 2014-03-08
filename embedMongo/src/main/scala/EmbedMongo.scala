package info.schleichardt.play2.embed.mongo

import play.api.{ Mode, Configuration, GlobalSettings }
import java.io.File

import play.api.{ Logger, Plugin, Application }
import java.util.logging.{ Logger => JLogger }
import de.flapdoodle.embed.mongo.{ Command, MongodStarter, MongodProcess, MongodExecutable }
import de.flapdoodle.embed.process.distribution.GenericVersion
import de.flapdoodle.embed.mongo.config.{ Net, MongodConfigBuilder, RuntimeConfigBuilder }
import de.flapdoodle.embed.process.runtime.Network
import java.io.IOException
import de.flapdoodle.embed.mongo.distribution.Versions
import EmbedMongoPlugin.ConfigKeys._
import de.flapdoodle.embed.process.config.io.ProcessOutput

object MongoExeFactory {
  def apply(port: Int, versionNumber: String) = {
    val runtimeConfig = new RuntimeConfigBuilder()
      .defaultsWithLogger(Command.MongoD, JLogger.getLogger("embed.mongo"))
      .processOutput(ProcessOutput.getDefaultInstanceSilent())
      .build()
    val runtime = MongodStarter.getInstance(runtimeConfig)
    val config = new MongodConfigBuilder()
      .version(Versions.withFeatures(new GenericVersion(versionNumber)))
      .net(new Net("localhost", port, Network.localhostIsIPv6())).build()
    Logger("play-embed-mongo").info(s"Starting MongoDB on port $port. This might take a while the first time due to the download of MongoDB.")
    runtime.prepare(config)
  }
}

/**
 * Provides a MongoDB instance for development and testing.
 * Hast to be loaded before any other plugin that connects with MongoDB.
 */
class EmbedMongoPlugin(app: Application) extends Plugin {
  private var mongoExe: MongodExecutable = _
  private var process: MongodProcess = _

  override def enabled = app.configuration.getBoolean(KeyEnabled).getOrElse(false)

  override def onStart() {
    val port = conf.getInt(KeyPort).getOrElse(throw new RuntimeException(s"$KeyPort is missing in your configuration"))
    val versionNumber = app.configuration.getString(KeyMongoDbVersion).getOrElse(throw new RuntimeException(s"$KeyMongoDbVersion is missing in your configuration"))
    mongoExe = MongoExeFactory(port, versionNumber)
    try {
      process = mongoExe.start()
    } catch {
      case e: IOException => {
        val message = s"""Maybe the port $port is used by another application. If it was a MongoDB, it might be down now."""
        throw new IOException(message, e)
      }
    }
  }

  def conf: Configuration = app.configuration

  override def onStop() {
    Logger("play-embed-mongo").info(s"Stopping MongoDB.")
    try {
      if (mongoExe != null)
        mongoExe.stop()
    } finally {
      if (process != null)
        process.stop()
    }
  }
}

object EmbedMongoPlugin {
  private[mongo] def freePort() = Network.getFreeServerPort

  object ConfigKeys {
    val KeyPort = "embed.mongo.port"
    val KeyEnabled = "embed.mongo.enabled"
    val KeyMongoDbVersion = "embed.mongo.dbversion"
  }

}

trait DynamicEmbedMongoPort extends GlobalSettings {

  def additionalEmbedMongoPortSettings(port: Int) = Map[String, Any]()

  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode) = {
    val embedMongoActive = config.getBoolean("embed.mongo.enabled").getOrElse(false)
    def dynamicPortRequested = config.getInt("embed.mongo.port").map(_ == 0).getOrElse(false)
    val intermediate = if (embedMongoActive && dynamicPortRequested) {
      val port = EmbedMongoPlugin.freePort
      config ++ Configuration.from(Map("embed.mongo.port" -> port) ++ additionalEmbedMongoPortSettings(port))
    } else config
    super.onLoadConfig(intermediate, path, classloader, mode)
  }
}
