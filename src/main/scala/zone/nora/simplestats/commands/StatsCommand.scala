package zone.nora.simplestats.commands

import java.util.UUID

import com.google.gson.JsonObject
import net.hypixel.api.HypixelAPI
import net.hypixel.api.util.ILeveling
import net.minecraft.command.{CommandBase, ICommandSender}
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.util.Utils

class StatsCommand extends CommandBase {
  override def getCommandName: String = "stats"

  override def getCommandUsage(sender: ICommandSender): String = "/stats [player]"

  //override def getCommandAliases: util.List[String] = ???

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        if (!SimpleStats.validKey) {
          Utils.breakline()
          Utils.error("Your api key is not valid :(")
          Utils.error("You can set your api key with '/setkey [Hypixel API Key]")
          Utils.error("You can get your API key by typing \"/api new\" in a Hypixel lobby.")
          Utils.breakline()
          return
        }
        if (args.isEmpty) {
          Utils.error("/stats [player]")
          return
        }

        val api = new HypixelAPI(UUID.fromString(SimpleStats.apiKey))
        val playerReply = api.getPlayerByName(args(0)).get()
        if (!playerReply.isSuccess) {
          Utils.error(s"Unexpected Error: ${playerReply.getCause}")
          api.shutdown()
          return
        }
        val player = playerReply.getPlayer
        if (player == null) {
          Utils.error("Invalid player.")
          api.shutdown()
          return
        }

        if (args.length == 1) {
          Utils.breakline()
          firstLine(player)
          printStat("Hypixel Level", try {
            ILeveling.getLevel(player.get("networkExp").getAsDouble)
          } catch {
            case _: NullPointerException => "1"
          })
          printStat("Achievement Points", player.get("achievementPoints"))
          printStat("Karma", player.get("karma"))
          printStat("Discord", try {
            player.get("socialMedia").getAsJsonObject.get("links").getAsJsonObject.get("DISCORD").getAsString
          } catch {
            case _: NullPointerException => "N/A"
          })
          printStat("First Login", Utils.parseTime(player.get("firstLogin").getAsLong))
          printStat("Last Login", try {
            Utils.parseTime(player.get("lastLogin").getAsLong)
          } catch {
            case _: NullPointerException => "Hidden"
          })
          printStat("Online", try {
            player.get("lastLogin").getAsLong > player.get("lastLogout").getAsLong
          } catch {
            case _: NullPointerException => false
          })
          Utils.breakline()
        } else {
          if (!player.has("stats")) {
            Utils.error("This player has manually hidden their stats :(")
          }
          Utils.breakline()
          args(1).toLowerCase match {
            case "arc" | "arcade" =>
              val arcade = getGameStats(player, "Arcade")
              if (arcade == null) return
              firstLine(player, "Arcade")
              printStat("Coins", arcade.get("coins").getAsInt)
            case "ab" | "arena" | "arenabrawl" =>
              val arena = getGameStats(player, "Arena")
              if (arena == null) return
              firstLine(player, "Arena Brawl")
              printStat("1v1 Wins", arena.get("wins_ffa"))
              printStat("2v2 Wins", arena.get("wins_2v2"))
              printStat("4v4 Wins", arena.get("wins_4v4"))
              printStat("1v1 Losses", arena.get("losses_ffa"))
              printStat("2v2 Losses", arena.get("losses_2v2"))
              printStat("4v4 Losses", arena.get("losses_4v4"))
              printStat("Coins", arena.get("coins"))
              try {
                printStat("Offensive Skill", arena.get("offensive").getAsString.replace("_", " "))
                printStat("Utility Skill", arena.get("utility").getAsString.replace("_", " "))
                printStat("Support Skill", arena.get("support").getAsString.replace("_", " "))
                printStat("Ultimate Skill", arena.get("ultimate").getAsString.replace("_", " "))
              } catch { case _: NullPointerException => /* ignored */ }
            case "wl" | "bg" | "warlords" | "battleground" =>
              val bg = getGameStats(player, "Battleground")
              if (bg == null) return
              firstLine(player, "Warlords")
              printStat("Kills", bg.get("kills"))
              printStat("Assists", bg.get("assists"))
              printStat("Wins", bg.get("wins"))
              printStat("Losses", bg.get("losses"))
              printStat("Coins", bg.get("coins"))
              val achievements = player.get("achievements").getAsJsonObject
              printStat("Mage Level", Utils.getWarlordsClassLevel(bg, "mage"))
              printStat("Paladin Level", Utils.getWarlordsClassLevel(bg, "paladin"))
              printStat("Shaman Level", Utils.getWarlordsClassLevel(bg, "shaman"))
              printStat("Warrior Level", Utils.getWarlordsClassLevel(bg, "warrior"))
            case "bw" | "bedwars" =>
              val bw = getGameStats(player, "Bedwars")
              if (bw == null) return
              val achievements = player.get("achievements").getAsJsonObject
              firstLine(player, "BedWars")
              printStat("Level", achievements.get("bedwars_level"))
              printStat("Kills", bw.get("kills_bedwars"))
              printStat("Wins", bw.get("wins_bedwars"))
              printStat("Losses", bw.get("losses_bedwars"))
              printStat("Final Kills", bw.get("final_kills_bedwars"))
              printStat("Final Deaths", bw.get("final_deaths_bedwars"))
              printStat("Winstreak", bw.get("winstreak"))
              printStat("Coins", bw.get("coins"))
            case "bb" | "build" | "buildbattle" =>
              val bb = getGameStats(player, "BuildBattle")
              if (bb == null) return
              firstLine(player, "Build Battle")
              printStat("Wins", bb.get("wins"))
              printStat("Score", bb.get("score"))
              printStat("Coins", bb.get("coins"))
              printStat("Games Played", bb.get("games_played"))
              printStat("Correct GTB Guesses", bb.get("correct_guesses"))
            case "duels" =>
              val duels = getGameStats(player, "Duels")
              if (duels == null) return
              firstLine(player, "Duels")
              printStat("Wins", duels.get("wins"))
              printStat("Losses", duels.get("losses"))
              printStat("Coins", duels.get("coins"))
              printStat("Melee Hits", duels.get("melee_hits"))
              printStat("Bow Hits", duels.get("bow_hits"))
              printStat("Winstreak", duels.get("current_winstreak"))
            case "tkr" | "turbo" | "turbokartracers" | "gingerbread" =>
              val tkr = getGameStats(player, "GingerBread")
              if (tkr == null) return
              firstLine(player, "Turbo Kart Racers")
              printStat("Gold Trophies", tkr.get("gold_trophy"))
              printStat("Silver Trophies", tkr.get("silver_trophy"))
              printStat("Bronze Trophies", tkr.get("bronze_trophy"))
              printStat("Coins", tkr.get("coins"))
            case "blitz" | "bsg" | "sg" | "hg" | "hungergames" =>
              val bsg = getGameStats(player, "HungerGames")
              if (bsg == null) return
              firstLine(player, "Blitz Survival Games")
              printStat("Kills", bsg.get("kills"))
              printStat("Wins", bsg.get("wins"))
              printStat("Deaths", bsg.get("Deaths"))
              printStat("Coins", bsg.get("coins"))
              printStat("Default Kit", bsg.get("defaultkit"))
            case "legacy" | "classic" | "classiclobby" =>
              val legacy = getGameStats(player, "Legacy")
              if (legacy == null) return
              firstLine(player, "Classic Lobby")
              printStat("Current Tokens", legacy.get("tokens"))
              printStat("Lifetime Tokens", legacy.get("total_tokens"))
            case "cvc" | "cac" | "copsandcrims" | "mcgo" =>
              val cvc = getGameStats(player, "MCGO")
              if (cvc == null) return
              firstLine(player, "CVC")
              printStat("Defusal Kills", cvc.get("kills"))
              printStat("Defusal Round Wins", cvc.get("round_wins"))
              printStat("Defusal Game Wins", cvc.get("game_wins"))
              printStat("Defusal Deaths", cvc.get("deaths"))
              printStat("Deathmatch Kills", cvc.get("kills_deathmatch"))
              printStat("Deathmatch Wins", cvc.get("game_wins_deathmatch"))
              printStat("Deathmatch Deaths", cvc.get("deaths_deathmatch"))
              printStat("Coins", cvc.get("coins"))
            case "pb" | "paintball" =>
              val pb = getGameStats(player, "Paintball")
              if (pb == null) return
              firstLine(player, "Paintball")
              printStat("Kills", pb.get("kills"))
              printStat("Wins", pb.get("wins"))
              printStat("Deaths", pb.get("deaths"))
              printStat("Coins", pb.get("coins"))
              printStat("Shots Fired", pb.get("shots_fired"))
            case "pit" => Utils.error("Stats lookup for The Pit is coming soon (tm)")
            case "q" | "quake" | "quakecraft" =>
              val q = getGameStats(player, "Quake")
              if (q == null) return
              firstLine(player, "Quake")
              printStat("Solo Kills", q.get("kills"))
              printStat("Solo Wins", q.get("wins"))
              printStat("Teams Kills", q.get("kills_teams"))
              printStat("Teams Wins", q.get("wins_teams"))
              printStat("Coins", q.get("coins"))
              printStat("Trigger", q.get("trigger"))
            case "sb" | "skyblock" => Utils.error("Stats lookup for SkyBlock are coming soon (tm)")
            case "sw" | "skywars" =>
              val sw = getGameStats(player, "SkyWars")
              if (sw == null) return
              firstLine(player, "SkyWars")
              printStat("Kills", sw.get("kills"))
              printStat("Wins", sw.get("wins"))
              printStat("Deaths", sw.get("deaths"))
              // https://hypixel.net/posts/19293045
              val swLevel = try {
                val swExp = sw.get("skywars_experience").getAsInt.toDouble
                val exps = List(0, 20, 70, 150, 250, 500, 1000, 2000, 3500, 6000, 10000, 15000)
                if (swExp >= 15000) (swExp - 15000) / 10000 + 12
                else for (i <- 0 to exps.size) if (swExp < exps(i)) 1 + i + (swExp - exps(i) / (exps(i) - exps(i - 1)))
              } catch {
                case _: Exception => "Error"
              }
              printStat("SkyWars Level", swLevel)
              printStat("Coins", sw.get("coins"))
              printStat("Souls", sw.get("souls"))
              printStat("Heads", sw.get("heads"))
            case "suhc" | "speed" | "speeduhc" =>
              val suhc = getGameStats(player, "SpeedUHC")
              if (suhc == null) return
              firstLine(player, "Speed UHC")
              printStat("Kills", suhc.get("kills"))
              printStat("Wins", suhc.get("deaths"))
              printStat("Deaths", suhc.get("deaths"))
              printStat("Coins", suhc.get("coins"))
              try {
                printStat("Selected Mastery", suhc.get("activeMasteryPerk").getAsString.split("_")(1))
              } catch { case _: Exception => /* ignored */ }
              printStat("Winstreak", suhc.get("winstreak"))
            case "sh" | "smash" | "supersmash" | "smashheroes" =>
              val sh = getGameStats(player, "SuperSmash")
              if (sh == null) return
              firstLine(player, "Smash Heroes")
              printStat("Kills", sh.get("kills"))
              printStat("Wins", sh.get("wins"))
              printStat("Deaths", sh.get("deaths"))
              printStat("Smash Level", sh.get("smashLevel"))
              val activeClass = sh.get("active_class").getAsString
              val prestige = if (sh.has(s"pg_$activeClass")) sh.get(s"pg_$activeClass").getAsInt else 0
              val level = if (sh.has(s"lastLevel_$activeClass")) sh.get(s"lastLevel_$activeClass") else 0
              printStat("Active Class", s"$activeClass (P$prestige Lv$level)")
            case "tnt" | "tntgames" =>
              val tnt = getGameStats(player, "TNTGames")
              if (tnt == null) return
              firstLine(player, "TNT Games")
              printStat("TNT Run Wins", tnt.get("wins_tntrun"))
              printStat("PVP Run Wins", tnt.get("win_pvprun"))
              printStat("Bowspleef Wins", tnt.get("wins_bowspleef"))
              printStat("TNT Wizards Wins", tnt.get("wins_capture"))
              printStat("TNT Tag Wins", tnt.get("wins_tnttag"))
              printStat("Coins", tnt.get("coins"))
            case "tc" | "cw" | "crazywalls" | "truecombat" =>
              val cw = getGameStats(player, "TrueCombat")
              if (cw == null) return
              firstLine(player, "Crazy Walls")
              printStat("Kills", cw.get("kills"))
              printStat("Wins", cw.get("wins"))
              printStat("Deaths", cw.get("deaths"))
              printStat("Coins", cw.get("coins"))
              printStat("Golden Skulls", cw.get("golden_skulls"))
              printStat("Gold Dust", cw.get("gold_dust"))
            case "uhc" =>
              val uhc = getGameStats(player, "UHC")
              if (uhc == null) return
              firstLine(player, "UHC")
              printStat("Kills", uhc.get("kills"))
              printStat("Wins", uhc.get("wins"))
              printStat("Deaths", uhc.get("deaths"))
              printStat("Coins", uhc.get("coins"))
              printStat("Score", uhc.get("score"))
              printStat("Heads Eaten", uhc.get("heads_eaten"))
              try {
                printStat("Selected Kit", uhc.get("equippedKit").getAsString)
              } catch { case _: Exception => /* ignored */ }
            case "vz" | "vampz" | "vampirez" =>
              val vz = getGameStats(player, "VampireZ")
              if (vz == null) return
              firstLine(player, "VampireZ")
              printStat("Human Wins", vz.get("human_wins"))
              printStat("Vampire Wins", vz.get("vampire_wins"))
              printStat("Zombie Kills", vz.get("zombie_kills"))
              printStat("Coins", vz.get("coins"))
            case "walls" =>
              val walls = getGameStats(player, "Walls")
              if (walls == null) return
              firstLine(player, "Walls")
              printStat("Kills", walls.get("kills"))
              printStat("Wins", walls.get("wins"))
              printStat("Losses", walls.get("losses"))
              printStat("Coins", walls.get("coins"))
              printStat("Insane Farmer", walls.get("insane_farmer"))
            case "walls3" | "mw" | "mega" | "megawalls" =>
              val mw = getGameStats(player, "Walls3")
              if (mw == null) return
              firstLine(player, "Mega Walls")
              printStat("Kills", mw.get("kills"))
              printStat("Wins", mw.get("wins"))
              printStat("Losses", mw.get("losses"))
              printStat("Finals", mw.get("total_final_kills"))
              printStat("Final Deaths", mw.get("final_deaths"))
              printStat("Selected Class", mw.get("chosen_class"))
            case _ => Utils.error(s"${args(1)} is not a valid game.")
          }
          Utils.breakline()
        }
        api.shutdown()
      }
    })
    thread.start()
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = true

  private def getGameStats(player: JsonObject, game: String): JsonObject = try {
    player.get("stats").getAsJsonObject.get(game).getAsJsonObject
  } catch {
    case _: Exception => {
      Utils.error(s"${player.get("displayname").getAsString} has no stats in $game")
      Utils.breakline()
      null
    }
  }

  private def firstLine(player: JsonObject, game: String = ""): Unit = {
    val s = if (game == "") "S" else s"$game s"
    val rank = try { Utils.getRank(player) } catch { case e: Exception => {
      e.printStackTrace()
      ""
    } }
    Utils.put(s"${s}tats of ${if (rank.endsWith("]")) s"$rank " else rank}${player.get("displayname").getAsString}")
  }

  private def printStat(name: String, value: Any): Unit = Utils.put(s"$name: ${if (value == null) "N/A" else value}")
}
