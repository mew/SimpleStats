package zone.nora.simplestats.util

import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

object Utils {
    private const val PREFIX: String = "\u00a79[\u00a76SIMPLE\u00a79] \u00a7f"
    private val mc: Minecraft = Minecraft.getMinecraft()

    fun parseTimestamp(timestamp: Long): String = try {
        SimpleDateFormat("dd/MM/yyyy").format(Date(Timestamp(timestamp).time))
    } catch (_: Exception) {
        "\u00a7cN/A"
    }

    fun getSkyWarsLevel(exp: Long): Int = try {
        val exps = arrayOf(0, 20, 70, 150, 250, 500, 1000, 2000, 3500, 6000, 10000, 15000)
        if (exp >= 15000) ((exp - 15000) / 10000 + 12).toInt() else {
            for (i in exps.indices) {
                if (exp < exps[i]) {
                    1 + i + (exp - exps[i - 1]) / (exps[i] - exps[i - 1])
                }
            }
            0
        }
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }

    fun getGame(s: String): String? = when (s.lowercase()) {
        "hg", "bsg", "blitz", "sg", "hungergames" -> "HungerGames"
        "tnt", "tntgames" -> "TNTGames"
        "walls", "w", "walls2" -> "Walls"
        "bg", "warlords", "wl", "battleground", "warlord" -> "Battleground"
        "q", "quake", "qc", "quakecraft" -> "Quake"
        "a", "arc", "arcade" -> "Arcade"
        "v", "vz", "vampz", "vamp", "vampirez" -> "VampireZ"
        "gb", "tkr", "turbokartracers", "gingerbread" -> "GingerBread"
        "sw", "skywars" -> "SkyWars"
        "w3", "mw", "m", "walls3", "mega", "megawalls" -> "Walls3"
        "u", "uhc", "hardcore", "ultra", "champions" -> "UHC"
        "mcgo", "csgo", "cvc", "cac", "copsandcrims", "pigsandcrims" -> "MCGO"
        "tc", "cw", "crazywalls", "crazy", "truecombat", "combat" -> "TrueCombat"
        "sh", "smash", "smashheroes", "smashheros", "heroes", "supersmash" -> "SuperSmash"
        "pb", "paintball" -> "Paintball"
        "arena", "ab", "arenabrawl" -> "Arena"
        "suhc", "speeduhc", "su" -> "SpeedUHC"
        "sc", "skyclash", "cx" /* kevos did nothing wrong */ -> "SkyClash"
        "classic", "legacy" -> "Legacy"
        "b", "bw", "bed", "bedwars" -> "Bedwars"
        "mm", "murder", "mystery", "murdermystery" -> "MurderMystery"
        "bb", "build", "buildbattle", "gtb" -> "BuildBattle"
        "duels", "d", "1v1", "pot" -> "Duels"
        "p", "pit", "thepit" -> "Pit"
        "sb", "skyblock" -> "SkyBlock" // todo skyblock stats
        "h", "housing", "house" -> "Housing"
        else -> null
    }

    fun breakline() {
        val dashes = StringBuilder()
        val dash = floor((280 * mc.gameSettings.chatWidth + 40) / 320 * (1 / mc.gameSettings.chatScale) * 53).toInt() - 6
        for (i in 1..dash) {
            dashes.append(if (i == dash shr 1) "\u00a79[\u00a76SIMPLE\u00a79]\u00a7m" else "-")
        }
        put("\u00a79\u00a7m$dashes", false)
    }

    fun put(msg: String, prefix: Boolean = true): Unit =
        mc.thePlayer.addChatMessage(ChatComponentText(if (prefix) "$PREFIX$msg" else msg))

    fun err(msg: String, prefix: Boolean = true): Unit = put("\u00a7c$msg", prefix)
}