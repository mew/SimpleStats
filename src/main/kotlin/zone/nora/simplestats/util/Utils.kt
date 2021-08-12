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