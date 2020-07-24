package zone.nora.simplestats.commands

import java.util
import java.util.UUID

import net.hypixel.api.HypixelAPI
import net.minecraft.client.Minecraft
import net.minecraft.command.{CommandBase, ICommandSender}
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.core.Stats
import zone.nora.simplestats.util.Utils

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

//noinspection DuplicatedCode
class StatsCommand extends CommandBase {

  override def getCommandName: String = "stats"

  override def getCommandUsage(sender: ICommandSender): String = "/stats [player]"

  override def getCommandAliases: util.List[String] = ("hstats" :: "stat" :: Nil).asJava

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        if (args.isEmpty) {
          Utils.error(s"/stats [player] [game]", prefix = true)
          return
        }

        val apiKey = try {
          UUID.fromString(SimpleStats.key)
        } catch {
          case e: IllegalArgumentException =>
            Utils.error("You haven't set your Hypixel API yet. Use /setkey <key>", true)
            return
        }

        val api = new HypixelAPI(UUID.fromString(SimpleStats.key))
        if (!api.getKey.get().isSuccess) {
          Utils.error("Invalid Hypixel API key. Use /setkey <key>", prefix = true)
          api.shutdown()
          return
        }

        val compactMode = args(0).charAt(0).equals(':') // Putting this before a name activates compact mode.
        val serverMode = args(0).charAt(0).equals('*') // Prints stats of everyone on the server in compact mode.

        if (serverMode) {
          val lines: ListBuffer[String] = new ListBuffer
          val players: util.List[String] = new util.ArrayList[String]()
          Minecraft.getMinecraft.getNetHandler.getPlayerInfoMap.foreach { playerInfo =>
            // TODO This way of getting all players returns random 10 digit username sometimes.
            players.add(playerInfo.getGameProfile.getName)
          }

          if (players.size > 24) players.trimEnd(players.size - 24) // Limited to 24 to not get API query limited.
          SimpleStats.logger.info(s"Stat check queue is $players")

          players.foreach { player =>
            val stat = new Stats(api, player, compact = true)
            if (isRequestSuccessful(stat, silent = true)) {
              if (args.length == 1) stat.saveStats()
              else stat.saveStats(args(1))
            }

            lines.append(stat.getStatsInOneLine)
          }

          Utils.breakLine()
          lines.foreach { it => if (!it.isEmpty) Utils.put(it) }
          Utils.breakLine()
        } else {
          val name = args(0).charAt(0) match {
            case ':' =>
              args(0).replaceAll(":", "")
            case '.' =>
              Minecraft.getMinecraft.thePlayer.getName
            case _ =>
              args(0)
          }

          val stat = new Stats(api, name, compact = compactMode)
          if (isRequestSuccessful(stat)) {
            if (args.length == 1) stat.saveStats()
            else stat.saveStats(args(1))
            stat.printStats()
          }
        }

        api.shutdown()
      }
    })
    thread.start()
  }

  private def isRequestSuccessful(stat: Stats, silent: Boolean = false): Boolean = {
    if (!stat.reply.isSuccess) {
      if (!silent) Utils.error(s"Unexpected API error: ${stat.reply.getCause}", prefix = true)
      return false
    } else if (stat.player == null) {
      if (!silent) Utils.error("Invalid player.", prefix = true)
      return false
    }

    true
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = true
}
