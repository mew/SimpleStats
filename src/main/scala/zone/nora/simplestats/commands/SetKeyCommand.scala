package zone.nora.simplestats.commands

import java.io.File

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
            SimpleStats.apiKey = key
            SimpleStats.validKey = true
            val file = new File("apikey.txt")
            if (!file.exists()) file.createNewFile()
            FileUtils.writeStringToFile(file, key)
            Utils.put("\u00a7aYour API Key has been set :)", prefix = true)
          } else Utils.error("This is not a valid Hypixel API Key.", prefix = true)
        }
      }
    })
    thread.start()
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true
}
