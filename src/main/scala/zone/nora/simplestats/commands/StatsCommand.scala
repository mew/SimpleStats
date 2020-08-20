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
        } else if (!SimpleStats.valid) {
          Utils.error("Invalid Hypixel API key!", prefix = true)
          return
        }

        val api = new HypixelAPI(SimpleStats.key)

        if (args(0).charAt(0).equals('*')) { // Server mode
          val players: util.List[String] = new util.ArrayList[String]()
          Minecraft.getMinecraft.getNetHandler.getPlayerInfoMap.foreach { playerInfo =>
            players.add(playerInfo.getGameProfile.getName)
            if (players.size() >= 24) return
          }
          
          SimpleStats.logger.info(s"Stat check queue is $players")	
          
          val listBuffer: ListBuffer[String] = new ListBuffer[String]
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
        } else if (args(0).charAt(0).equals('#')) { // API key statistics
          val keyStats = api.getKey.get().getRecord
          Utils.breakLine()	
          Utils.put(s"Total queries: ${keyStats.getTotalQueries}")	
          Utils.put(s"Queries in last minute: ${keyStats.getQueriesInPastMin}")	
          Utils.breakLine()
        } else { // Single player mode
          val name: String = if (args(0).contains(".")) Minecraft.getMinecraft.thePlayer.getName
          else args(0).replaceAll(":", "")
          
          val stat = new Stats(api, name, compact = args(0).contains(":"))
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
    if (stat.reply.isSuccess) return true
    else Utils.error(s"Unexpected API error: ${stat.reply.getCause}", prefix = true)
    false
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = true
}
