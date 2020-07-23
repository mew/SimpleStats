package zone.nora.simplestats.util

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import com.google.gson.JsonObject
import net.hypixel.api.HypixelAPI
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import zone.nora.simplestats.SimpleStats

object Utils {

  final val PREFIX = "\u00a79[\u00a76SS\u00a79] \u00a7f"
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
    if (playerRank == "MVP_PLUS")
      if (player.has("monthlyPackageRank") && player.get("monthlyPackageRank").getAsString == "SUPERSTAR")
        playerRank = "MVP_PLUS_PLUS"
    if (rank == "NONE") playerRank = null
    val colourNameToCode = Map(
      "black" -> "\u00a70",
      "dark_green" -> "\u00a72",
      "dark_aqua" -> "\u00a73",
      "dark_red" -> "\u00a74",
      "dark_purple" -> "\u00a75",
      "gold" -> "\u00a76",
      "gray" -> "\u00a77",
      "dark_gray" -> "\u00a78",
      "blue" -> "\u00a79",
      "green" -> "\u00a7a",
      "aqua" -> "\u00a7b",
      "red" -> "\u00a7c",
      "light_purple" -> "\u00a7d",
      "yellow" -> "\u00a7e",
      "white" -> "\u00a7f"
    )

    val plusColour = if (player.has("rankPlusColor"))
      colourNameToCode(player.get("rankPlusColor").getAsString.toLowerCase) else "\u00a7c"

    val plusPlusColour = if (player.has("monthlyRankColor"))
      colourNameToCode(player.get("monthlyRankColor").getAsString.toLowerCase) else "\u00a76"

    playerRank match {
      case "VIP" => "\u00a7a[VIP]"
      case "VIP_PLUS" => "\u00a7a[VIP\u00a76+\u00a7a]"
      case "MVP" => "\u00a7b[MVP]"
      case "MVP_PLUS" => s"\u00a7b[MVP$plusColour+\u00a7b]"
      case "MVP_PLUS_PLUS" => s"$plusPlusColour[MVP$plusColour++$plusPlusColour]"
      case "HELPER" => "\u00a79[HELPER]"
      case "MODERATOR" => "\u00a72[MOD]"
      case "ADMIN" => "\u00a7c[ADMIN]"
      case "YOUTUBER" => "\u00a7c[\u00a7fYOUTUBE\u00a7c]"
      case null => "\u00a77"
      case _ => "\u00a77"
    }
  }

  def getWarlordsClassLevel(bg: JsonObject, wlClass: String): Int = {
    val list = List(
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
    var total = 0
    list.foreach { it =>
      val key = s"${wlClass}_$it"
      if (bg.has(key)) total += bg.get(key).getAsInt
    }
    total
  }

  def roundDouble(n: Double): Double = (math rint n * 100) / 100

  def parseTime(time: Long): String = try
    new SimpleDateFormat("dd/MM/yyyy").format(new Date(new Timestamp(time).getTime))
  catch {
    case _: Exception => "N/A"
  } // Hypixel Staff can hide their stats, which causes this function to freak out.

  def error(message: String, prefix: Boolean = false): Unit = put(s"\u00a7c$message", prefix)

  def put(message: String, prefix: Boolean = false): Unit =
    mc.thePlayer.addChatMessage(new ChatComponentText(s"${if (prefix) PREFIX else ""}$message"))

  def breakLine(): Unit = {
    val dashes = new StringBuilder
    val dash = Math.floor((280 * mc.gameSettings.chatWidth + 40) / 320 * (1 / mc.gameSettings.chatScale) * 53).toInt - 3
    for (i <- 1 to dash)
      if (i == (dash / 2)) dashes.append("\u00a79[\u00a76SS\u00a79]\u00a79\u00a7m") else dashes.append("-")
    mc.thePlayer.addChatMessage(new ChatComponentText(s"\u00a79\u00a7m$dashes"))
  }

  def checkForUpdates(): String = {
    try {
      val url = new URL("https://raw.githubusercontent.com/mew/simplestats/master/version.txt")
      val connection = url.openConnection
      connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0")
      connection.connect()
      val serverResponse: BufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream))
      val response: String = serverResponse.readLine
      serverResponse.close()
      response
    } catch {
      case _: Exception => SimpleStats.VERSION
    }
  }
}
