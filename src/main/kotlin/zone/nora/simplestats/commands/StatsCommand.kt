package zone.nora.simplestats.commands

import gg.essential.api.utils.Multithreading
import net.hypixel.api.HypixelAPI
import net.hypixel.api.util.ILeveling
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.config.Config
import zone.nora.simplestats.dsl.toUUID
import zone.nora.simplestats.dsl.withObject
import zone.nora.simplestats.stats.Stat
import zone.nora.simplestats.stats.StatsBuilder
import zone.nora.simplestats.util.Utils
import java.math.BigInteger
import java.util.*

@Suppress("DEPRECATION")
object StatsCommand : CommandBase() {
    override fun getCommandName(): String = "stats"

    override fun getCommandUsage(sender: ICommandSender): String = "/stats"

    override fun processCommand(sender: ICommandSender, args: Array<out String>): Unit = Multithreading.runAsync {
        when (args.size) {
            0 -> Utils.err("/stats <player> [game]")
            1 -> {
                val playerName = args[0].lowercase()
                val api = HypixelAPI(Config.hypixelKey)
                try {
                    val uuid = SimpleStats.uuidMap[playerName]
                    val b = uuid == null
                    println(uuid)
                    val playerReply = (if (b) api.getPlayerByName(playerName) else api.getPlayerByUuid(uuid)).get()
                    if (!playerReply.isSuccess) {
                        Utils.err(playerReply.cause ?: "Unknown error.")
                    } else {
                        val reply = playerReply.player
                        if (reply == null) {
                            Utils.err("API returned empty body. Is this a real player?")
                            SimpleStats.uuidMap[playerName] = UUID.randomUUID()
                        } else {
                            StatsBuilder(reply).construct { player, _ ->
                                firstLine()
                                this + Stat<Double>("networkExp", "Network Level") {
                                    "%.2f".format(ILeveling.getLevel(it) + ILeveling.getPercentageToNextLevel(it)).toFloat()
                                }
                                this + Stat<Int>("achievementPoints", "Achievement Points")
                                // todo quests
                                val quests = player["quests"]?.asJsonObject
                                val quests2 = quests?.entrySet()?.sumOf {
                                    it.value?.asJsonObject?.get("completions")?.asJsonArray?.size() ?: 0
                                } ?: 0
                                addLine("Quests Completed" to quests2)

                                this + Stat<Int>("karma")
                                val socials = player["socialMedia"]?.asJsonObject?.get("links")?.asJsonObject
                                if (socials != null) {
                                    this + (Stat<String>("DISCORD", "Discord", '9') withObject socials)
                                }
                                this + Stat<Long>("lastLogin", "Last Login") {
                                    val lastLogout = player["lastLogout"]?.asLong ?: 0L
                                    addLine("Online" to (it > lastLogout && lastLogout != 0L))
                                    this@construct + Stat<String>("_id", "First Login") { mongoId ->
                                        val firstLogin = player["firstLogin"]?.asLong ?: 0
                                        val x = BigInteger(mongoId.substring(0..7), 16).longValueExact() * 1000
                                        Utils.parseTimestamp(if (firstLogin < x) firstLogin else x)
                                    }
                                    Utils.parseTimestamp(it)
                                }
                            }.send()
                            if (b) {
                                val u = reply["uuid"].asString.toUUID()
                                println(u)
                                if (u != null) {
                                    SimpleStats.uuidMap[playerName] = u
                                }
                            }
                        }
                    }
                    api.shutdown()
                } catch (ex: Exception) {
                    Utils.err("${ex.message ?: "Unknown error"}. See logs for more details.")
                    api.shutdown()
                    ex.printStackTrace()
                }
            }
        }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<out String>,
        pos: BlockPos
    ): MutableList<String>? = null // todo

    override fun isUsernameIndex(args: Array<out String>, index: Int): Boolean = true
}