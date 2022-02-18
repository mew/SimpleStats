package zone.nora.simplestats.stats

import com.google.gson.JsonObject
import zone.nora.simplestats.dsl.getAsType
import zone.nora.simplestats.dsl.getFormattedPlayerName
import zone.nora.simplestats.dsl.withObject
import zone.nora.simplestats.util.Utils

class StatsBuilder(val player: JsonObject, val game: String? = null) {
    private val lines: ArrayList<String> = ArrayList()
    var activeObject: JsonObject = player
    private val gameObject: JsonObject? =
        if (game != null) player.getAsJsonObject("stats")?.getAsJsonObject(game) else null

    fun construct(build: StatsBuilder.(player: JsonObject, game: JsonObject?) -> Unit) = apply {
        build(player, gameObject)
    }

    inline operator fun <reified T> plus(stat: Stat<T>): Unit = plus(stat withObject activeObject)

    inline operator fun <reified T> plus(statWithJson: Pair<JsonObject, Stat<T>>) {
        val stat = statWithJson.second
        val statValue: T? = statWithJson.first.get(stat.key)?.getAsType()
        if (statValue == null) {
            addEmpty(stat.display)
        } else {
            addLine(stat.display to (stat.action?.invoke(stat, statValue) ?: statValue), stat.colour)
        }
    }

    fun firstLine() {
        val s = if (game == null) "Stats" else when (game) {
            "Arena" -> "Arena Brawl"
            "Battleground" -> "Warlords"
            "HungerGames" -> "Blitz Survival Games"
            "MCGO" -> "Pigs and Crims" // we do a little trolling
            "Quake" -> "Quakecraft"
            "TNTGames" -> "TNT Games"
            "Walls3" -> "Mega Walls"
            "GingerBread" -> "Turbo Kart Racers"
            "TrueCombat" -> "Crazy Walls"
            "SuperSmash" -> "Smash Heroes"
            "SpeedUHC" -> "Speed UHC"
            "Legacy" -> "Classic Lobby"
            "MurderMystery" -> "Murder Mystery"
            "BuildBattle" -> "Build Battle"
            else -> game
        } + " stats"
        lines.add("$s of ${player.getFormattedPlayerName()}")
    }

    fun addError(err: String) {
        lines += "\u00a7c$err"
    }

    fun addEmpty(display: String): Unit = addLine(display to "N/A", 'c')

    fun addLine(kvp: Pair<String, Any>, colour: Char? = null) {
        val c: Char = colour ?: when (kvp.second) {
            is Number -> 'e'
            is String -> '7'
            is Boolean -> if (kvp.second as Boolean) 'a' else 'c'
            else -> 'f'
        }
        lines += "${kvp.first}: \u00a7$c${kvp.second}"
    }

    fun send() {
        Utils.breakline()
        lines.forEach {
            Utils.put(it, false)
        }
        Utils.breakline()
    }
}