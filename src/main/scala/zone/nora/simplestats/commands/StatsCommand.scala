package zone.nora.simplestats.commands

import java.util
import java.util.concurrent.{ExecutorService, Executors}

import net.hypixel.api.HypixelAPI
import net.minecraft.client.Minecraft
import net.minecraft.command.{CommandBase, ICommandSender}
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.core.Stats
import zone.nora.simplestats.util.Utils

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class StatsCommand extends CommandBase {

  private final val COMMAND_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()

  override def getCommandName: String = "stats"

  override def getCommandUsage(sender: ICommandSender): String = "/stats [player] [game]"

  override def getCommandAliases: util.List[String] = ("hstats" :: Nil).asJava

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    COMMAND_EXECUTOR.execute(new Runnable {
      override def run(): Unit = {
        if (args.isEmpty) {
          Utils.error(s"/stats [player] [game]", prefix = true)
          return
        }

        if (!SimpleStats.valid) {
          Utils.error("Invalid Hypixel API key!", prefix = true)
          return
        }

        // Hypixel API instance
        val api = new HypixelAPI(SimpleStats.key)
        val compactMode = args(0).charAt(0).equals(':') // Putting this before a name activates compact mode.
        val serverMode = args(0).charAt(0).equals('*') // Prints stats of everyone on the server in compact mode.

        if (serverMode) {
          val players: util.List[String] = new util.ArrayList[String]()
          Minecraft.getMinecraft.getNetHandler.getPlayerInfoMap.foreach { playerInfo =>
            players.add(playerInfo.getGameProfile.getName)
          }

          if (players.size > 24) players.trimEnd(players.size - 24) // Limited to 24 to not get API query limited.
          SimpleStats.logger.info(s"Stats check queue is $players")

          val listBuffer: ListBuffer[String] = new ListBuffer[String]

          // Changed from foreach as return wont work in a for each loop.
          for (player <- players) {
            val stat = new Stats(api, player, compact = true)
            if (!isSuccess(stat)) {
              api.shutdown()
              return
            }

            if (stat.player == null) Utils.error(s"Invalid player: $player")
            else {
              if (args.length == 1) stat.saveStats()
              else stat.saveStats(args(1))

              if (stat.getStatsInOneLine.isEmpty) Utils.error(s"Error during getting statistics of $player")
              else listBuffer.add(stat.getStatsInOneLine)
            }
          }

          if (listBuffer.nonEmpty) {
            Utils.breakLine()
            listBuffer.foreach { it => if (!it.isEmpty) Utils.put(it) }
            Utils.breakLine()
          }
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
          if (!isSuccess(stat)) {
            api.shutdown()
            return
          }

          if (stat.player == null) Utils.error(s"Invalid player: $name")
          else {
            if (args.length == 1) stat.saveStats()
            else stat.saveStats(args(1))
            stat.printStats()
          }
        }

        // Check afterwards to not slow the actual command down.
        if (api.getKey.get().getRecord.getQueriesInPastMin > 100) {
          Utils.error("API query limit exceeded. Please wait a minute before trying again!", prefix = true)
        }

        api.shutdown()
      }
    })
  }

  /**
   * Just gives a error if a player reply API request is not successful.
   *
   * @param stat Stats check request
   */
  private def isSuccess(stat: Stats): Boolean = {
    if (stat.reply.isSuccess) true
    else {
      Utils.error(s"Unexpected API error: ${stat.reply.getCause}", prefix = true)
      false
    }
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = true
}
