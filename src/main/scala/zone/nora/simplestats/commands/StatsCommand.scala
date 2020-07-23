package zone.nora.simplestats.commands

import java.util
import java.util.UUID

import com.google.gson.JsonObject
import net.hypixel.api.HypixelAPI
import net.hypixel.api.reply.PlayerReply
import net.minecraft.command.{CommandBase, ICommandSender}
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.util.{Stats, Utils}

class StatsCommand extends CommandBase {

  override def getCommandName: String = "stats"

  override def getCommandUsage(sender: ICommandSender): String = "/stats [player]"

  override def getCommandAliases: util.List[String] = util.Arrays.asList[String]("hstats", "stat")

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        if (args.isEmpty) {
          Utils.error(s"/stats [player] [game]", prefix = true)
          return
        }

        if (!SimpleStats.validity) {
          Utils.error("Invalid Hypixel API key. Use /setkey <key>", prefix = true)
          return
        }

        val api = new HypixelAPI(UUID.fromString(SimpleStats.key))
        val stat = new Stats(api, args(0))
        val playerReply: PlayerReply = stat.reply

        if (!playerReply.isSuccess) {
          Utils.error(s"Unexpected API error: ${playerReply.getCause}", prefix = true)
          api.shutdown()
          return
        }

        val player: JsonObject = stat.player
        if (player == null) {
          Utils.error("Invalid player.", prefix = true)
          api.shutdown()
          return
        }

        if (args.length == 1) stat.printStats()
        else stat.printStats(args(1))
        api.shutdown()
      }
    })
    thread.start()
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = true
}
