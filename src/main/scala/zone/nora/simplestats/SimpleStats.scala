package zone.nora.simplestats

import java.io.File

import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import org.apache.commons.io.FileUtils
import zone.nora.simplestats.commands.{SetKeyCommand, StatsCommand}
import zone.nora.simplestats.util.Utils

@Mod(modid = "SimpleStats", name = "SimpleStats", version = "1.0", modLanguage = "scala")
object SimpleStats {
  var apiKey = ""
  var validKey = false

  @EventHandler
  def init(e: FMLInitializationEvent): Unit = {
    ClientCommandHandler.instance.registerCommand(new SetKeyCommand)
    ClientCommandHandler.instance.registerCommand(new StatsCommand)

    val file = new File("apikey.txt")
    if (!file.exists()) {
      file.createNewFile()
      return
    }
    val s = FileUtils.readFileToString(file)
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        if (Utils.validateKey(s)) {
          validKey = true
          apiKey = s
        }
      }
    })
    thread.start()
  }
}