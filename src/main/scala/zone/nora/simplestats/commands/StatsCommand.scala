package zone.nora.simplestats.commands

import java.util
import java.util.concurrent.{ExecutorService, Executors}
import java.util.regex.Pattern

import net.hypixel.api.HypixelAPI
import net.minecraft.client.Minecraft
import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.event.ClickEvent
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.core.{Stats, StatsManager}
import zone.nora.simplestats.util.{ChatComponentBuilder, PlayerInfo, Utils}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class StatsCommand extends CommandBase {

  private final val COMMAND_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()
  private final val PATTERN: Pattern = Pattern.compile("^[a-zA-Z0-9_]*$")

  override def getCommandName: String = "stats"

  override def getCommandAliases: util.List[String] = ("hstats" :: Nil).asJava

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    COMMAND_EXECUTOR.execute(new Runnable {
      override def run(): Unit = {
        if (args == null) return
        else if (args.isEmpty) {
          Utils.error(getCommandUsage(sender), prefix = true)
          return
        } else if (!SimpleStats.valid) {
          Utils.error("Invalid Hypixel API key! Do /setkey <apikey>", prefix = true)
          return
        }

        val api = new HypixelAPI(SimpleStats.key)

        if (args(0).equals("*")) { // Server mode
          val listBuffer: ListBuffer[String] = new ListBuffer[String]
          for (player <- PlayerInfo.getPlayers(24)) {
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
        } else if (args(0).equals("#")) { // API key statistics
          val keyStats = api.getKey.get().getRecord
          Utils.put(s"Total queries: ${keyStats.getTotalQueries}")
          Utils.put(s"Queries in last minute: ${keyStats.getQueriesInPastMin}")
        } else { // Single player mode
          val name: String = if (args(0).contains(".")) Minecraft.getMinecraft.thePlayer.getName
          else args(0).replaceAll(":", "")

          if (!PATTERN.matcher(name).matches()) {
            Utils.error("Illegal characters in string!", prefix = true)
            api.shutdown()
            return
          }

          val stat = new Stats(api, name, compact = args(0).contains(":"))
          if (!isSuccess(stat)) {
            api.shutdown()
            return
          }

          if (stat.player == null) Utils.error(s"Invalid player: $name")
          else {
            if (args.length == 1) stat.saveStats()
            else if (args(1) == "sb" || args(1) == "skyblock") {
              if (stat.player.has("stats")) {
                val skyblock = new StatsManager(stat.player, "SkyBlock")
                if (skyblock == null || !stat.player.get("stats").getAsJsonObject.has("SkyBlock")) {
                  api.shutdown()
                  Utils.error(s"${stat.player.get("playername").getAsString} has no SkyBlock stats.", prefix = true)
                  return
                }
                Utils.breakLine()
                val profiles = skyblock.stats.get("profiles").getAsJsonObject
                Utils.put("Click the profile name to view it's stats:")
                profiles.entrySet().foreach { it =>
                  val profile = it.getValue.getAsJsonObject
                  val cuteName = profile.get("cute_name").getAsString
                  val id = it.getKey
                  Minecraft.getMinecraft.thePlayer.addChatMessage(
                    ChatComponentBuilder.of(s"  \u00a78\u27a4 \u00a7a$cuteName")
                      .setClickEvent(ClickEvent.Action.RUN_COMMAND, "/$skyblock_stats " + s"$id ${stat.player.get("uuid").getAsString}")
                      .setHoverEvent(s"Click to view $cuteName (id: $id).")
                      .build()
                  )
                }
                Utils.breakLine()
              } else {
                stat.firstLine(stat.player, "SkyBlock")
                stat.lines.append("\u00a7cNo stats found.")
              }
            } else stat.saveStats(args(1))
            stat.printStats()
          }
        }

        api.shutdown()
      }
    })
  }

  override def getCommandUsage(sender: ICommandSender): String = "/stats [player] [game]"

  /**
   * Just gives a error if a player reply API request is not successful.
   *
   * @param stat Stats check request
   */
  private def isSuccess(stat: Stats): Boolean = try {
    if (stat.reply.isSuccess) return true
    else Utils.error(s"Unexpected API error: ${stat.reply.getCause}", prefix = true)
    false
  } catch {
    case _: Exception => false
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = true
}
