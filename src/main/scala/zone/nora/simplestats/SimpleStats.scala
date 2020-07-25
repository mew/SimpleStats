package zone.nora.simplestats

import java.io.File

import com.google.gson.{JsonObject, JsonParser}
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.{LogManager, Logger}
import zone.nora.simplestats.commands.{SetKeyCommand, StatsCommand}
import zone.nora.simplestats.listener.EventListener
import zone.nora.simplestats.util.Utils

@Mod(modid = "SimpleStats", name = "SimpleStats", version = SimpleStats.VERSION, modLanguage = "scala")
object SimpleStats {
  final val VERSION = "1.2" // Current version of SimpleStats
  val logger: Logger = LogManager.getLogger("SimpleStats")
  var key = "" // Hypixel API key

  @EventHandler
  def init(e: FMLInitializationEvent): Unit = {
    ClientCommandHandler.instance.registerCommand(new SetKeyCommand)
    ClientCommandHandler.instance.registerCommand(new StatsCommand)

    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        val file = new File("config/simplestats.cfg")
        if (file.exists) {
          val parser = new JsonParser().parse(FileUtils.readFileToString(file))
          key = parser.getAsJsonObject.get("key").getAsString
          if (Utils.validateKey(key)) logger.info("Valid Hypixel API key found.")
          else logger.error("Invalid Hypixel API key found at " + file.getCanonicalPath)
        } else {
          val oldConfigFile = new File("apikey.txt")
          // Grab Hypixel API key from where it was stored in older versions of the mod.
          if (oldConfigFile.exists) {
            logger.info("Found old config file.")
            val apiKey = FileUtils.readFileToString(oldConfigFile)
            if (Utils.validateKey(apiKey)) {
              key = apiKey
              file.createNewFile
              val jsonObject = new JsonObject
              jsonObject.addProperty("key", apiKey)
              FileUtils.writeStringToFile(file, jsonObject.toString)
              logger.info("Brought over Hypixel API key from old config file.")
            }
            oldConfigFile.delete()
          }
        }

        if (Utils.checkForUpdates() != VERSION) MinecraftForge.EVENT_BUS.register(new EventListener)
      }
    })
    thread.start()
  }
}
