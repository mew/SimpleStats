package zone.nora.simplestats.util

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import com.google.gson.JsonObject
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

  def getRank(player: JsonObject): String = {
    if (player.has("prefix")) return player.get("prefix").getAsString

    val rank = if (player.has("rank")) player.get("rank").getAsString else null
    var playerRank = ""
    playerRank = if (rank != null && rank != "NORMAL") rank
    else if (player.has("newPackageRank")) player.get("newPackageRank").getAsString
    else if (player.has("packageRank")) player.get("packageRank").getAsString
    else null
    if (playerRank == "MVP_PLUS" && player.get("monthlyPackageRank").getAsString == "SUPERSTAR")
      playerRank = "MVP_PLUS_PLUS"
    if (rank == "NONE") playerRank = null
    val plusColour = if (player.has("rankPlusColor"))
      colourNameToCode(player.get("rankPlusColor").getAsString)
    val plusPlusColour = if (player.has("monthlyRankColor"))
      colourNameToCode(player.get("monthlyRankColor").getAsString)
    playerRank match {
      case "VIP" => "\u00a7a[VIP]"
      case "VIP_PLUS" => "\u00a7a[VIP\u00a76+\u00a7a]"
      case "MVP" => "\u00a7b[MVP]"
      case "MVP_PLUS" => s"\u00a7b[MVP$plusColour+\u00a7b]"
      case "MVP_PLUS_PLUS" => s"$plusPlusColour[MVP$plusColour++$plusPlusColour]"
      case "HELPER" => "\u00a79[HELPER]"
      case "MODERATOR" => "\u00a72[MOD]"
      case "ADMIN" => "\u00a7c[ADMIN]"
      case "YOUTUBER" => "\u00a7c[\u00a7fYOUTUBER\u00a7c]"
      case null => "\u00a77"
      case _ => "\u00a77"
    }
  }

  def colourNameToCode(colour: String): String = colour.toLowerCase() match {
    case "black" => "\u00a70"
    case "dark_green" => "\u00a72"
    case "dark_aqua" => "\u00a73"
    case "dark_red" => "\u00a74"
    case "dark_purple" => "\u00a75"
    case "gold" => "\u00a76"
    case "gray" => "\u00a77"
    case "dark_gray" => "\u00a78"
    case "blue" => "\u00a79"
    case "green" => "\u00a7a"
    case "aqua" => "\u00a7b"
    case "red" => "\u00a7c"
    case "light_purple" => "\u00a7d"
    case "yellow" => "\u00a7e"
    case "white" => "\u00a7f"
  }

  def parseTime(time: Long): String = try
    new SimpleDateFormat("dd/MM/yyyy").format(new Date(new Timestamp(time).getTime))
  catch { case _: Exception => "N/A" } // Hypixel Staff can hide their stats, which causes this function to freak out.

  def error(message: String): Unit = put(s"\u00a7c$message")

  def put(message: String): Unit = mc.thePlayer.addChatMessage(new ChatComponentText(s"$PREFIX$message"))

  def breakline(): Unit = {
    val dashes = new StringBuilder
    val dash = Math.floor((280 * mc.gameSettings.chatWidth + 40) / 320 * (1 / mc.gameSettings.chatScale) * 53).toInt
    for (_ <- 1 to dash) dashes.append("-")
    mc.thePlayer.addChatMessage(new ChatComponentText(s"\u00a79\u00a7m$dashes"))
  }
}