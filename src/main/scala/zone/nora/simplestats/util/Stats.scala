package zone.nora.simplestats.util

import java.math.BigInteger
import java.sql.Timestamp

import com.google.gson.{JsonElement, JsonObject}
import net.hypixel.api.HypixelAPI
import net.hypixel.api.reply.PlayerReply
import net.hypixel.api.util.ILeveling
import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent

import scala.collection.mutable.ListBuffer

/**
 * Gets the stats of a Hypixel player
 *
 * @param api  HypixelAPI instance to use.
 * @param name The name of the player to get stats of.
 */
//noinspection DuplicatedCode
class Stats(api: HypixelAPI, name: String) {

  // Player reply from Hypixel API
  val reply: PlayerReply = api.getPlayerByName(name).get()

  // Player reply from Hypixel API as a JSON object.
  val player: JsonObject = reply.getPlayer

  // This is what contains the messages to be printed to the console.
  val lines: ListBuffer[String] = new ListBuffer

  /**
   * Prints the player's Hypixel profile
   */
  def printStats(): Unit = {
    firstLine(player)
    saveStat("Hypixel Level", try {
      ILeveling.getLevel(player.get("networkExp").getAsDouble).toInt
    } catch {
      case _: NullPointerException => 1
    })

    saveStat("Achievement Points", player.get("achievementPoints"))
    saveStat("Karma", player.get("karma"))
    saveStat("Discord", try {
      player.get("socialMedia").getAsJsonObject.get("links").getAsJsonObject.get("DISCORD").getAsString
    } catch {
      case _: NullPointerException => null
    })

    // https://steveridout.github.io/mongo-object-time/
    saveStat("First Login",
      Utils.parseTime(new BigInteger(player.get("_id").getAsString.substring(0, 8), 16).longValue * 1000))
    saveStat("Last Login", try {
      Utils.parseTime(player.get("lastLogin").getAsLong)
    } catch {
      case _: NullPointerException => "Hidden"
    })

    saveStat("Online", try {
      player.get("lastLogin").getAsLong > player.get("lastLogout").getAsLong
    } catch {
      case _: NullPointerException => false
    })

    Utils.breakLine()
    lines.foreach { it => Utils.put(it) }
    Utils.breakLine()
  }

  /**
   * Prints the stats of a player to the minecraft chat.
   *
   * @param game Name of the game.
   */
  def printStats(game: String): Unit = {
    if (!player.has("stats")) {
      Utils.error("This player has manually hidden their stats :(", prefix = true)
    }

    var b = true
    var g = (false, "")
    game.toLowerCase match {
      case "arc" | "arcade" =>
        val arcade = getGameStats(player, "Arcade")
        if (arcade == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Arcade")
        saveStat("Coins", arcade.get("coins").getAsInt)
      case "ab" | "arena" | "arenabrawl" =>
        val arena = getGameStats(player, "Arena")
        if (arena == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Arena Brawl")
        saveStat("1v1 Wins", arena.get("wins_1v1"))
        saveStat("2v2 Wins", arena.get("wins_2v2"))
        saveStat("4v4 Wins", arena.get("wins_4v4"))
        saveStat("1v1 Losses", arena.get("losses_1v1"))
        saveStat("2v2 Losses", arena.get("losses_2v2"))
        saveStat("4v4 Losses", arena.get("losses_4v4"))
        saveStat("Coins", arena.get("coins"))
        try {
          saveStat("Offensive Skill", arena.get("offensive").getAsString.replace("_", " "))
          saveStat("Utility Skill", arena.get("utility").getAsString.replace("_", " "))
          saveStat("Support Skill", arena.get("support").getAsString.replace("_", " "))
          saveStat("Ultimate Skill", arena.get("ultimate").getAsString.replace("_", " "))
        } catch {
          case _: NullPointerException => /* ignored */
        }
      case "wl" | "bg" | "warlords" | "battleground" =>
        val bg = getGameStats(player, "Battleground")
        if (bg == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Warlords")
        saveStat("Kills", bg.get("kills"))
        saveStat("Assists", bg.get("assists"))
        saveStat("Wins", bg.get("wins"))
        saveStat("Losses", bg.get("losses"))
        saveStat("Coins", bg.get("coins"))
        saveStat("Mage Level", Utils.getWarlordsClassLevel(bg, "mage"))
        saveStat("Paladin Level", Utils.getWarlordsClassLevel(bg, "paladin"))
        saveStat("Shaman Level", Utils.getWarlordsClassLevel(bg, "shaman"))
        saveStat("Warrior Level", Utils.getWarlordsClassLevel(bg, "warrior"))
      case "bw" | "bedwars" =>
        val bw = getGameStats(player, "Bedwars")
        if (bw == null) {
          api.shutdown()
          return
        }
        val achievements = player.get("achievements").getAsJsonObject
        firstLine(player, "BedWars")
        saveStat("Level", achievements.get("bedwars_level"))
        saveStat("Kills", bw.get("kills_bedwars"))
        saveStat("Wins", bw.get("wins_bedwars"))
        saveStat("Losses", bw.get("losses_bedwars"))
        saveStat("Final Kills", bw.get("final_kills_bedwars"))
        saveStat("Final Deaths", bw.get("final_deaths_bedwars"))
        saveStat("Winstreak", bw.get("winstreak"))
        saveStat("Coins", bw.get("coins"))
      case "bb" | "build" | "buildbattle" =>
        val bb = getGameStats(player, "BuildBattle")
        if (bb == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Build Battle")
        saveStat("Wins", bb.get("wins"))
        saveStat("Score", bb.get("score"))
        saveStat("Coins", bb.get("coins"))
        saveStat("Games Played", bb.get("games_played"))
        saveStat("Correct GTB Guesses", bb.get("correct_guesses"))
      case "duels" =>
        val duels = getGameStats(player, "Duels")
        if (duels == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Duels")
        saveStat("Wins", duels.get("wins"))
        saveStat("Losses", duels.get("losses"))
        saveStat("Coins", duels.get("coins"))
        saveStat("Melee Hits", duels.get("melee_hits"))
        saveStat("Bow Hits", duels.get("bow_hits"))
        saveStat("Winstreak", duels.get("current_winstreak"))
      case "tkr" | "turbo" | "turbokartracers" | "gingerbread" =>
        val tkr = getGameStats(player, "GingerBread")
        if (tkr == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Turbo Kart Racers")
        saveStat("Gold Trophies", tkr.get("gold_trophy"))
        saveStat("Silver Trophies", tkr.get("silver_trophy"))
        saveStat("Bronze Trophies", tkr.get("bronze_trophy"))
        saveStat("Coins", tkr.get("coins"))
      case "blitz" | "bsg" | "sg" | "hg" | "hungergames" =>
        val bsg = getGameStats(player, "HungerGames")
        if (bsg == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Blitz Survival Games")
        saveStat("Kills", bsg.get("kills"))
        saveStat("Wins", bsg.get("wins"))
        saveStat("Deaths", bsg.get("Deaths"))
        saveStat("Coins", bsg.get("coins"))
        saveStat("Default Kit", bsg.get("defaultkit"))
      case "legacy" | "classic" | "classiclobby" =>
        val legacy = getGameStats(player, "Legacy")
        if (legacy == null) return
        firstLine(player, "Classic Lobby")
        saveStat("Current Tokens", legacy.get("tokens"))
        saveStat("Lifetime Tokens", legacy.get("total_tokens"))
      case "cvc" | "cac" | "copsandcrims" | "mcgo" =>
        val cvc = getGameStats(player, "MCGO")
        if (cvc == null) {
          api.shutdown()
          return
        }
        firstLine(player, "CVC")
        saveStat("Defusal Kills", cvc.get("kills"))
        saveStat("Defusal Round Wins", cvc.get("round_wins"))
        saveStat("Defusal Game Wins", cvc.get("game_wins"))
        saveStat("Defusal Deaths", cvc.get("deaths"))
        saveStat("Deathmatch Kills", cvc.get("kills_deathmatch"))
        saveStat("Deathmatch Wins", cvc.get("game_wins_deathmatch"))
        saveStat("Deathmatch Deaths", cvc.get("deaths_deathmatch"))
        saveStat("Coins", cvc.get("coins"))
      case "pb" | "paintball" =>
        val pb = getGameStats(player, "Paintball")
        if (pb == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Paintball")
        saveStat("Kills", pb.get("kills"))
        saveStat("Wins", pb.get("wins"))
        saveStat("Deaths", pb.get("deaths"))
        saveStat("Coins", pb.get("coins"))
        saveStat("Shots Fired", pb.get("shots_fired"))
      case "pit" => Utils.error("Stats lookup for The Pit is coming soon (tm)")
      case "q" | "quake" | "quakecraft" =>
        val q = getGameStats(player, "Quake")
        if (q == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Quake")
        saveStat("Solo Kills", q.get("kills"))
        saveStat("Solo Wins", q.get("wins"))
        saveStat("Teams Kills", q.get("kills_teams"))
        saveStat("Teams Wins", q.get("wins_teams"))
        saveStat("Coins", q.get("coins"))
        saveStat("Trigger", q.get("trigger"))
      case "sb" | "skyblock" => Utils.error("Stats lookup for SkyBlock are coming soon (tm)")
      case "sw" | "skywars" =>
        val sw = getGameStats(player, "SkyWars")
        if (sw == null) {
          api.shutdown()
          return
        }
        firstLine(player, "SkyWars")
        saveStat("Kills", sw.get("kills"))
        saveStat("Wins", sw.get("wins"))
        saveStat("Deaths", sw.get("deaths"))
        // https://hypixel.net/posts/19293045
        val swLevel = try {
          val swExp = sw.get("skywars_experience").getAsInt.toDouble
          val exps = 0 :: 20 :: 70 :: 150 :: 250 :: 500 :: 1000 :: 2000 :: 3500 :: 6000 :: 10000 :: 15000 :: Nil
          if (swExp >= 1500) (swExp - 15000) / 10000 + 12
          else for (i <- exps.indices) if (swExp < exps(i)) 1 + i + (swExp - exps(i - 1)) / (exps(i) - exps(i - 1))
        } catch {
          case e: Exception => e.printStackTrace(); "Error"
        }
        saveStat("SkyWars Level", swLevel)
        saveStat("Coins", sw.get("coins"))
        saveStat("Souls", sw.get("souls"))
        saveStat("Heads", sw.get("heads"))
      case "sc" | "skyclash" =>
        val sc = getGameStats(player, "SkyClash")
        if (sc == null) {
          api.shutdown()
          return
        }
        firstLine(player, "SkyClash")
        saveStat("Kills", sc.get("kills"))
        saveStat("Wins", sc.get("wins"))
        saveStat("Deaths", sc.get("deaths"))
        saveStat("Coins", sc.get("coins"))
        saveStat("Winstreak", sc.get("win_streak"))
        saveStat("Card Packs", sc.get("card_packs"))
      case "suhc" | "speed" | "speeduhc" =>
        val suhc = getGameStats(player, "SpeedUHC")
        if (suhc == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Speed UHC")
        saveStat("Kills", suhc.get("kills"))
        saveStat("Wins", suhc.get("wins"))
        saveStat("Deaths", suhc.get("deaths"))
        saveStat("Coins", suhc.get("coins"))
        try {
          saveStat("Selected Mastery", suhc.get("activeMasteryPerk").getAsString.split("_")(1))
        } catch {
          case _: Exception => /* ignored */
        }
        saveStat("Winstreak", suhc.get("winstreak"))
      case "sh" | "smash" | "supersmash" | "smashheroes" =>
        val sh = getGameStats(player, "SuperSmash")
        if (sh == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Smash Heroes")
        saveStat("Kills", sh.get("kills"))
        saveStat("Wins", sh.get("wins"))
        saveStat("Deaths", sh.get("deaths"))
        saveStat("Coins", sh.get("coins"))
        saveStat("Smash Level", sh.get("smashLevel"))
        val activeClass = sh.get("active_class").getAsString
        val prestige = if (sh.has(s"pg_$activeClass")) sh.get(s"pg_$activeClass").getAsInt else 0
        val level = if (sh.has(s"lastLevel_$activeClass")) sh.get(s"lastLevel_$activeClass") else 0
        saveStat("Active Class", s"$activeClass (P$prestige Lv$level)")
      case "tnt" | "tntgames" =>
        val tnt = getGameStats(player, "TNTGames")
        if (tnt == null) {
          api.shutdown()
          return
        }
        firstLine(player, "TNT Games")
        saveStat("TNT Run Wins", tnt.get("wins_tntrun"))
        saveStat("PVP Run Wins", tnt.get("win_pvprun"))
        saveStat("Bowspleef Wins", tnt.get("wins_bowspleef"))
        saveStat("TNT Wizards Wins", tnt.get("wins_capture"))
        saveStat("TNT Tag Wins", tnt.get("wins_tnttag"))
        saveStat("Coins", tnt.get("coins"))
      case "tc" | "cw" | "crazywalls" | "truecombat" =>
        val cw = getGameStats(player, "TrueCombat")
        if (cw == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Crazy Walls")
        saveStat("Kills", cw.get("kills"))
        saveStat("Wins", cw.get("wins"))
        saveStat("Deaths", cw.get("deaths"))
        saveStat("Coins", cw.get("coins"))
        saveStat("Golden Skulls", cw.get("golden_skulls"))
        saveStat("Gold Dust", cw.get("gold_dust"))
      case "uhc" =>
        val uhc = getGameStats(player, "UHC")
        if (uhc == null) {
          api.shutdown()
          return
        }
        firstLine(player, "UHC")
        saveStat("Kills", uhc.get("kills"))
        saveStat("Wins", uhc.get("wins"))
        saveStat("Deaths", uhc.get("deaths"))
        saveStat("Coins", uhc.get("coins"))
        saveStat("Score", uhc.get("score"))
        saveStat("Heads Eaten", uhc.get("heads_eaten"))
        try {
          saveStat("Selected Kit", uhc.get("equippedKit").getAsString)
        } catch {
          case _: Exception => /* ignored */
        }
      case "vz" | "vampz" | "vampirez" =>
        val vz = getGameStats(player, "VampireZ")
        if (vz == null) {
          api.shutdown()
          return
        }
        firstLine(player, "VampireZ")
        saveStat("Human Wins", vz.get("human_wins"))
        saveStat("Vampire Wins", vz.get("vampire_wins"))
        saveStat("Zombie Kills", vz.get("zombie_kills"))
        saveStat("Coins", vz.get("coins"))
      case "walls" =>
        val walls = getGameStats(player, "Walls")
        if (walls == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Walls")
        saveStat("Kills", walls.get("kills"))
        saveStat("Wins", walls.get("wins"))
        saveStat("Losses", walls.get("losses"))
        saveStat("Coins", walls.get("coins"))
        saveStat("Insane Farmer", walls.get("insane_farmer"))
      case "walls3" | "mw" | "mega" | "megawalls" =>
        val mw = getGameStats(player, "Walls3")
        if (mw == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Mega Walls")
        saveStat("Kills", mw.get("kills"))
        saveStat("Wins", mw.get("wins"))
        saveStat("Losses", mw.get("losses"))
        saveStat("Finals", mw.get("total_final_kills"))
        saveStat("Final Deaths", mw.get("final_deaths"))
        saveStat("Selected Class", mw.get("chosen_class"))
      case "mm" | "murder" | "murdermystery" =>
        val mm = getGameStats(player, "MurderMystery")
        if (mm == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Murder Mystery")
        saveStat("Coins", mm.get("coins"))
        saveStat("Murderer Wins", mm.get("murderer_wins"))
        saveStat("Murderer Kills", mm.get("kills_as_murderer"))
        saveStat("Detective Wins", mm.get("detective_wins"))
        saveStat("Assassins Wins", mm.get("wins_MURDER_ASSASSINS"))
        saveStat("Infection Wins", mm.get("wins_MURDER_INFECTION"))
        saveStat("Murderer Chance", s"${mm.get("murderer_chance")}%")
        saveStat("Detective Chance", s"${mm.get("detective_chance")}%")
      case "g" | "guild" =>
        val guildReply = api.getGuildByPlayer(player.get("uuid").getAsString).get()
        if (!guildReply.isSuccess) {
          Utils.error(s"Unexpected error getting ${player.get("displayname").getAsString}'s Guild:'")
          Utils.error(guildReply.getCause)
          return
        }
        val guild = guildReply.getGuild
        if (guild == null) {
          Utils.error(s"${player.get("displayname").getAsString} is not in a guild.")
          api.shutdown()
          return
        }
        firstLine(player, guild = true)
        saveStat("Name", guild.getName)
        saveStat("Tag", s"[${guild.getTag}]")
        saveStat("Level", getGuildLevel(guild.getExp))
        saveStat("Created", Utils.parseTime(Timestamp.valueOf(guild.getCreated.toLocalDateTime).getTime))
        saveStat("Members", s"${guild.getMembers.size}/125")
        g = (true, guild.getName)
      case _ =>
        b = false
        Utils.breakLine()
        Utils.error(s"$game is not a valid game.")
        Utils.error("Try one of these:")
        List(
          "arcade", "arenabrawl", "warlords", "bedwars", "duels", "tkr", "blitz", "legacy", "cvc", "paintball",
          "quake", "skywars", "skyclash", "speeduhc", "smash", "tnt", "crazywalls", "uhc", "vampirez", "walls",
          "megawalls", "murdermystery", "guild"
        ).foreach { it => Utils.put(s"\u00a78- \u00a7a$it") }
    }

    if (b) Utils.breakLine()
    lines.foreach { it => Utils.put(it) }
    if (g._1) Minecraft.getMinecraft.thePlayer.addChatMessage(
      ChatComponentBuilder.of("\u00a7aClick to view on \u00a73plancke.io")
        .setHoverEvent(s"Click here to view ${g._2} on plancke.io")
        .setClickEvent(
          ClickEvent.Action.OPEN_URL,
          s"https://ncke.io/hypixel/guild/player/${player.get("uuid").getAsString}"
        ).build)
    Utils.breakLine()
  }

  /**
   * Calculate the guild level based on total guild experience.
   * Credit to littlemissmadivirus.
   */
  private def getGuildLevel(experience: Long): Double = {
    var exp = experience
    val exps = List(
      100000, 150000, 250000, 500000, 750000, 1000000, 1250000, 1500000, 2000000, 2500000, 2500000, 2500000, 2500000,
      2500000
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

  /**
   * Returns the game stats JSON object from Hypixel's API.
   */
  private def getGameStats(player: JsonObject, game: String): JsonObject = try {
    player.get("stats").getAsJsonObject.get(game).getAsJsonObject
  } catch {
    case _: Exception =>
      Utils.error(s"${player.get("displayname").getAsString} has no stats in $game")
      Utils.breakLine()
      null
  }

  /**
   * Saves the stat value into line buffer in key-value format.
   */
  private def saveStat(name: String, value: Any): Unit = {
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
      if (value.contains("/")) value
      else if (value.contains("#")) value
      else {
        val digits = "\\d+.\\d+".r.unanchored
        val t = digits.replaceAllIn(value, m =>
          if (m.group(0) contains ".") {
            val formatter = java.text.NumberFormat.getInstance
            formatter.format(m.group(0).toDouble)
          }
          else f"${m.group(0).toInt}%,d"
        )
        t
      }
    }

    val statColour = s"\u00a7${f(value)}"
    val done = if (value == null) null else y(value.toString)
    lines.append(s"$name: ${if (value == null) "\u00a7cN/A" else s"$statColour$done"}")
  }

  /**
   * Prints the first line showing the name/guild of the player.
   */
  private def firstLine(player: JsonObject, game: String = "", guild: Boolean = false): Unit = {
    val s = if (game == "" && !guild) "Stats" else if (guild) "Guild" else s"$game stats"
    val rank = try Utils.getRank(player)
    catch {
      case e: Exception => e.printStackTrace(); ""
    }

    lines.append(s"$s of ${if (rank.endsWith("]")) s"$rank " else rank}${player.get("displayname").getAsString}")
  }
}
