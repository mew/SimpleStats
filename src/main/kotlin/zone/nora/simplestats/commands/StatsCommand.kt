package zone.nora.simplestats.commands

import gg.essential.api.utils.Multithreading
import net.hypixel.api.HypixelAPI
import net.hypixel.api.util.ILeveling
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.config.Config
import zone.nora.simplestats.dsl.firstCharUpper
import zone.nora.simplestats.dsl.toUUID
import zone.nora.simplestats.dsl.withObject
import zone.nora.simplestats.stats.Stat
import zone.nora.simplestats.stats.StatsBuilder
import zone.nora.simplestats.util.Utils
import java.math.BigInteger
import java.util.*

@Suppress("DEPRECATION")
object StatsCommand : CommandBase() {
    private fun stats(playerName: String, gameDbName: String?) {
        val api = HypixelAPI(Config.hypixelKey)
        try {
            val uuid = SimpleStats.uuidMap[playerName]
            val b = uuid == null
            val playerReply = (if (b) api.getPlayerByName(playerName) else api.getPlayerByUuid(uuid)).get()
            if (!playerReply.isSuccess) {
                Utils.err(playerReply.cause ?: "Unknown error.")
            } else {
                val reply = playerReply.player
                if (reply == null) {
                    Utils.err("API returned empty body. Is this a real player?")
                    SimpleStats.uuidMap[playerName] = UUID.randomUUID()
                } else {
                    if (gameDbName == null) {
                        StatsBuilder(reply).construct { player, _ ->
                            firstLine()
                            this + Stat<Double>("networkExp", "Network Level") {
                                "%.2f".format(ILeveling.getLevel(it) + ILeveling.getPercentageToNextLevel(it)).toFloat()
                            }
                            this + Stat<Int>("achievementPoints", "Achievement Points")
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
                    } else {
                        StatsBuilder(reply, gameDbName).construct { player, game ->
                            firstLine()
                            activeObject = game ?: return@construct // this better not ever happen
                            when (gameDbName) {
                                "Arcade" -> {
                                    this + Stat<Int>("coins")
                                }
                                "Arena" -> {
                                    fun arenaModesStats(stat: String) {
                                        val statUpper = stat.firstCharUpper()
                                        var totalStat = 0
                                        arrayOf("1v1", "2v2", "4v4").forEach { mode ->
                                            this + Stat<Int>("${stat}_$mode", "$mode $statUpper") {
                                                totalStat += it
                                                it
                                            }
                                        }
                                        addLine("Total $statUpper" to totalStat)
                                    }
                                    arenaModesStats("wins")
                                    arenaModesStats("losses")
                                    this + Stat<Int>("coins")
                                    arrayOf("offensive", "utility", "support", "ultimate").forEach { skill ->
                                        this + Stat<String>(skill, "${skill.firstCharUpper()} Skill") {
                                            it.replace('_', ' ')
                                        }
                                    }
                                }
                                "Battleground" -> {
                                    arrayOf("kills", "assists", "deaths", "wins", "losses").forEach {
                                        this + Stat<Int>(it)
                                    }
                                    arrayOf(
                                        "win_streak" to "Winstreak",
                                        "damage" to "Damage Dealt",
                                        "damage_taken" to "Damage Taken",
                                        "coins" to "Coins"
                                    ).forEach {
                                        this + Stat<Int>(it.first, it.second)
                                    }
                                    val classes = arrayOf("mage", "paladin", "shaman", "warrior")
                                    val upgrades = arrayOf(
                                        "cooldown",
                                        "critchance",
                                        "critmultiplier",
                                        "energy",
                                        "health",
                                        "skill1",
                                        "skill2",
                                        "skill3",
                                        "skill4",
                                        "skill5"
                                    )
                                    classes.forEach { wlClass ->
                                        var total = 0
                                        upgrades.forEach { upgrade ->
                                            total += game["${wlClass}_$upgrade"]?.asInt ?: 0
                                        }
                                        addLine("${wlClass.firstCharUpper()} Level" to total)
                                    }
                                }
                                "Bedwars" -> {
                                    val lvl = player.get("achievements")?.asJsonObject?.get("bedwars_level")?.asInt ?: 1
                                    val symbol = if (lvl in 1000..1999) '\u272a' else if (lvl > 1999) '\u269d' else '\u272b'
                                    val cl = lvl.toString()
                                    fun cLvl(c: String): String = "${c[0]}${cl[0]}${c[1]}${cl[1]}${c[2]}${cl[2]}${c[3]}${cl[3]}${c[4]}"
                                    val s = when (lvl) {
                                        in (-1)..99 -> "7$lvl"
                                        in 100..199 -> "f$lvl"
                                        in 200..299 -> "6$lvl"
                                        in 300..399 -> "b$lvl"
                                        in 400..499 -> "2$lvl"
                                        in 500..599 -> "3$lvl"
                                        in 600..699 -> "4$lvl"
                                        in 700..799 -> "d$lvl"
                                        in 800..899 -> "9$lvl"
                                        in 900..999 -> "5$lvl"
                                        in 1000..1099 -> cLvl("6eabd")
                                        in 1100..1199 -> "f$lvl\u00a77"
                                        in 1200..1299 -> "e$lvl\u00a76"
                                        in 1300..1399 -> "b$lvl\u00a73"
                                        in 1400..1499 -> "a$lvl\u00a72"
                                        in 1500..1599 -> "3$lvl\u00a79"
                                        in 1600..1699 -> "c$lvl\u00a74"
                                        in 1700..1799 -> "d$lvl\u00a75"
                                        in 1800..1899 -> "9$lvl\u00a71"
                                        in 1900..1999 -> "5$lvl\u00a78"
                                        in 2000..2099 -> cLvl("7ff77")
                                        in 2100..2199 -> cLvl("fee66")
                                        in 2200..2299 -> cLvl("6ffbb")
                                        in 2300..2399 -> cLvl("5dd6e")
                                        in 2400..2499 -> cLvl("bff77")
                                        in 2500..2599 -> cLvl("faa22")
                                        in 2600..2699 -> cLvl("4ccdd")
                                        in 2700..2799 -> cLvl("eff88")
                                        in 2800..2899 -> cLvl("a2266")
                                        in 2900..2999 -> cLvl("b3399")
                                        in 3000..10000 -> cLvl("e66cc")
                                        else -> "$lvl"
                                    }
                                    addLine("BedWars Level" to "\u00a7$s$symbol")
                                }
                            }
                        }.send()
                    }
                    if (b) {
                        val u = reply["uuid"].asString.toUUID()
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

    override fun getCommandName(): String = "stats"

    override fun getCommandUsage(sender: ICommandSender): String = "/stats"

    override fun processCommand(sender: ICommandSender, args: Array<out String>): Unit = Multithreading.runAsync {
        when (args.size) {
            1 -> stats(args[0].lowercase(), null)
            2 -> stats(args[0].lowercase(), args[1])
            else -> Utils.err("/stats <player> [game]")
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