package zone.nora.simplestats.commands

import java.util.UUID

import net.hypixel.api.HypixelAPI
import net.hypixel.api.util.ILeveling
import net.minecraft.command.{CommandBase, ICommandSender}
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.util.Utils

class StatsCommand extends CommandBase {
  override def getCommandName: String = "stats"

  override def getCommandUsage(sender: ICommandSender): String = "/stats [player]"

  //override def getCommandAliases: util.List[String] = ???

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        if (!SimpleStats.validKey) {
          Utils.breakline()
          Utils.error("Your api key is not valid :(")
          Utils.error("You can set your api key with '/setkey [Hypixel API Key]")
          Utils.error("You can get your API key by typing \"/api new\" in a Hypixel lobby.")
          Utils.breakline()
          return
        }
        if (args.length == 0) {
          Utils.error("/stats [player]")
          return
        }
        val playerReply = new HypixelAPI(UUID.fromString(SimpleStats.apiKey)).getPlayerByName(args(0)).get()
        if (!playerReply.isSuccess) {
          Utils.error(s"Unexpected Error: ${playerReply.getCause}")
          return
        }
        val player = playerReply.getPlayer
        Utils.breakline()
        Utils.put(s"Stats of ${player.get("displayname").getAsString}")
        printStat("Hypixel Level", ILeveling.getLevel(player.get("networkExp").getAsDouble))
        printStat("Achievement Points", player.get("achievementPoints"))
        printStat("Karma", player.get("karma"))
        printStat("Discord", try {
          player.get("socialMedia").getAsJsonObject.get("links").getAsJsonObject.get("DISCORD").getAsString
        } catch { case _: NullPointerException => "N/A" })
        printStat("First Login", Utils.parseTime(player.get("firstLogin").getAsLong))
        printStat("Last Login", Utils.parseTime(player.get("lastLogin").getAsLong))
        printStat("Online", try {
          player.get("lastLogin").getAsLong > player.get("lastLogout").getAsLong
        } catch { case _: NullPointerException => false })
        Utils.breakline()
      }
    })
    thread.start()
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = true

  def printStat(name: String, value: Any): Unit = Utils.put(s"$name: ${if (value == null) "N/A" else value}")
}
