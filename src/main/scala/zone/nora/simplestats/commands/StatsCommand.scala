package zone.nora.simplestats.commands

import java.util
import java.util.UUID

import net.hypixel.api.HypixelAPI
import net.minecraft.client.Minecraft
import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.entity.player.EntityPlayer
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

        val api = new HypixelAPI(UUID.fromString(SimpleStats.key))
        if (!api.getKey.get().isSuccess) {
          Utils.error("Invalid Hypixel API key. Use /setkey <key>", prefix = true)
          return
        }

        val compactMode = args(0).charAt(0).equals(':') // Putting this before a name activates compact mode.
        val serverMode = args(0).charAt(0).equals('*') // Prints stats of everyone on the server in compact mode.

        if (serverMode) {
          val playerEntities: util.List[EntityPlayer] = Minecraft.getMinecraft.theWorld.playerEntities
          var i = 0
          for (w <- 0 to playerEntities.size()) { // can use foreach here
            Thread.sleep(250)
            val name = playerEntities.get(w).getName
            val stat = new Stats(api, name, oneLine = true)

            if (isRequestSuccessful(stat)) {
              if (args.length == 1) stat.printStats()
              else stat.printStats(args(1))
            }
          }
        } else {
          val stat = new Stats(api, args(0).replace(":", ""), oneLine = compactMode)
          if (isRequestSuccessful(stat)) {
            if (args.length == 1) stat.printStats()
            else stat.printStats(args(1))
          }
        }

        api.shutdown()
      }
    })
    thread.start()
  }

  private def isRequestSuccessful(stat: Stats): Boolean = {
    if (!stat.reply.isSuccess) {
      Utils.error(s"Unexpected API error: ${stat.reply.getCause}", prefix = true)
      return false
    } else if (stat.player == null) {
      Utils.error("Invalid player.", prefix = true)
      return false
    }

    true
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = true
}
