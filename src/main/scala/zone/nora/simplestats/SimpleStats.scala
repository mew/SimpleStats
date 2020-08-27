package zone.nora.simplestats

import java.io.File
import java.net.URI
import java.util.UUID

import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent}
import org.apache.commons.io.FileUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.logging.log4j.{LogManager, Logger}
import zone.nora.simplestats.commands.hidden.HiddenSkyBlockCommand
import zone.nora.simplestats.commands.{SetKeyCommand, StatsCommand}
import zone.nora.simplestats.listener.EventListener
import zone.nora.simplestats.util.Utils

import scala.util.control.NonFatal

@Mod(modid = "SimpleStats", name = "SimpleStats", version = SimpleStats.VERSION, modLanguage = "scala")
object SimpleStats {
  final val VERSION = "1.4.1" // Current version of SimpleStats
  final val logger: Logger = LogManager.getLogger("SimpleStats")
  var valid = false // Is the API key valid
  var key: UUID = _ // Hypixel API key in UUID form

  @EventHandler
  def init(e: FMLInitializationEvent): Unit = {
    ClientCommandHandler.instance.registerCommand(new SetKeyCommand)
    ClientCommandHandler.instance.registerCommand(new StatsCommand)
    ClientCommandHandler.instance.registerCommand(new HiddenSkyBlockCommand)

    new Thread(new Runnable {
      override def run(): Unit = {
        val file = new File("config/simplestats.cfg")
        if (file.exists) {
          val parser = new JsonParser().parse(FileUtils.readFileToString(file))
          if (Utils.validateKey(parser.getAsJsonObject.get("key").getAsString)) {
            logger.info("Valid Hypixel API key found.")
            key = UUID.fromString(parser.getAsJsonObject.get("key").getAsString)
            valid = true
          } else logger.error(s"Invalid Hypixel API key found at ${file.getCanonicalPath}")
        }

        if (Utils.checkForUpdates() != VERSION) MinecraftForge.EVENT_BUS.register(new EventListener)
      }
    }).start()
  }

  @EventHandler
  def postInit(e: FMLPostInitializationEvent): Unit = {
    var b = false
    // this is obviously super easy to bypass. it is just to deter, not to prevent.
    // if i *really* cared, i would obfuscate the mod and definitely not open source it.
    try {
      val client = HttpClients.createDefault()
      val req = new HttpGet(new URI("https://gist.githubusercontent.com/mew/6686a939151c8fb3be34a54392646189/raw"))
      req.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0")
      val blacklist = EntityUtils.toString(client.execute(req).getEntity)
      b = blacklist.contains(Minecraft.getMinecraft.getSession.getPlayerID.replace("-", ""))
    } catch { case NonFatal(_) => /* nothing */ }
    if (b) throw new RuntimeException("You are blacklisted from using SimpleStats.")
  }
}
