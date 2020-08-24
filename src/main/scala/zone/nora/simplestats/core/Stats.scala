package zone.nora.simplestats.core

import java.math.BigInteger
import java.sql.Timestamp

import com.google.gson.JsonObject
import net.hypixel.api.HypixelAPI
import net.hypixel.api.reply.PlayerReply
import net.hypixel.api.util.ILeveling
import zone.nora.simplestats.util.{Storage, QuestData, Utils}

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

/**
 * Gets the stats of a Hypixel player into a line buffer.
 *
 * @param api     HypixelAPI instance to use.
 * @param name    The name of the player to get stats of.
 * @param compact Save stats of a player compactly in one-line.
 */
//noinspection DuplicatedCode
class Stats(api: HypixelAPI, name: String, compact: Boolean = false) {

  // Player reply from Hypixel API
  val reply: PlayerReply = try {
    api.getPlayerByName(name).get()
  } catch {
    case _: Exception => null
  }

  // Player response JSON object from Hypixel API
  val player: JsonObject = try {
    reply.getPlayer
  } catch {
    case _: Exception => null
  }

  // Contains the messages to be printed to the console.
  val lines: ListBuffer[String] = new ListBuffer[String]

  /**
   * Print stats from the lines buffer into the minecraft chat.
   */
  def printStats(): Unit = {
    if (compact) { // Compact mode
      Utils.put(getStatsInOneLine, prefix = true)
    } else if (lines.nonEmpty) { // Detailed mode
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
    lines.indices.foreach { it => str.append(s"${lines(it)}\u00a7f ${if (it != lines.size - 1 && it != 0) "| " else ""}") }
    str.toString()
  }

  /**
   * Saves profile stats into the line buffer.
   */
  def saveStats(): Unit = {
    firstLine(player)
    if (player == null) {
      lines.append("Invalid player.")
      return
    }

    saveStatsToBuffer("Network Level", try {
      val exp = player.get("networkExp").getAsDouble
      val level = ILeveling.getLevel(exp).toInt
      if (compact) level else {
        val percent = (ILeveling.getPercentageToNextLevel(exp) * 100).toInt
        s"$level ($percent% to ${level + 1})"
      }
    } catch {
      case _: NullPointerException => 1
    })
    saveStatsToBuffer("AP", player.get("achievementPoints"))
    saveStatsToBuffer(if (compact) "Quests" else "Quests Completed", QuestData.getData(player.getAsJsonObject("quests")))
    if (!compact) { // Only included in detailed mode
      saveStatsToBuffer("Karma", player.get("karma"))
      saveStatsToBuffer("Discord", try {
        player.get("socialMedia").getAsJsonObject.get("links").getAsJsonObject.get("DISCORD").getAsString
      } catch {
        case _: NullPointerException => "\u00a7cN/A"
      })
      saveStatsToBuffer("Online", try {
        player.get("lastLogin").getAsLong > player.get("lastLogout").getAsLong
      } catch {
        case _: NullPointerException => false
      })
      saveStatsToBuffer("First Login", // https://steveridout.github.io/mongo-object-time/
        Utils.parseTime(new BigInteger(player.get("_id").getAsString.substring(0, 8), 16).longValue * 1000))
      saveStatsToBuffer("Last Login", try {
        Utils.parseTime(player.get("lastLogin").getAsLong)
      } catch {
        case _: NullPointerException => "\u00a7cHidden"
      })
      val uuid = player.get("uuid").getAsString
      if (Storage.contributors.contains(uuid)) {
        saveStatsToBuffer("Mod Contributor", true)
      } else if (Storage.cuties.contains(uuid)) {
        saveStatsToBuffer("Cutie \u2764", "\u00a7dtrue")
      }
    }
  }

  /**
   * Saves stats of a Hypixel player to the line buffer.
   *
   * @param game Name of the game.
   */
  def saveStats(game: String): Unit = {
    if (player == null) {
      firstLine(player)
      lines.append("Invalid player.")
      return
    }

    if (!player.has("stats")) {
      firstLine(player)
      lines.append("\u00a7cNo stats found.")
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
        saveStatsToBuffer("Coins", arcade.getStatsAsInt("coins"))
      case "ab" | "arena" | "arenabrawl" =>
        val arena = new StatsManager(player, "Arena")
        if (arena.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Arena Brawl")
        val wins = arena.getStatsAsInt("wins_1v1") + arena.getStatsAsInt("wins_2v2") + arena.getStatsAsInt("wins_4v4")
        val losses = arena.getStatsAsInt("losses_1v1") + arena.getStatsAsInt("losses_2v2") + arena.getStatsAsInt("losses_4v4")

        if (compact) {
          saveStatsToBuffer("Wins", wins)
          saveStatsToBuffer("Losses", losses)
        } else {
          saveStatsToBuffer("1v1 Wins", arena.getStatsAsInt("wins_1v1"))
          saveStatsToBuffer("2v2 Wins", arena.getStatsAsInt("wins_2v2"))
          saveStatsToBuffer("4v4 Wins", arena.getStatsAsInt("wins_4v4"))
          saveStatsToBuffer("Total wins", wins)
          saveStatsToBuffer("1v1 Losses", arena.getStatsAsInt("losses_1v1"))
          saveStatsToBuffer("2v2 Losses", arena.getStatsAsInt("losses_2v2"))
          saveStatsToBuffer("4v4 Losses", arena.getStatsAsInt("losses_4v4"))
          saveStatsToBuffer("Total losses", losses)
          saveStatsToBuffer("Coins", arena.getStatsAsInt("coins"))
          saveStatsToBuffer("Offensive Skill", arena.getStatsAsString("offensive").replace("_", " "))
          saveStatsToBuffer("Utility Skill", arena.getStatsAsString("utility").replace("_", " "))
          saveStatsToBuffer("Support Skill", arena.getStatsAsString("support").replace("_", " "))
          saveStatsToBuffer("Ultimate Skill", arena.getStatsAsString("ultimate").replace("_", " "))
        }
      case "wl" | "bg" | "warlords" | "battleground" =>
        val bg = new StatsManager(player, "Battleground")
        if (bg.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Warlords")
        if (compact) {
          saveStatsToBuffer("KDR", Utils.roundDouble(bg.getStatsAsDouble("kills") / bg.getStatsAsInt("deaths", one = true)))
          saveStatsToBuffer("(K+A)DR", Utils.roundDouble((bg.getStatsAsDouble("kills") + bg.getStatsAsDouble("assists")) / bg.getStatsAsInt("deaths", one = true)))
          saveStatsToBuffer("WS", bg.getStatsAsInt("win_streak"))
        } else {
          saveStatsToBuffer("Kills", bg.getStatsAsInt("kills"))
          saveStatsToBuffer("Assists", bg.getStatsAsInt("assists"))
          saveStatsToBuffer("Deaths", bg.getStatsAsInt("deaths"))
          saveStatsToBuffer("Wins", bg.getStatsAsInt("wins"))
          saveStatsToBuffer("Losses", bg.getStatsAsInt("losses"))
          saveStatsToBuffer("Winstreak", bg.getStatsAsInt("win_streak"))
          saveStatsToBuffer("Damage dealt", bg.getStatsAsLong("damage"))
          saveStatsToBuffer("Damage taken", bg.getStatsAsLong("damage_taken"))
          saveStatsToBuffer("Coins", bg.getStatsAsInt("coins"))
          saveStatsToBuffer("Mage Level", Utils.getWarlordsClassLevel(bg.stats, "mage"))
          saveStatsToBuffer("Paladin Level", Utils.getWarlordsClassLevel(bg.stats, "paladin"))
          saveStatsToBuffer("Shaman Level", Utils.getWarlordsClassLevel(bg.stats, "shaman"))
          saveStatsToBuffer("Warrior Level", Utils.getWarlordsClassLevel(bg.stats, "warrior"))
        }
      case "bw" | "bedwars" =>
        val bw = new StatsManager(player, "Bedwars")
        if (bw.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "BedWars")
        if (bw.achievements != null) {
          val level = bw.achievements.get("bedwars_level").getAsInt
          val colour = level match {
            case level if 100 to 199 contains level => 'f'
            case level if 200 to 299 contains level => '6'
            case level if 300 to 399 contains level => 'b'
            case level if 400 to 499 contains level => '2'
            case level if 500 to 599 contains level => '3'
            case level if 600 to 699 contains level => '4'
            case level if 700 to 799 contains level => 'd'
            case level if 800 to 899 contains level => '9'
            case level if 900 to 999 contains level => '5'
            case level if 1000 to 9999 contains level =>
              //val chars = level.toString.toCharArray
              val level_ = level.toString
              //s"6${chars(0)}\u00a7e${chars(1)}\u00a7a${chars(2)}\u00a7b${chars(3)}\u00a7d"
              s"6${level_.charAt(0)}\u00a7e${level_.charAt(1)}\u00a7a${level_.charAt(2)}\u00a7b${level_.charAt(3)}\u00a7d"
            case _ => 7
          }
          saveStatsToBuffer("Level", s"\u00a7$colour${if (colour.isInstanceOf[String]) "" else level}\u272b")
        }

        val wlr = Utils.roundDouble(bw.getStatsAsDouble("wins_bedwars") / bw.getStatsAsInt("losses_bedwars", one = true))
        val fkdr = Utils.roundDouble(bw.getStatsAsDouble("final_kills_bedwars") / bw.getStatsAsInt("final_deaths_bedwars", one = true))
        if (compact) {
          saveStatsToBuffer("WLR", wlr)
          saveStatsToBuffer("FKDR", fkdr)
          saveStatsToBuffer("WS", bw.getStatsAsInt("winstreak"))
        } else {
          saveStatsToBuffer("W | L", bw.getStatsAsInt("wins_bedwars") + " | " + bw.getStatsAsInt("losses_bedwars"))
          saveStatsToBuffer("WLR", wlr)
          saveStatsToBuffer("Final K | D", bw.getStatsAsInt("final_kills_bedwars") + " | " + bw.getStatsAsInt("final_deaths_bedwars"))
          saveStatsToBuffer("FKDR", fkdr)
          saveStatsToBuffer("Winstreak", bw.getStatsAsInt("winstreak"))
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
        if (!compact) {
          saveStatsToBuffer("Coins", bb.getStatsAsInt("coins"))
          saveStatsToBuffer("Games Played", bb.getStatsAsInt("games_played"))
          saveStatsToBuffer("Correct GTB Guesses", bb.getStatsAsInt("correct_guesses"))
        }
      case "duels" =>
        val duels = new StatsManager(player, "Duels")
        if (duels.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Duels")
        val wlr = Utils.roundDouble(duels.getStatsAsDouble("wins") / duels.getStatsAsInt("losses", one = true))
        val kdr = Utils.roundDouble(duels.getStatsAsDouble("kills") / duels.getStatsAsInt("deaths", one = true))
        if (compact) {
          saveStatsToBuffer("WLR", wlr)
          saveStatsToBuffer("KDR", kdr)
          saveStatsToBuffer("WS", duels.getStatsAsInt("current_winstreak"))
        } else {
          saveStatsToBuffer("Wins", duels.getStatsAsInt("wins"))
          saveStatsToBuffer("Losses", duels.getStatsAsInt("losses"))
          saveStatsToBuffer("WLR", wlr)
          saveStatsToBuffer("Kills", duels.getStatsAsInt("kills"))
          saveStatsToBuffer("Deaths", duels.getStatsAsInt("deaths"))
          saveStatsToBuffer("KDR", kdr)
          saveStatsToBuffer("WS", duels.getStatsAsInt("current_winstreak"))
          saveStatsToBuffer("Best WS", duels.getStatsAsInt("best_overall_winstreak"))
          saveStatsToBuffer("Melee Hits", duels.getStatsAsInt("melee_hits"))
          saveStatsToBuffer("Bow Hits", duels.getStatsAsInt("bow_hits"))
          saveStatsToBuffer("Coins", duels.getStatsAsInt("coins"))
        }
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
        if (!compact) {
          saveStatsToBuffer("Wins", tkr.getStatsAsInt("wins"))
          saveStatsToBuffer("Coins", tkr.getStatsAsInt("coins"))
        }
      case "blitz" | "bsg" | "sg" | "hg" | "hungergames" =>
        val bsg = new StatsManager(player, "HungerGames")
        if (bsg.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Blitz Survival Games")
        val kdr = Utils.roundDouble(bsg.getStatsAsDouble("kills") / bsg.getStatsAsInt("deaths", one = true))
        if (compact) {
          saveStatsToBuffer("Wins", bsg.getStatsAsInt("wins"))
          saveStatsToBuffer("KDR", kdr)
        } else {
          saveStatsToBuffer("Wins", bsg.getStatsAsInt("wins"))
          saveStatsToBuffer("Kills", bsg.getStatsAsInt("kills"))
          saveStatsToBuffer("Deaths", bsg.getStatsAsInt("deaths"))
          saveStatsToBuffer("KDR", kdr)
          saveStatsToBuffer("Damage dealt", bsg.getStatsAsInt("damage"))
          saveStatsToBuffer("Damage taken", bsg.getStatsAsInt("damage_taken"))
          saveStatsToBuffer("Coins", bsg.getStatsAsInt("coins"))
          saveStatsToBuffer("Default Kit", bsg.getStatsAsString("defaultkit"))
        }
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
        val dfKDR = Utils.roundDouble(cvc.getStatsAsDouble("kills") / cvc.getStatsAsInt("deaths", one = true))
        val dmKDR = Utils.roundDouble(cvc.getStatsAsDouble("kills_deathmatch") / cvc.getStatsAsInt("deaths_deathmatch", one = true))
        if (compact) {
          saveStatsToBuffer("Defusal KDR", dfKDR)
          saveStatsToBuffer("Defusal Wins", cvc.getStatsAsInt("game_wins"))
          saveStatsToBuffer("Deathmatch KDR", dmKDR)
          saveStatsToBuffer("Deathmatch Wins", cvc.getStatsAsInt("game_wins_deathmatch"))
        } else {
          saveStatsToBuffer("Defusal Kills", cvc.getStatsAsInt("kills"))
          saveStatsToBuffer("Defusal Round Wins", cvc.getStatsAsInt("round_wins"))
          saveStatsToBuffer("Defusal Game Wins", cvc.getStatsAsInt("game_wins"))
          saveStatsToBuffer("Defusal Deaths", cvc.getStatsAsInt("deaths"))
          saveStatsToBuffer("Defusal KDR", dfKDR)
          saveStatsToBuffer("Deathmatch Kills", cvc.getStatsAsInt("kills_deathmatch"))
          saveStatsToBuffer("Deathmatch Wins", cvc.getStatsAsInt("game_wins_deathmatch"))
          saveStatsToBuffer("Deathmatch Deaths", cvc.getStatsAsInt("deaths_deathmatch"))
          saveStatsToBuffer("Deathmatch KDR", dmKDR)
          saveStatsToBuffer("Coins", cvc.getStatsAsInt("coins"))
        }
      case "pb" | "paintball" =>
        val pb = new StatsManager(player, "Paintball")
        if (pb.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Paintball")
        val kdr = Utils.roundDouble(pb.getStatsAsDouble("kills") / pb.getStatsAsInt("deaths", one = true))
        if (compact) {
          saveStatsToBuffer("Wins", pb.getStatsAsInt("wins"))
          saveStatsToBuffer("KDR", kdr)
          saveStatsToBuffer("SF", pb.getStatsAsInt("shots_fired"))
        } else {
          saveStatsToBuffer("Kills", pb.getStatsAsInt("kills"))
          saveStatsToBuffer("Wins", pb.getStatsAsInt("wins"))
          saveStatsToBuffer("Deaths", pb.getStatsAsInt("deaths"))
          saveStatsToBuffer("Shots Fired", pb.getStatsAsInt("shots_fired"))
          saveStatsToBuffer("Coins", pb.getStatsAsInt("coins"))
        }
      case "p" | "pit" | "thepit" =>
        val pit = new StatsManager(player, "Pit", subkey = "pit_stats_ptl")
        val pit_ = new StatsManager(player, "Pit", "profile")
        if (pit.stats == null || pit_.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "The Pit")
        val kdr = pit.getStatsAsDouble("kills") / pit.getStatsAsInt("deaths")
        val kadr = (pit.getStatsAsDouble("kills") + pit.getStatsAsDouble("assists")) / pit.getStatsAsInt("deaths")
        if (compact) {
          saveStatsToBuffer("KDR", kdr)
          saveStatsToBuffer("(K+A)DR", kadr)
        } else {
          saveStatsToBuffer("Kills", pit.getStatsAsInt("kills"))
          saveStatsToBuffer("Assists", pit.getStatsAsInt("assists"))
          saveStatsToBuffer("Deaths", pit.getStatsAsInt("deaths"))
          saveStatsToBuffer("Gold", pit_.getStatsAsDouble("cash"))
          saveStatsToBuffer("Renown", pit_.getStatsAsInt("renown"))
          saveStatsToBuffer("Highest killstreak", pit.getStatsAsInt("max_streak"))
          saveStatsToBuffer("KDR", kdr)
          saveStatsToBuffer("Damage dealt", pit.getStatsAsInt("damage_dealt"))
          saveStatsToBuffer("Damage taken", pit.getStatsAsInt("damage_received"))
          if (pit_.has("genesis_allegiance")) {
            val s = pit_.getStatsAsString("genesis_allegiance")
            saveStatsToBuffer("Genesis Allegiance", if (s == "ANGEL") "\u00a7bAngel" else "\u00a7cDemon")
          }
          saveStatsToBuffer("Prestige", if (pit_.has("prestiges")) pit_.stats.getAsJsonArray("prestiges").size() else 0)
          saveStatsToBuffer("Pit Supporter", new StatsManager(player, "Pit").has("packages"))
        }
      case "q" | "quake" | "quakecraft" =>
        val q = new StatsManager(player, "Quake")
        if (q.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Quake")
        if (compact) {
          saveStatsToBuffer("Kills", q.getStatsAsInt("kills") + q.getStatsAsInt("kills_teams"))
          saveStatsToBuffer("Wins", q.getStatsAsInt("wins") + q.getStatsAsInt("wins_teams"))
        } else {
          saveStatsToBuffer("Solo Kills", q.getStatsAsInt("kills"))
          saveStatsToBuffer("Solo Wins", q.getStatsAsInt("wins"))
          saveStatsToBuffer("Teams Kills", q.getStatsAsInt("kills_teams"))
          saveStatsToBuffer("Teams Wins", q.getStatsAsInt("wins_teams"))
          saveStatsToBuffer("Coins", q.getStatsAsInt("coins"))
          saveStatsToBuffer("Trigger", q.getStatsAsString("trigger"))
        }
      case "sb" | "skyblock" => Utils.error("Stats lookup for SkyBlock are coming soon (tm)")
      case "sw" | "skywars" =>
        val sw = new StatsManager(player, "SkyWars")
        if (sw.stats == null) {
          api.shutdown()
          return
        }

        firstLine(player, "SkyWars")
        val swLevel = if (sw.has("levelFormatted")) {
          sw.stats.get("levelFormatted").getAsString
        } else {
          try { // https://hypixel.net/posts/19293045
            val swExp = sw.getStatsAsDouble("skywars_experience")
            val exps = 0 :: 20 :: 70 :: 150 :: 250 :: 500 :: 1000 :: 2000 :: 3500 :: 6000 :: 10000 :: 15000 :: Nil
            if (swExp >= 1500) (swExp - 15000) / 10000 + 12
            else for (i <- exps.indices) if (swExp < exps(i)) 1 + i + (swExp - exps(i - 1)) / (exps(i) - exps(i - 1))
          } catch {
            case e: Exception => e.printStackTrace(); 0
          }
        }

        val swlr = Utils.roundDouble(sw.getStatsAsDouble("wins_solo") / sw.getStatsAsInt("losses_solo", one = true))
        val owlr = Utils.roundDouble(sw.getStatsAsDouble("wins") / sw.getStatsAsInt("losses", one = true))
        val skdr = Utils.roundDouble(sw.getStatsAsDouble("kills_solo") / sw.getStatsAsInt("deaths_solo", one = true))
        val okdr = Utils.roundDouble(sw.getStatsAsDouble("kills") / sw.getStatsAsInt("deaths", one = true))
        if (compact) {
          saveStatsToBuffer("Level", swLevel)
          saveStatsToBuffer("WLR", owlr)
          saveStatsToBuffer("KDR", okdr)
        } else {
          saveStatsToBuffer("SkyWars Level", swLevel)
          saveStatsToBuffer("K | D", sw.getStatsAsInt("kills") + " | " + sw.getStatsAsInt("deaths"))
          saveStatsToBuffer("Solo KDR", skdr)
          saveStatsToBuffer("Overall KDR", okdr)
          saveStatsToBuffer("W | L", sw.getStatsAsInt("wins") + " | " + sw.getStatsAsInt("losses"))
          saveStatsToBuffer("Solo WLR", swlr)
          saveStatsToBuffer("Overall WLR", owlr)
          saveStatsToBuffer("Winstreak", sw.getStatsAsInt("win_streak"))
          saveStatsToBuffer("Coins", sw.getStatsAsInt("coins"))
          saveStatsToBuffer("Souls", sw.getStatsAsInt("souls"))
          saveStatsToBuffer("Heads", sw.getStatsAsInt("heads"))
        }
      case "sc" | "skyclash" => // Not really maintained due to its removal
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
        val kdr = Utils.roundDouble(suhc.getStatsAsDouble("kills") / suhc.getStatsAsInt("deaths", one = true))
        if (compact) {
          saveStatsToBuffer("KDR", kdr)
          saveStatsToBuffer("Wins", suhc.getStatsAsInt("wins"))
          saveStatsToBuffer("WS", suhc.getStatsAsInt("winstreak"))
        } else {
          saveStatsToBuffer("Wins", suhc.getStatsAsInt("wins"))
          saveStatsToBuffer("Kills", suhc.getStatsAsInt("kills"))
          saveStatsToBuffer("Deaths", suhc.getStatsAsInt("deaths"))
          saveStatsToBuffer("KDR", kdr)
          saveStatsToBuffer("Coins", suhc.getStatsAsInt("coins"))
          saveStatsToBuffer("Winstreak", suhc.getStatsAsInt("winstreak"))
          try {
            saveStatsToBuffer("Selected Mastery", suhc.getStatsAsString("activeMasteryPerk").split("_")(1))
          } catch {
            case _: Exception => /* ignored */
          }
        }
      case "sh" | "smash" | "supersmash" | "smashheroes" =>
        val sh = new StatsManager(player, "SuperSmash")
        if (sh.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Smash Heroes")
        if (compact) {
          saveStatsToBuffer("Wins", sh.getStatsAsInt("wins"))
          saveStatsToBuffer("Kills", sh.getStatsAsInt("kills"))
        } else {
          saveStatsToBuffer("Kills", sh.getStatsAsInt("kills"))
          saveStatsToBuffer("Wins", sh.getStatsAsInt("wins"))
          saveStatsToBuffer("Deaths", sh.getStatsAsInt("deaths"))
          saveStatsToBuffer("Coins", sh.getStatsAsInt("coins"))
          saveStatsToBuffer("Smash Level", s"${sh.getStatsAsInt("smashLevel")}\u272b")
          val activeClass = sh.getStatsAsString("active_class")
          val prestige = if (sh.stats.has(s"pg_$activeClass")) sh.getStatsAsInt(s"pg_$activeClass") else 0
          val level = if (sh.stats.has(s"lastLevel_$activeClass")) sh.getStatsAsInt(s"lastLevel_$activeClass") else 0
          saveStatsToBuffer("Active Class", s"$activeClass (P$prestige Lv$level)")
        }
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
        if (!compact)
          saveStatsToBuffer("Coins", tnt.getStatsAsInt("coins"))
      case "tc" | "cw" | "crazywalls" | "truecombat" =>
        val cw = new StatsManager(player, "TrueCombat")
        if (cw.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Crazy Walls")
        if (compact) {
          saveStatsToBuffer("Wins", cw.getStatsAsInt("wins"))
          saveStatsToBuffer("KDR", Utils.roundDouble(cw.getStatsAsInt("kills") / cw.getStatsAsInt("deaths", one = true)))
        } else {
          saveStatsToBuffer("Kills", cw.getStatsAsInt("kills"))
          saveStatsToBuffer("Wins", cw.getStatsAsInt("wins"))
          saveStatsToBuffer("Deaths", cw.getStatsAsInt("deaths"))
          saveStatsToBuffer("Coins", cw.getStatsAsInt("coins"))
          saveStatsToBuffer("Golden Skulls", cw.getStatsAsInt("golden_skulls"))
          saveStatsToBuffer("Gold Dust", cw.getStatsAsInt("gold_dust"))
        }
      case "uhc" =>
        val uhc = new StatsManager(player, "UHC")
        if (uhc.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "UHC")
        val kdr = Utils.roundDouble(uhc.getStatsAsDouble("kills") / uhc.getStatsAsInt("deaths", one = true))
        if (compact) {
          saveStatsToBuffer("Wins", uhc.getStatsAsInt("wins"))
          saveStatsToBuffer("KDR", kdr)
        } else {
          saveStatsToBuffer("Kills", uhc.getStatsAsInt("kills"))
          saveStatsToBuffer("Deaths", uhc.getStatsAsInt("deaths"))
          saveStatsToBuffer("KDR", kdr)
          saveStatsToBuffer("Wins", uhc.getStatsAsInt("wins"))
          saveStatsToBuffer("Coins", uhc.getStatsAsInt("coins"))
          saveStatsToBuffer("Score", uhc.getStatsAsInt("score"))
          saveStatsToBuffer("Heads Eaten", uhc.getStatsAsInt("heads_eaten"))
          try {
            saveStatsToBuffer("Selected Kit", uhc.getStatsAsString("equippedKit"))
          } catch {
            case _: Exception => /* ignored */
          }
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
        if (!compact)
          saveStatsToBuffer("Coins", vz.getStatsAsInt("coins"))
      case "walls" =>
        val walls = new StatsManager(player, "Walls")
        if (walls.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Walls")
        if (compact) {
          saveStatsToBuffer("KDR", Utils.roundDouble(walls.getStatsAsDouble("kills") / walls.getStatsAsInt("deaths", one = true)))
          saveStatsToBuffer("WLR", Utils.roundDouble(walls.getStatsAsDouble("wins") / walls.getStatsAsInt("losses", one = true)))
        } else {
          saveStatsToBuffer("Kills", walls.getStatsAsInt("kills"))
          saveStatsToBuffer("Wins", walls.getStatsAsInt("wins"))
          saveStatsToBuffer("Losses", walls.getStatsAsInt("losses"))
          saveStatsToBuffer("Coins", walls.getStatsAsInt("coins"))
          saveStatsToBuffer("Insane Farmer", walls.stats.get("insane_farmer"))
        }
      case "walls3" | "mw" | "mega" | "megawalls" =>
        val mw = new StatsManager(player, "Walls3")
        if (mw.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Mega Walls")
        val fkdr = Utils.roundDouble(mw.getStatsAsDouble("total_final_kills") / mw.getStatsAsInt("final_deaths", one = true))
        val wlr = Utils.roundDouble(mw.getStatsAsDouble("wins") / mw.getStatsAsInt("losses", one = true))
        if (compact) {
          saveStatsToBuffer("fkdr", fkdr)
          saveStatsToBuffer("wlr", wlr)
        } else {
          saveStatsToBuffer("Kills", mw.getStatsAsInt("kills"))
          saveStatsToBuffer("W", mw.getStatsAsInt("wins"))
          saveStatsToBuffer("L", mw.getStatsAsInt("losses"))
          saveStatsToBuffer("WLR", wlr)
          saveStatsToBuffer("FKs", mw.getStatsAsInt("total_final_kills"))
          saveStatsToBuffer("FDs", mw.getStatsAsInt("final_deaths"))
          saveStatsToBuffer("FKDR", fkdr)
          saveStatsToBuffer("Selected Class", mw.getStatsAsString("chosen_class"))
        }
      case "mm" | "murder" | "murdermystery" =>
        val mm = new StatsManager(player, "MurderMystery")
        if (mm.stats == null) {
          api.shutdown()
          return
        }
        firstLine(player, "Murder Mystery")
        if (compact) {
          saveStatsToBuffer("Wins", mm.getStatsAsInt("wins"))
          saveStatsToBuffer("Kills", mm.getStatsAsInt("kills"))
          saveStatsToBuffer("Murderer Chance", s"${mm.stats.get("murderer_chance")}%")
          saveStatsToBuffer("Detective Chance", s"${mm.stats.get("detective_chance")}%")
        } else {
          saveStatsToBuffer("Murderer Wins", mm.getStatsAsInt("murderer_wins"))
          saveStatsToBuffer("Detective Wins", mm.getStatsAsInt("detective_wins"))
          saveStatsToBuffer("Assassins Wins", mm.getStatsAsInt("wins_MURDER_ASSASSINS"))
          saveStatsToBuffer("Infection Wins", mm.getStatsAsInt("wins_MURDER_INFECTION"))
          saveStatsToBuffer("Murderer Chance", s"${mm.stats.get("murderer_chance")}%")
          saveStatsToBuffer("Detective Chance", s"${mm.stats.get("detective_chance")}%")
          saveStatsToBuffer("Murderer Kills", mm.getStatsAsInt("kills_as_murderer"))
          saveStatsToBuffer("Coins", mm.getStatsAsInt("coins"))
        }
      case "status" | "session" | "s" | "online" =>
        val statusReply = api.getStatus(Utils.addDashes(player.get("uuid").getAsString)).get()
        if (!statusReply.isSuccess || statusReply.getSession == null) {
          Utils.error(s"Unexpected error getting ${player.get("displayname").getAsString}'s online status:'", prefix = true)
          Utils.error(statusReply.getCause)
          api.shutdown()
          return
        }
        val status = statusReply.getSession
        firstLine(player, status = true)
        saveStatsToBuffer("Online", status.isOnline)
        if (status.isOnline) {
          saveStatsToBuffer("Game", status.getGameType.getName)
          if (!compact) {
            saveStatsToBuffer("Mode", status.getMode)
            saveStatsToBuffer("Map", status.getMap)
          }
        }
      case "g" | "guild" =>
        val guildReply = api.getGuildByPlayer(player.get("uuid").getAsString).get()
        if (!guildReply.isSuccess) {
          Utils.error(s"Unexpected error getting ${player.get("displayname").getAsString}'s Guild:'", prefix = true)
          Utils.error(guildReply.getCause)
          api.shutdown()
          return
        }
        val guild = guildReply.getGuild
        if (guild == null) {
          Utils.error(s"${player.get("displayname").getAsString} is not in a guild.", prefix = true)
          api.shutdown()
          return
        }
        firstLine(player, guild = true)
        saveStatsToBuffer("Name", guild.getName)
        saveStatsToBuffer("Level", Utils.getGuildLevel(guild.getExp))
        try {
          saveStatsToBuffer("Tag", try {
            s"${Storage.colourNameToCode(guild.getTagColor.toLowerCase)}[${guild.getTag}]"
          } catch {
            case _: NullPointerException => s"[${guild.getTag}]"
          })
        } catch {
          case NonFatal(_) => // nothing lol
        }
        if (!compact) {
          saveStatsToBuffer("Joinable", guild.getJoinable)
          saveStatsToBuffer("Legacy Rank", guild.getLegacyRanking)
          saveStatsToBuffer("Created", Utils.parseTime(Timestamp.valueOf(guild.getCreated.toLocalDateTime).getTime))
          saveStatsToBuffer("Members", s"${guild.getMembers.size}/125")
        }
      case _ =>
        Utils.breakLine()
        Utils.error(s"$game is not a valid game.")
        Utils.error("Try one of these:")
        List(
          "arcade", "arenabrawl", "warlords", "bedwars", "duels", "tkr", "blitz", "legacy", "cvc", "paintball",
          "quake", "skywars", "skyclash", "speeduhc", "smash", "tnt", "crazywalls", "uhc", "vampirez", "walls",
          "megawalls", "murdermystery", "pit", "status", "guild"
        ).foreach { it => Utils.put(s"\u00a78- \u00a7a$it") }
        Utils.breakLine()
    }
  }

  /**
   * Saves stats value into line buffer in a key-value format.
   */
  private def saveStatsToBuffer(name: String, value: Any): Unit = {
    if (value == null) return
    if (value.isInstanceOf[String] && value.toString.isEmpty) return
    lines.append(Utils.formatStat(name, value))
  }

  /**
   * Prints the first line showing the name/guild of the player.
   */
  def firstLine(player: JsonObject, game: String = "", guild: Boolean = false, status: Boolean = false): Unit = {
    val rank = try Utils.getRank(player)
    catch {
      case e: Exception => e.printStackTrace(); ""
    }

    val name: String = if (rank.endsWith("]")) rank + " " + player.get("displayname").getAsString
    else rank + player.get("displayname").getAsString

    if (compact) lines.append(name)
    else {
      val str =
        if (game == "" && !guild) "Stats" else if (guild) "Guild" else if (status) "Online status" else s"$game stats"
      lines.append(s"$str of $name")
    }
  }
}
