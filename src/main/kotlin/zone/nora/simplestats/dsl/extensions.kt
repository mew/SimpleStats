package zone.nora.simplestats.dsl

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.stats.Stat
import zone.nora.simplestats.util.HypixelConstants
import java.util.*

infix fun <T> Stat<T>.withObject(obj: JsonObject): Pair<JsonObject, Stat<T>> = obj to this

inline fun <reified T> JsonElement.getAsType(): T = SimpleStats.gson.fromJson(this, T::class.java)

fun JsonObject.getFormattedPlayerName(): String {
    val prefix = get("prefix")?.asString
    if (prefix != null) {
        return prefix
    }

    val rank = get("rank")?.asString
    var rank2 = if (rank != null && rank != "NORMAL") rank else get("newPackageRank")?.asString ?: get("packageRank")?.asString

    if (rank2 == "MVP_PLUS" && get("monthlyPackageRank")?.asString == "SUPERSTAR") {
        rank2 = "SUPERSTAR"
    }
    if (rank == "NONE") {
        rank2 = ""
    }

    val colours = arrayOf("rankPlusColor" to 'c', "monthlyRankColor" to '6').map {
        val c = get(it.first)?.asString?.lowercase()
        if (c != null) HypixelConstants.COLOUR_NAME_TO_CODE[c] else "\u00a7${it.second}"
    }

    val playerRank = if (rank2.isNullOrBlank()) "\u00a77" else when (rank2) {
        "VIP" -> "\u00a7a[VIP] "
        "VIP_PLUS" -> "\u00a7a[VIP\u00a76+\u00a7a] "
        "MVP" -> "\u00a7b[MVP] "
        "MVP_PLUS" -> "\u00a7b[MVP${colours[0]}+\u00a7b] "
        "SUPERSTAR" -> "${colours[1]}[MVP${colours[0]}++${colours[1]}] "
        "HELPER" -> "\u00a79[HELPER] "
        "MODERATOR" -> "\u00a72[MOD] "
        "ADMIN" -> "\u00a7c[ADMIN] "
        "YOUTUBER" -> "\u00a7c[\u00a7fYOUTUBE\u00a7c] "
        "GAME_MASTER" -> "\u00a72[GM] "
        else -> "\u00a77"
    }
    return "$playerRank${get("displayname")?.asString ?: "???"}"
}

// this sucks: todo make better
fun String.toUUID(): UUID? = try {
    var s = ""
    arrayOf(0 to 7, 8 to 11, 12 to 15, 16 to 19, 20 to 31).forEachIndexed { index, pair ->
        s += substring(pair.first..pair.second)
        if (index != 4) s += '-'
    }
    UUID.fromString(s)
} catch (e: Exception) {
    e.printStackTrace()
    null
}
