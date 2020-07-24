package zone.nora.simplestats.core

import java.math.BigInteger
import java.sql.Timestamp

import com.google.gson.{JsonElement, JsonObject}
import net.hypixel.api.HypixelAPI
import net.hypixel.api.reply.PlayerReply
import net.hypixel.api.util.ILeveling
import zone.nora.simplestats.util.Utils

import scala.collection.mutable.ListBuffer

/**
 * Gets the stats of a Hypixel player and saves it into a line buffer.
 *
 * @param api     HypixelAPI instance to use.
 * @param name    The name of the player to get stats of.
 * @param compact Save stats of a player compactly in one-line.
 */
//noinspection DuplicatedCode
class Stats(api: HypixelAPI, name: String, compact: Boolean = false) {

  // Player reply from Hypixel API
  val reply: PlayerReply = api.getPlayerByName(name).get()

  // Player response JSON object from Hypixel API
  val player: JsonObject = reply.getPlayer

  // Contains the messages to be printed to the console.
  val lines: ListBuffer[String] = new ListBuffer[String]

  /**
   * Print stats from the lines buffer into the minecraft chat.
   */
  def printStats(): Unit = {
    if (compact) { // Compact mode
      Utils.put(getStatsInOneLine, prefix = true)
    } else { // Detailed mode
      Utils.breakLine()
      lines.foreach { it => Utils.put(it) }
      Utils.breakLine()
    }
  }

  /**
   * @return Contents of the line buffer in one line.
   */
  def getStatsInOneLine: String = {
    val str = new StringBuilder
    lines.foreach { it => str.append(it).append("\u00a7f").append(" ") }
    str.toString()
  }

  /**
   * Saves profile stats into the line buffer.
   *
   * @note Also prints them if {print} is true
   */
  def saveStats(): Unit = {
    firstLine(player)
    if (player == null) {
      firstLine(player)
      lines.append("Invalid player.")
      return
    }

    saveStatsToBuffer("HyLevel", try {
      ILeveling.getLevel(player.get("networkExp").getAsDouble).toInt
    } catch {
      case _: NullPointerException => 1
    })
    saveStatsToBuffer("AP", player.get("achievementPoints"))
    saveStatsToBuffer("Online", try {
      player.get("lastLogin").getAsLong > player.get("lastLogout").getAsLong
    } catch {
      case _: NullPointerException => false
    })

    if (!compact) { // Add stuff you only want to be included in detailed mode
      saveStatsToBuffer("Discord", try {
        player.get("socialMedia").getAsJsonObject.get("links").getAsJsonObject.get("DISCORD").getAsString
      } catch {
        case _: NullPointerException => "Not defined"
      })

      saveStatsToBuffer("Karma", player.get("karma"))
      saveStatsToBuffer("First Login", // https://steveridout.github.io/mongo-object-time/
        Utils.parseTime(new BigInteger(player.get("_id").getAsString.substring(0, 8), 16).longValue * 1000))
      saveStatsToBuffer("Last Login", try {
        Utils.parseTime(player.get("lastLogin").getAsLong)
      } catch {
        case _: NullPointerException => "Hidden"
      })
    }
  }

  /**
   * Saves stats value into line buffer in a key-value format.
   */
  private def saveStatsToBuffer(name: String, value: Any): Unit = {
    if (value == null) return
    if (value.isInstanceOf[String] && value.toString.isEmpty) return

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
    val done = y(value.toString)
    lines.append(s"$name: ${if (value == null) "\u00a7cN/A" else s"$statColour$done"}")
  }

  /**
   * Prints the first line showing the name/guild of the player.
   */
  private def firstLine(player: JsonObject, game: String = "", guild: Boolean = false): Unit = {
    val rank = try Utils.getRank(player)
    catch {
      case e: Exception => e.printStackTrace(); ""
    }

    val name: String = if (rank.endsWith("]")) rank + " " + player.get("displayname").getAsString
    else rank + player.get("displayname").getAsString

    if (compact) lines.append(name)
    else {
      val str = if (game == "" && !guild) "Stats" else if (guild) "Guild" else s"$game stats"
      lines.append(s"$str of $name")
    }
  }

  /**
   * Saves stats of a Hypixel player to the line buffer.
   *
   * @param game Name of the game.
   * @note prints stats as well if {print} is true.
   * @note compact mode is only defined for bedwars atm.
   */
  def saveStats(game: String): Unit = {
    if (player == null) {
      firstLine(player)
      lines.append("Invalid player.")
      return
    }

    if (!player.has("stats")) {
      firstLine(player)
      lines.append("Hidden stats.")
      return
    }

    game.toLowerCase match {
      case "arc" | "arcade" =>
        val arcade = new StatsManager(player, "Arcade")
        if (arcade.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Arcade")
        saveStatsToBuffer("Coins", arcade.getStatsAsInt("coin"))
      case "ab" | "arena" | "arenabrawl" =>
        val arena = new StatsManager(player, "Arena")
        if (arena.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Arena Brawl")
        saveStatsToBuffer("1v1 Wins", arena.getStatsAsInt("wins_1v1"))
        saveStatsToBuffer("2v2 Wins", arena.getStatsAsInt("wins_2v2"))
        saveStatsToBuffer("4v4 Wins", arena.getStatsAsInt("wins_4v4"))
        saveStatsToBuffer("1v1 Losses", arena.getStatsAsInt("losses_1v1"))
        saveStatsToBuffer("2v2 Losses", arena.getStatsAsInt("losses_2v2"))
        saveStatsToBuffer("4v4 Losses", arena.getStatsAsInt("losses_4v4"))
        saveStatsToBuffer("Coins", arena.getStatsAsInt("coins"))
        saveStatsToBuffer("Offensive Skill", arena.getStatsAsString("offensive").replace("_", " "))
        saveStatsToBuffer("Utility Skill", arena.getStatsAsString("utility").replace("_", " "))
        saveStatsToBuffer("Support Skill", arena.getStatsAsString("support").replace("_", " "))
        saveStatsToBuffer("Ultimate Skill", arena.getStatsAsString("ultimate").replace("_", " "))
      case "wl" | "bg" | "warlords" | "battleground" =>
        val bg = new StatsManager(player, "Battleground")
        if (bg.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Warlords")
        saveStatsToBuffer("Kills", bg.getStatsAsInt("kills"))
        saveStatsToBuffer("Assists", bg.getStatsAsInt("assists"))
        saveStatsToBuffer("Wins", bg.getStatsAsInt("wins"))
        saveStatsToBuffer("Losses", bg.getStatsAsInt("losses"))
        saveStatsToBuffer("Coins", bg.getStatsAsInt("coins"))
        saveStatsToBuffer("Mage Level", Utils.getWarlordsClassLevel(bg.stats, "mage"))
        saveStatsToBuffer("Paladin Level", Utils.getWarlordsClassLevel(bg.stats, "paladin"))
        saveStatsToBuffer("Shaman Level", Utils.getWarlordsClassLevel(bg.stats, "shaman"))
        saveStatsToBuffer("Warrior Level", Utils.getWarlordsClassLevel(bg.stats, "warrior"))
      case "bw" | "bedwars" =>
        val bw = new StatsManager(player, "Bedwars")
        if (bw.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "BedWars")
        if (bw.achievements != null) saveStatsToBuffer("Level", bw.achievements.get("bedwars_level").getAsInt)


        saveStatsToBuffer("WLR", bw.getStatsAsInt("wins_bedwars") / bw.getStatsAsInt("losses_bedwars", one = true))
        saveStatsToBuffer("FKDR", bw.getStatsAsInt("final_kills_bedwars") / bw.getStatsAsInt("final_deaths_bedwars", one = true))
        saveStatsToBuffer("WS", bw.getStatsAsInt("winstreak"))

        if (!compact) { // show these stats if in detailed mode
          saveStatsToBuffer("Kills", bw.getStatsAsInt("kills_bedwars"))
          saveStatsToBuffer("Coins", bw.getStatsAsInt("coins"))
        }
      case "bb" | "build" | "buildbattle" =>
        val bb = new StatsManager(player, "BuildBattle")
        if (bb.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Build Battle")
        saveStatsToBuffer("Wins", bb.getStatsAsInt("wins"))
        saveStatsToBuffer("Score", bb.getStatsAsInt("score"))
        saveStatsToBuffer("Coins", bb.getStatsAsInt("coins"))
        saveStatsToBuffer("Games Played", bb.getStatsAsInt("games_played"))
        saveStatsToBuffer("Correct GTB Guesses", bb.getStatsAsInt("correct_guesses"))
      case "duels" =>
        val duels = new StatsManager(player, "Duels")
        if (duels.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Duels")
        saveStatsToBuffer("Wins", duels.getStatsAsInt("wins"))
        saveStatsToBuffer("Losses", duels.getStatsAsInt("losses"))
        saveStatsToBuffer("Coins", duels.getStatsAsInt("coins"))
        saveStatsToBuffer("Melee Hits", duels.getStatsAsInt("melee_hits"))
        saveStatsToBuffer("Bow Hits", duels.getStatsAsInt("bow_hits"))
        saveStatsToBuffer("Winstreak", duels.getStatsAsInt("current_winstreak"))
      case "tkr" | "turbo" | "turbokartracers" | "gingerbread" =>
        val tkr = new StatsManager(player, "GingerBread")
        if (tkr.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Turbo Kart Racers")
        saveStatsToBuffer("Gold Trophies", tkr.getStatsAsInt("gold_trophy"))
        saveStatsToBuffer("Silver Trophies", tkr.getStatsAsInt("silver_trophy"))
        saveStatsToBuffer("Bronze Trophies", tkr.getStatsAsInt("bronze_trophy"))
        saveStatsToBuffer("Coins", tkr.getStatsAsInt("coins"))
      case "blitz" | "bsg" | "sg" | "hg" | "hungergames" =>
        val bsg = new StatsManager(player, "HungerGames")
        if (bsg.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Blitz Survival Games")
        saveStatsToBuffer("Kills", bsg.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", bsg.getStatsAsInt("wins"))
        saveStatsToBuffer("Deaths", bsg.getStatsAsInt("deaths"))
        saveStatsToBuffer("Coins", bsg.getStatsAsInt("coins"))
        saveStatsToBuffer("Default Kit", bsg.getStatsAsString("defaultkit"))
      case "legacy" | "classic" | "classiclobby" =>
        val legacy = new StatsManager(player, "Legacy")
        if (legacy.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Classic Lobby")
        saveStatsToBuffer("Current Tokens", legacy.getStatsAsInt("tokens"))
        saveStatsToBuffer("Lifetime Tokens", legacy.getStatsAsInt("total_tokens"))
      case "cvc" | "cac" | "copsandcrims" | "mcgo" =>
        val cvc = new StatsManager(player, "MCGO")
        if (cvc.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "CVC")
        saveStatsToBuffer("Defusal Kills", cvc.getStatsAsInt("kills"))
        saveStatsToBuffer("Defusal Round Wins", cvc.getStatsAsInt("round_wins"))
        saveStatsToBuffer("Defusal Game Wins", cvc.getStatsAsInt("game_wins"))
        saveStatsToBuffer("Defusal Deaths", cvc.getStatsAsInt("deaths"))
        saveStatsToBuffer("Deathmatch Kills", cvc.getStatsAsInt("kills_deathmatch"))
        saveStatsToBuffer("Deathmatch Wins", cvc.getStatsAsInt("game_wins_deathmatch"))
        saveStatsToBuffer("Deathmatch Deaths", cvc.getStatsAsInt("deaths_deathmatch"))
        saveStatsToBuffer("Coins", cvc.getStatsAsInt("coins"))
      case "pb" | "paintball" =>
        val pb = new StatsManager(player, "Paintball")
        if (pb.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Paintball")
        saveStatsToBuffer("Kills", pb.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", pb.getStatsAsInt("wins"))
        saveStatsToBuffer("Deaths", pb.getStatsAsInt("deaths"))
        saveStatsToBuffer("Coins", pb.getStatsAsInt("coins"))
        saveStatsToBuffer("Shots Fired", pb.getStatsAsInt("shots_fired"))
      case "pit" => Utils.error("Stats lookup for The Pit is coming soon (tm)")
      case "q" | "quake" | "quakecraft" =>
        val q = new StatsManager(player, "Quake")
        if (q.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Quake")
        saveStatsToBuffer("Solo Kills", q.getStatsAsInt("kills"))
        saveStatsToBuffer("Solo Wins", q.getStatsAsInt("wins"))
        saveStatsToBuffer("Teams Kills", q.getStatsAsInt("kills_teams"))
        saveStatsToBuffer("Teams Wins", q.getStatsAsInt("wins_teams"))
        saveStatsToBuffer("Coins", q.getStatsAsInt("coins"))
        saveStatsToBuffer("Trigger", q.getStatsAsString("trigger"))
      case "sb" | "skyblock" => Utils.error("Stats lookup for SkyBlock are coming soon (tm)")
      case "sw" | "skywars" =>
        val sw = new StatsManager(player, "SkyWars")
        if (sw.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "SkyWars")
        saveStatsToBuffer("Kills", sw.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", sw.getStatsAsInt("wins"))
        saveStatsToBuffer("Deaths", sw.getStatsAsInt("deaths"))
        val swLevel = try { // https://hypixel.net/posts/19293045
          val swExp = sw.getStatsAsDouble("skywars_experience")
          val exps = 0 :: 20 :: 70 :: 150 :: 250 :: 500 :: 1000 :: 2000 :: 3500 :: 6000 :: 10000 :: 15000 :: Nil
          if (swExp >= 1500) (swExp - 15000) / 10000 + 12
          else for (i <- exps.indices) if (swExp < exps(i)) 1 + i + (swExp - exps(i - 1)) / (exps(i) - exps(i - 1))
        } catch {
          case e: Exception => e.printStackTrace(); "Error"
        }
        saveStatsToBuffer("SkyWars Level", swLevel)
        saveStatsToBuffer("Coins", sw.getStatsAsInt("coins"))
        saveStatsToBuffer("Souls", sw.getStatsAsInt("souls"))
        saveStatsToBuffer("Heads", sw.getStatsAsInt("heads"))
      case "sc" | "skyclash" =>
        val sc = new StatsManager(player, "SkyClash")
        if (sc.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "SkyClash")
        saveStatsToBuffer("Kills", sc.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", sc.getStatsAsInt("wins"))
        saveStatsToBuffer("Deaths", sc.getStatsAsInt("deaths"))
        saveStatsToBuffer("Coins", sc.getStatsAsInt("coins"))
        saveStatsToBuffer("Winstreak", sc.getStatsAsInt("win_streak"))
        saveStatsToBuffer("Card Packs", sc.getStatsAsInt("card_packs"))
      case "suhc" | "speed" | "speeduhc" =>
        val suhc = new StatsManager(player, "SpeedUHC")
        if (suhc.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Speed UHC")
        saveStatsToBuffer("Kills", suhc.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", suhc.getStatsAsInt("wins"))
        saveStatsToBuffer("Deaths", suhc.getStatsAsInt("deaths"))
        saveStatsToBuffer("Coins", suhc.getStatsAsInt("coins"))
        try {
          saveStatsToBuffer("Selected Mastery", suhc.getStatsAsString("activeMasteryPerk").split("_")(1))
        } catch {
          case _: Exception => /* ignored */
        }
        saveStatsToBuffer("Winstreak", suhc.getStatsAsInt("winstreak"))
      case "sh" | "smash" | "supersmash" | "smashheroes" =>
        val sh = new StatsManager(player, "SuperSmash")
        if (sh.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Smash Heroes")
        saveStatsToBuffer("Kills", sh.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", sh.getStatsAsInt("wins"))
        saveStatsToBuffer("Deaths", sh.getStatsAsInt("deaths"))
        saveStatsToBuffer("Coins", sh.getStatsAsInt("coins"))
        saveStatsToBuffer("Smash Level", sh.getStatsAsInt("smashLevel"))
        val activeClass = sh.getStatsAsString("active_class")
        val prestige = if (sh.stats.has(s"pg_$activeClass")) sh.getStatsAsInt(s"pg_$activeClass") else 0
        val level = if (sh.stats.has(s"lastLevel_$activeClass")) sh.getStatsAsInt(s"lastLevel_$activeClass") else 0
        saveStatsToBuffer("Active Class", s"$activeClass (P$prestige Lv$level)")
      case "tnt" | "tntgames" =>
        val tnt = new StatsManager(player, "TNTGames")
        if (tnt.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "TNT Games")
        saveStatsToBuffer("TNT Run Wins", tnt.getStatsAsInt("wins_tntrun"))
        saveStatsToBuffer("PVP Run Wins", tnt.getStatsAsInt("win_pvprun"))
        saveStatsToBuffer("Bowspleef Wins", tnt.getStatsAsInt("wins_bowspleef"))
        saveStatsToBuffer("TNT Wizards Wins", tnt.getStatsAsInt("wins_capture"))
        saveStatsToBuffer("TNT Tag Wins", tnt.getStatsAsInt("wins_tnttag"))
        saveStatsToBuffer("Coins", tnt.getStatsAsInt("coins"))
      case "tc" | "cw" | "crazywalls" | "truecombat" =>
        val cw = new StatsManager(player, "TrueCombat")
        if (cw.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Crazy Walls")
        saveStatsToBuffer("Kills", cw.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", cw.getStatsAsInt("wins"))
        saveStatsToBuffer("Deaths", cw.getStatsAsInt("deaths"))
        saveStatsToBuffer("Coins", cw.getStatsAsInt("coins"))
        saveStatsToBuffer("Golden Skulls", cw.getStatsAsInt("golden_skulls"))
        saveStatsToBuffer("Gold Dust", cw.getStatsAsInt("gold_dust"))
      case "uhc" =>
        val uhc = new StatsManager(player, "UHC")
        if (uhc.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "UHC")
        saveStatsToBuffer("Kills", uhc.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", uhc.getStatsAsInt("wins"))
        saveStatsToBuffer("Deaths", uhc.getStatsAsInt("deaths"))
        saveStatsToBuffer("Coins", uhc.getStatsAsInt("coins"))
        saveStatsToBuffer("Score", uhc.getStatsAsInt("score"))
        saveStatsToBuffer("Heads Eaten", uhc.getStatsAsInt("heads_eaten"))
        try {
          saveStatsToBuffer("Selected Kit", uhc.getStatsAsString("equippedKit"))
        } catch {
          case _: Exception => /* ignored */
        }
      case "vz" | "vampz" | "vampirez" =>
        val vz = new StatsManager(player, "VampireZ")
        if (vz.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "VampireZ")
        saveStatsToBuffer("Human Wins", vz.getStatsAsInt("human_wins"))
        saveStatsToBuffer("Vampire Wins", vz.getStatsAsInt("vampire_wins"))
        saveStatsToBuffer("Zombie Kills", vz.getStatsAsInt("zombie_kills"))
        saveStatsToBuffer("Coins", vz.getStatsAsInt("coins"))
      case "walls" =>
        val walls = new StatsManager(player, "Walls")
        if (walls.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Walls")
        saveStatsToBuffer("Kills", walls.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", walls.getStatsAsInt("wins"))
        saveStatsToBuffer("Losses", walls.getStatsAsInt("losses"))
        saveStatsToBuffer("Coins", walls.getStatsAsInt("coins"))
        saveStatsToBuffer("Insane Farmer", walls.stats.get("insane_farmer"))
      case "walls3" | "mw" | "mega" | "megawalls" =>
        val mw = new StatsManager(player, "Walls3")
        if (mw.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Mega Walls")
        saveStatsToBuffer("Kills", mw.getStatsAsInt("kills"))
        saveStatsToBuffer("Wins", mw.getStatsAsInt("wins"))
        saveStatsToBuffer("Losses", mw.getStatsAsInt("losses"))
        saveStatsToBuffer("Finals", mw.getStatsAsInt("total_final_kills"))
        saveStatsToBuffer("Final Deaths", mw.getStatsAsInt("final_deaths"))
        saveStatsToBuffer("Selected Class", mw.getStatsAsString("chosen_class"))
      case "mm" | "murder" | "murdermystery" =>
        val mm = new StatsManager(player, "MurderMystery")
        if (mm.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Murder Mystery")
        saveStatsToBuffer("Coins", mm.getStatsAsInt("coins"))
        saveStatsToBuffer("Murderer Wins", mm.getStatsAsInt("murderer_wins"))
        saveStatsToBuffer("Murderer Kills", mm.getStatsAsInt("kills_as_murderer"))
        saveStatsToBuffer("Detective Wins", mm.getStatsAsInt("detective_wins"))
        saveStatsToBuffer("Assassins Wins", mm.getStatsAsInt("wins_MURDER_ASSASSINS"))
        saveStatsToBuffer("Infection Wins", mm.getStatsAsInt("wins_MURDER_INFECTION"))
        saveStatsToBuffer("Murderer Chance", s"${mm.stats.get("murderer_chance")}%")
        saveStatsToBuffer("Detective Chance", s"${mm.stats.get("detective_chance")}%")
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
        saveStatsToBuffer("Name", guild.getName)
        saveStatsToBuffer("Tag", s"[${guild.getTag}]")
        saveStatsToBuffer("Level", Utils.getGuildLevel(guild.getExp))
        saveStatsToBuffer("Created", Utils.parseTime(Timestamp.valueOf(guild.getCreated.toLocalDateTime).getTime))
        saveStatsToBuffer("Members", s"${guild.getMembers.size}/125")
      case _ =>
        Utils.error(s"$game is not a valid game.")
        Utils.error("Try one of these:")
        List(
          "arcade", "arenabrawl", "warlords", "bedwars", "duels", "tkr", "blitz", "legacy", "cvc", "paintball",
          "quake", "skywars", "skyclash", "speeduhc", "smash", "tnt", "crazywalls", "uhc", "vampirez", "walls",
          "megawalls", "murdermystery", "guild"
        ).foreach { it => Utils.put(s"\u00a78- \u00a7a$it") }
    }
  }
}
