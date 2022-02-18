package zone.nora.simplestats.commands

import gg.essential.api.utils.Multithreading
import gg.essential.universal.UMinecraft
import net.hypixel.api.HypixelAPI
import net.hypixel.api.util.GameType
import net.hypixel.api.util.ILeveling
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.config.Config
import zone.nora.simplestats.dsl.firstCharUpper
import zone.nora.simplestats.dsl.lowerSnakeToUpper
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
                    Utils.err("API returned empty body. Is this a real player [$playerName]?")
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
                            if (game == null) {
                                addError("No stats found.")
                                return@construct
                            }
                            activeObject = game
//                            activeObject = game ?: return@construct
                            when (gameDbName) {
                                "Arcade" -> {
                                    this + Stat.COINS
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
                                    this + Stat.COINS
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
                                        this + Stat<Long>(it.first, it.second)
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
                                            total += game.get("${wlClass}_$upgrade")?.asInt ?: 0
                                        }
                                        addLine("${wlClass.firstCharUpper()} Level" to total)
                                    }
                                }
                                "Bedwars" -> {
                                    val lvl = player.get("achievements")?.asJsonObject?.get("bedwars_level")?.asInt ?: 1
                                    val symbol = if (lvl in 1000..1999) '\u272a' else if (lvl > 1999) '\u269d' else '\u272b'
                                    val cl = lvl.toString()
                                    fun cLvl(c: String): String =
                                        "${c[0]}${cl[0]}\u00a7${c[1]}${cl[1]}\u00a7${c[2]}${cl[2]}\u00a7${c[3]}${cl[3]}\u00a7${c[4]}"
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
                                    addLine("Level" to "\u00a7$s$symbol")
                                    this + Stat<Int>("kills_bedwars", "Kills")
                                    this + Stat<Int>("deaths_bedwars", "Deaths")
                                    this + Stat<Int>("final_kills_bedwars", "Final Kills")
                                    this + Stat<Int>("final_deaths_bedwars", "Final Deaths")
                                    this + Stat<Int>("wins_bedwars", "Wins")
                                    this + Stat<Int>("coins")
                                    this + Stat<Int>("winstreak")
                                }
                                "BuildBattle" -> {
                                    this + Stat<Int>("wins")
                                    this + Stat<Int>("score", "Title") {
                                        addLine("Score" to it)
                                        "\u00a7" + when (it) {
                                            in 0..99 -> "fRookie"
                                            in 100..249 -> "7Untrained"
                                            in 250..499 -> "eAmateur"
                                            in 500..999 -> "aApprentice"
                                            in 1000..1999 -> "dExperienced"
                                            in 2000..3499 -> "9Seasoned"
                                            in 3500..4999 -> "2Trained"
                                            in 5000..7499 -> "3Skilled"
                                            in 7500..9999 -> "cTalented"
                                            in 10000..14999 -> "5Professional"
                                            in 15000..19999 -> "1Expert"
                                            else -> "4Master"
                                        }
                                    }
                                    this + Stat<Int>("coins")
                                    this + Stat<Int>("games_played", "Games Played")
                                    this + Stat<Int>("correct_guesses", "Correct Guessed (GTB)")
                                }
                                "Duels" -> {
                                    // todo division
                                    this + Stat<Int>("wins")
                                    this + Stat<Int>("losses")
                                    this + Stat<Int>("current_winstreak", "Winstreak")
                                    this + Stat<Int>("best_overall_winstreak", "Best Winstreak")
                                    this + Stat<Int>("coins")
                                }
                                "GingerBread" -> {
                                    arrayOf("gold", "silver", "bronze").forEach {
                                        this + Stat<Int>("${it}_trophy", "${it.firstCharUpper()} Trophies")
                                    }
                                    this + Stat<Int>("coins")
                                }
                                "Housing" -> {
                                    addError("what did you even expect to be here.")
                                }
                                "HungerGames" -> {
                                    this + Stat<Int>("kills")
                                    this + Stat<Int>("deaths")
                                    this + Stat<Int>("wins_solo_normal", "Wins") {
                                        it + (game["wins_teams_normal"]?.asInt ?: 0)
                                    }
                                    this + Stat<Int>("coins")
                                    this + Stat<String>("defaultkit", "Default Kit")
                                }
                                "Legacy" -> {
                                    this + Stat<Int>("tokens")
                                    this + Stat<Int>("total_tokens", "Total Tokens")
                                }
                                "MCGO" -> {
                                    arrayOf("Defusal" to "", "Deathmatch" to "_deathmatch").forEachIndexed { i, mode ->
                                        this + Stat<Int>("kills${mode.second}", "${mode.first} Kills")
                                        this + Stat<Int>("deaths${mode.second}", "${mode.first} Deaths")
                                        if (i == 0) {
                                            this + Stat<Int>("round_wins", "Defusal Round Wins")
                                            this + Stat<Int>("game_wins", "Defusal Game Wins")
                                        } else {
                                            this + Stat<Int>("game_wins_deathmatch", "Deathmatch Wins")
                                        }
                                    }
                                    this + Stat.COINS
                                }
                                "MurderMystery" -> {
                                    this + Stat<Int>("murderer_wins")
                                    this + Stat<Int>("detective_wins")
                                    arrayOf("assassins", "infection").forEach {
                                        this + Stat<Int>("wins_MURDER_${it.uppercase()}", "${it.firstCharUpper()} Wins")
                                    }
                                    arrayOf("murderer", "detective").forEach { this + Stat<Int>("${it}_chance") { p -> "$p%" } }
                                    this + Stat<Int>("kills_as_murderer")
                                    this + Stat.COINS
                                }
                                "Paintball" -> {
                                    arrayOf("kills", "deaths", "wins", "shots_fired").forEach { this + Stat<Int>(it) }
                                    this + Stat<String>("hat") {
                                        it.lowerSnakeToUpper()
                                    }
                                    this + Stat.COINS
                                }
                                "Pit" -> {
                                    val pitProfile = game["profile"]?.asJsonObject
                                    val pitStatsPtl = game["pit_stats_ptl"]?.asJsonObject
                                    if (pitProfile == null || pitStatsPtl == null) {
                                        addError("Player has missing Pit stats.")
                                        return@construct
                                    }
                                    activeObject = pitStatsPtl
                                    this + Stat<Int>("kills")
                                    this + Stat<Int>("assists")
                                    this + Stat<Int>("deaths")
                                    activeObject = pitProfile
                                    this + Stat<Int>("cash", "Gold")
                                    this + Stat<Int>("renown")
                                    activeObject = pitStatsPtl
                                    this + Stat<Int>("max_streak", "Highest Killstreak")
                                    this + Stat<Long>("damage_dealt")
                                    this + Stat<Long>("damage_received")
                                    activeObject = pitProfile
                                    if (pitProfile.has("genesis_allegiance")) {
                                        this + Stat<String>("genesis_allegiance") {
                                            if (it == "ANGEL") "\u00a7bAngel" else "\u00a7cDemon"
                                        }
                                    }
                                    addLine("Prestige" to (if (pitProfile.has("prestiges")) pitProfile["prestiges"].asJsonArray.size() else 0))
                                    addLine("Pit Supporter" to (game["packages"]?.asJsonArray?.any {
                                        try {
                                            it.asString == "supporter"
                                        } catch (_: UnsupportedOperationException) {
                                            false
                                        }
                                    } ?: false))
                                }
                                "Quake" -> {
                                    this + Stat<Int>("kills", "Solo Kills")
                                    this + Stat<Int>("wins", "Solo Wins")
                                    this + Stat<Int>("kills_teams", "Teams Kills")
                                    this + Stat<Int>("wins_teams", "Teams Wins")
                                    this + Stat.COINS
                                    this + Stat<String>("trigger") {
                                        when (it) {
                                            "ONE_POINT_FIVE" -> "1.5s"
                                            "ONE_POINT_FOUR" -> "1.4s"
                                            "ONE_POINT_THREE" -> "1.3s"
                                            "ONE_POINT_TWO" -> "1.2s"
                                            "ONE_POINT_ONE" -> "1.1s"
                                            "ONE_POINT_ZERO", "ONE" -> "1s"
                                            "ZERO_POINT_NINE" -> "0.9s"
                                            "ZERO_POINT_EIGHT_FIVE" -> "0.85s"
                                            else -> it
                                        }
                                    }
                                }
                                "SkyClash" -> {
                                    arrayOf("kills", "deaths", "wins", "coins").forEach { this + Stat<Int>(it) }
                                    this + Stat<Int>("win_streak", "Winstreak")
                                    this + Stat<Int>("card_packs")
                                }
                                "SkyWars" -> {
                                    if (game.has("levelFormatted")) {
                                        this + Stat<String>("levelFormatted", "SkyWars Level")
                                    } else {
                                        this + Stat<Long>("skywars_experience", "SkyWars Level") {
                                            Utils.getSkyWarsLevel(it)
                                        }
                                    }
                                    arrayOf("kills", "deaths", "wins", "coins").forEach { this + Stat<Int>(it) }
                                    this + Stat<Int>("win_streak", "Winstreak")
                                    this + Stat<Int>("souls")
                                    this + Stat<Int>("heads")
                                    this + Stat<Int>("shard", "Shards") { "$it/20000" }
                                    this + Stat<Int>("opals")
                                }
                                "SpeedUHC" -> arrayOf("kills", "deaths", "wins", "coins", "winstreak").forEach {
                                    this + Stat<Int>(it)
                                }
                                "SuperSmash" -> {
                                    arrayOf("kills", "deaths", "wins", "coins").forEach { this + Stat<Int>(it) }
                                    this + Stat<Int>("smashLevel", "Smash Level", '6') { "$it\u272b" }
                                    val activeClass = game["active_class"]?.asString ?: return@construct
                                    val prestige = game["pg_$activeClass"]?.asInt ?: 0
                                    val level = game["lastLevel_$activeClass"]?.asInt ?: 0
                                    addLine("Active Class" to "$activeClass (P$prestige Lv$level)")
                                }
                                "TNTGames" -> {
                                    arrayOf(
                                        "tntrun" to "TNT Run",
                                        "pvprun" to "PVP Run",
                                        "bowspleef" to "TNT Bowspleef",
                                        "capture" to "TNT Wizards",
                                        "tnttag" to "TNT Tag"
                                    ).forEach { this + Stat<Int>("wins_${it.first}", "${it.second} Wins") }
                                    this + Stat.COINS
                                }
                                "TrueCombat" -> {
                                    arrayOf("kills", "deaths", "wins", "coins", "golden_skulls", "gold_dust").forEach {
                                        this + Stat<Int>(it)
                                    }
                                }
                                "UHC" -> {
                                    arrayOf("kills", "deaths", "wins", "coins", "score", "heads_eaten").forEach {
                                        this + Stat<Int>(it)
                                    }
                                }
                                "VampireZ" -> {
                                    arrayOf("human_wins", "vampire_wins", "zombie_kills", "coins").forEach {
                                        this + Stat<Int>(it)
                                    }
                                }
                                "Walls" -> {
                                    arrayOf("kills", "deaths", "wins", "coins").forEach {
                                        this + Stat<Int>(it)
                                    }
                                }
                                "Walls3" -> {
                                    this + Stat<Int>("kills")
                                    this + Stat<Int>("total_final_kills", "Finals")
                                    arrayOf("deaths", "final_deaths", "wins", "coins").forEach {
                                        this + Stat<Int>(it)
                                    }
                                    this + Stat<String>("chosen_class")
                                }
                                "SkyBlock" -> {
                                    addEmpty("todo lol") //todo skyblock
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
            2 -> {
                Utils.getGame(args[1]).run {
                    if (this == null) {
                        Utils.err("Invalid game :L")
                        return@runAsync
                    } else {
                        stats(args[0].lowercase(), this)
                    }
                }
            }
            else -> Utils.err("/stats <player> [game]")
        }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<out String>?,
        pos: BlockPos
    ): MutableList<String>? {
        if (args != null) {
            when (args.size) {
                1 -> {
                    val p = UMinecraft.getWorld()?.playerEntities ?: return null
                    val x = p.filter { it.gameProfile.id.version() == 4 }.map {
                        it.gameProfile.name
                    }
                    return getListOfStringsMatchingLastWord(args, x)
                }
                2 -> return getListOfStringsMatchingLastWord(args, GameType.values().map { it.dbName.lowercase() })
            }
        }
        return null
    }
}