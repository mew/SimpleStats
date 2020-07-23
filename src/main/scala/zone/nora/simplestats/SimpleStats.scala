package zone.nora.simplestats

import java.io.File

import com.google.gson.JsonParser
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import org.apache.commons.io.FileUtils
import zone.nora.simplestats.commands.{SetKeyCommand, StatsCommand}
import zone.nora.simplestats.listener.EventListener
import zone.nora.simplestats.util.Utils

@Mod(modid = "SimpleStats", name = "SimpleStats", version = SimpleStats.VERSION, modLanguage = "scala")
object SimpleStats {

  final val VERSION = "1.2" // Current version of SimpleStats
  var key = "" // Hypixel API key

  @EventHandler
  def init(e: FMLInitializationEvent): Unit = {
    ClientCommandHandler.instance.registerCommand(new SetKeyCommand)
    ClientCommandHandler.instance.registerCommand(new StatsCommand)

    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        val file = new File("config/simplestats.cfg")
        if (file.exists()) {
          val parser = new JsonParser().parse(FileUtils.readFileToString(file))
          key = parser.getAsJsonObject.get("key").getAsString
          if (Utils.validateKey(key)) println("Valid Hypixel API key found.")
          else println("Invalid Hypixel API key found at " + file.getCanonicalPath)
        }

        if (Utils.checkForUpdates() != VERSION) MinecraftForge.EVENT_BUS.register(new EventListener)
      }
    })
    thread.start()
  }
}