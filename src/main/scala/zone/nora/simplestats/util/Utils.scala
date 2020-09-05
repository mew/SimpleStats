package zone.nora.simplestats.util

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL
import java.sql.Timestamp
import java.text.{NumberFormat, SimpleDateFormat}
import java.util.regex.Pattern
import java.util.{Date, UUID}

import com.google.gson.{JsonElement, JsonObject}
import net.hypixel.api.HypixelAPI
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import zone.nora.simplestats.SimpleStats

object Utils {
  private final val PREFIX = "\u00a79[\u00a76SS\u00a79] \u00a7f"
  private final val VERSION_URL = new URL("https://raw.githubusercontent.com/mew/simplestats/master/version.txt")
  private final val MINECRAFT = Minecraft.getMinecraft

  def validateKey(apiKey: String): Boolean = try {
    val api = new HypixelAPI(UUID.fromString(apiKey))
    val response: Boolean = api.getKey.get().isSuccess
    api.shutdown()
    response
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

    val plusColour = if (player.has("rankPlusColor"))
      Constants.COLOUR_NAME_TO_CODE(player.get("rankPlusColor").getAsString.toLowerCase) else "\u00a7c"

    val plusPlusColour = if (player.has("monthlyRankColor"))
      Constants.COLOUR_NAME_TO_CODE(player.get("monthlyRankColor").getAsString.toLowerCase) else "\u00a76"

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

  def getGuildLevel(experience: Long): Double = {
    var exp = experience
    val exps = List(
      100000, 150000, 250000, 500000, 750000, 1000000, 1250000,
      1500000, 2000000, 2500000, 2500000, 2500000, 500000, 2500000
    )

    var c = 0.0
    exps.foreach { it =>
      if (it > exp) c + Utils.roundDouble(it / exp)
      exp -= it
      c += 1
    }

    val increment = 3000000
    while (exp > increment) {
      c += 1
      exp -= increment
    }
    c.+(Utils.roundDouble(exp / increment))
  }

  def roundDouble(n: Double): Double = (math rint n * 100) / 100

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

  def parseTime(time: Long): String = try
    new SimpleDateFormat("dd/MM/yyyy").format(new Date(new Timestamp(time).getTime))
  catch {
    case _: Exception => "N/A"
  } // Hypixel staff can hide their stats, which causes this function to freak out.

  def error(message: String, prefix: Boolean = false): Unit = put(s"\u00a7c$message", prefix)

  def put(message: String, prefix: Boolean = false): Unit =
    MINECRAFT.thePlayer.addChatMessage(new ChatComponentText(s"${if (prefix) PREFIX else ""}$message"))

  def breakLine(): Unit = {
    val dashes = new StringBuilder
    val dash = Math.floor((280 * MINECRAFT.gameSettings.chatWidth + 40) / 320 * (1 / MINECRAFT.gameSettings.chatScale) * 53).toInt - 3
    for (i <- 1 to dash)
      if (i == (dash / 2)) dashes.append("\u00a79[\u00a76SS\u00a79]\u00a7m") else dashes.append("-")
    MINECRAFT.thePlayer.addChatMessage(new ChatComponentText(s"\u00a79\u00a7m$dashes"))
  }

  def addDashes(uuidString: String): UUID =
    UUID.fromString(Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})").matcher(uuidString).replaceAll("$1-$2-$3-$4-$5"))

  def formatStat(name: String, value: Any): String = {
    def f[T](v: T) = v match {
      case _: String => '7'
      case _: Number => 'e'
      case b: Boolean => if (b) 'a' else 'c'
      case j: JsonElement =>
        val jp = j.getAsJsonPrimitive
        if (jp.isBoolean) {
          if (jp.getAsBoolean) 'a' else 'c'
        }
        else if (jp.isNumber) 'e'
        else if (jp.isString) '7'
      case _ => '6'
    }

    def y(value: String): String = {
      if (value.contains("/") || value.contains("\u272b")) value
      else if (value.contains("#")) value
      else {
        val digits = "\\d+.\\d+".r.unanchored
        val t = digits.replaceAllIn(value, m =>
          if (m.group(0) contains ".") {
            val formatter = NumberFormat.getInstance
            formatter.format(m.group(0).toDouble)
          }
          else f"${m.group(0).toInt}%,d"
        )
        t
      }
    }

    val statColour = s"\u00a7${f(value)}"
    val done = try { y(value.toString) } catch { case _: Throwable => value.toString}
    s"$name\u00a7r: ${if (value == null) "\u00a7cN/A" else s"$statColour$done"}"
  }

  def checkForUpdates(): String = {
    try {
      val connection = VERSION_URL.openConnection
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
