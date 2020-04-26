package zone.nora.simplestats.util

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import net.hypixel.api.HypixelAPI
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText

object Utils {
  private final val PREFIX = "\u00a79[\u00a76SS\u00a79] \u00a7f"
  private val mc = Minecraft.getMinecraft

  def validateKey(apiKey: String): Boolean = try {
    new HypixelAPI(UUID.fromString(apiKey)).getKey.get().isSuccess
  } catch {
    case _: Exception => false
  }

  def parseTime(time: Long): String = try
    new SimpleDateFormat("dd/MM/yyyy").format(new Date(new Timestamp(time).getTime))
  catch { case _: Exception => "N/A" } // Hypixel Staff can hide their stats, which causes this function to freak out.

  def error(message: String): Unit = put(s"\u00a7c$message")

  def put(message: String): Unit = mc.thePlayer.addChatMessage(new ChatComponentText(s"$PREFIX$message"))

  def breakline(): Unit = {
    val dashes = new StringBuilder
    val numberOfDashes = Math.floor((280 * mc.gameSettings.chatWidth + 40) / 320 * (1 / mc.gameSettings.chatScale) * 53).toInt
    for (_ <- 1 to numberOfDashes) dashes.append("-")
    mc.thePlayer.addChatMessage(new ChatComponentText(s"\u00a79\u00a7m$dashes"))
  }
}