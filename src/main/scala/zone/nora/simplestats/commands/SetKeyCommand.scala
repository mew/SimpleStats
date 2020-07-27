package zone.nora.simplestats.commands

import java.io.File

import com.google.gson.JsonObject
import net.minecraft.command.{CommandBase, ICommandSender}
import org.apache.commons.io.FileUtils
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.util.Utils

class SetKeyCommand extends CommandBase {

  override def getCommandName: String = "setkey"

  override def getCommandUsage(sender: ICommandSender): String = "/setkey [api-key]"

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        if (args.isEmpty) Utils.error("/setkey [api-key]", prefix = true) else {
          val key = args(0)
          if (key.length == 36 && Utils.validateKey(key)) {
            SimpleStats.key = key
            SimpleStats.valid = true

            val file = new File("config/simplestats.cfg")
            if (!file.exists()) file.createNewFile()

            val jsonObject = new JsonObject()
            jsonObject.addProperty("key", key)

            FileUtils.writeStringToFile(file, jsonObject.toString)
            Utils.put("\u00a7aYour API Key has been set.", prefix = true)
          } else Utils.error("This is not a valid Hypixel API key.", prefix = true)
        }
      }
    })
    thread.start()
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true
}
