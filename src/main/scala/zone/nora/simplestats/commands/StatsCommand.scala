package zone.nora.simplestats.commands

import java.math.BigInteger
import java.sql.Timestamp
import java.util.UUID

import com.google.gson.{JsonElement, JsonObject}
import net.hypixel.api.HypixelAPI
import net.hypixel.api.util.ILeveling
import net.minecraft.client.Minecraft
import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.event.ClickEvent
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.util.{ChatComponentBuilder, Utils}

import scala.collection.mutable.ListBuffer

class StatsCommand extends CommandBase {
  private val lines: ListBuffer[String] = new ListBuffer

  override def getCommandName: String = "stats"

  override def getCommandUsage(sender: ICommandSender): String = "/stats [player]"

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    lines.clear()
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
        if (args.isEmpty) { Utils.error("/stats [player] [game]", prefix = true); return }

        val api = new HypixelAPI(UUID.fromString(SimpleStats.apiKey))
        val playerReply = api.getPlayerByName(args(0)).get()
        if (!playerReply.isSuccess) {
          Utils.error(s"Unexpected Error: ${playerReply.getCause}", prefix = true)
          api.shutdown()
          return
        }

        val player = playerReply.getPlayer
        if (player == null) { Utils.error("Invalid player.", prefix = true); api.shutdown(); return }

        if (args.length == 1) {
          firstLine(player)
          printStat("Hypixel Level", try {
            ILeveling.getLevel(player.get("networkExp").getAsDouble).toInt
          } catch {
            case _: NullPointerException => 1
          })
          printStat("Achievement Points", player.get("achievementPoints"))
          printStat("Karma", player.get("karma"))
          printStat("Discord", try {
            player.get("socialMedia").getAsJsonObject.get("links").getAsJsonObject.get("DISCORD").getAsString
          } catch {
            case _: NullPointerException => null
          })
          // https://steveridout.github.io/mongo-object-time/
          printStat("First Login",
            Utils.parseTime(new BigInteger(player.get("_id").getAsString.substring(0, 8), 16).longValue*1000))
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
          lines.foreach { it => Utils.put(it) }
          Utils.breakline()
        } else {
          if (!player.has("stats")) {
            Utils.error("This player has manually hidden their stats :(")
          }
          //Utils.breakline()
          var b = true
          var g = (false, "")
          args(1).toLowerCase match {
            case "arc" | "arcade" =>
              val arcade = getGameStats(player, "Arcade")
              if (arcade == null) { api.shutdown(); return }
              firstLine(player, "Arcade")
              printStat("Coins", arcade.get("coins").getAsInt)
            case "ab" | "arena" | "arenabrawl" =>
              val arena = getGameStats(player, "Arena")
              if (arena == null) { api.shutdown(); return }
              firstLine(player, "Arena Brawl")
              printStat("1v1 Wins", arena.get("wins_1v1"))
              printStat("2v2 Wins", arena.get("wins_2v2"))
              printStat("4v4 Wins", arena.get("wins_4v4"))
              printStat("1v1 Losses", arena.get("losses_1v1"))
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
              if (bg == null) { api.shutdown(); return }
              firstLine(player, "Warlords")
              printStat("Kills", bg.get("kills"))
              printStat("Assists", bg.get("assists"))
              printStat("Wins", bg.get("wins"))
              printStat("Losses", bg.get("losses"))
              printStat("Coins", bg.get("coins"))
              printStat("Mage Level", Utils.getWarlordsClassLevel(bg, "mage"))
              printStat("Paladin Level", Utils.getWarlordsClassLevel(bg, "paladin"))
              printStat("Shaman Level", Utils.getWarlordsClassLevel(bg, "shaman"))
              printStat("Warrior Level", Utils.getWarlordsClassLevel(bg, "warrior"))
            case "bw" | "bedwars" =>
              val bw = getGameStats(player, "Bedwars")
              if (bw == null) { api.shutdown(); return }
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
              if (bb == null) { api.shutdown(); return }
              firstLine(player, "Build Battle")
              printStat("Wins", bb.get("wins"))
              printStat("Score", bb.get("score"))
              printStat("Coins", bb.get("coins"))
              printStat("Games Played", bb.get("games_played"))
              printStat("Correct GTB Guesses", bb.get("correct_guesses"))
            case "duels" =>
              val duels = getGameStats(player, "Duels")
              if (duels == null) { api.shutdown(); return }
              firstLine(player, "Duels")
              printStat("Wins", duels.get("wins"))
              printStat("Losses", duels.get("losses"))
              printStat("Coins", duels.get("coins"))
              printStat("Melee Hits", duels.get("melee_hits"))
              printStat("Bow Hits", duels.get("bow_hits"))
              printStat("Winstreak", duels.get("current_winstreak"))
            case "tkr" | "turbo" | "turbokartracers" | "gingerbread" =>
              val tkr = getGameStats(player, "GingerBread")
              if (tkr == null) { api.shutdown(); return }
              firstLine(player, "Turbo Kart Racers")
              printStat("Gold Trophies", tkr.get("gold_trophy"))
              printStat("Silver Trophies", tkr.get("silver_trophy"))
              printStat("Bronze Trophies", tkr.get("bronze_trophy"))
              printStat("Coins", tkr.get("coins"))
            case "blitz" | "bsg" | "sg" | "hg" | "hungergames" =>
              val bsg = getGameStats(player, "HungerGames")
              if (bsg == null) { api.shutdown(); return }
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
              if (cvc == null) { api.shutdown(); return }
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
              if (pb == null) { api.shutdown(); return }
              firstLine(player, "Paintball")
              printStat("Kills", pb.get("kills"))
              printStat("Wins", pb.get("wins"))
              printStat("Deaths", pb.get("deaths"))
              printStat("Coins", pb.get("coins"))
              printStat("Shots Fired", pb.get("shots_fired"))
            case "pit" => Utils.error("Stats lookup for The Pit is coming soon (tm)")
            case "q" | "quake" | "quakecraft" =>
              val q = getGameStats(player, "Quake")
              if (q == null) { api.shutdown(); return }
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
              if (sw == null) { api.shutdown(); return }
              firstLine(player, "SkyWars")
              printStat("Kills", sw.get("kills"))
              printStat("Wins", sw.get("wins"))
              printStat("Deaths", sw.get("deaths"))
              // https://hypixel.net/posts/19293045
              val swLevel = try {
                val swExp = sw.get("skywars_experience").getAsInt.toDouble
                val exps = 0 :: 20 :: 70 :: 150 :: 250 :: 500 :: 1000 :: 2000 :: 3500 :: 6000 :: 10000 :: 15000 :: Nil
                if (swExp >= 1500) (swExp - 15000) / 10000 + 12
                else for (i <- exps.indices) if (swExp < exps(i)) 1 + i + (swExp - exps(i - 1)) / (exps(i) - exps(i - 1))
              } catch {
                case e: Exception => e.printStackTrace(); "Error"
              }
              printStat("SkyWars Level", swLevel)
              printStat("Coins", sw.get("coins"))
              printStat("Souls", sw.get("souls"))
              printStat("Heads", sw.get("heads"))
            case "suhc" | "speed" | "speeduhc" =>
              val suhc = getGameStats(player, "SpeedUHC")
              if (suhc == null) { api.shutdown(); return }
              firstLine(player, "Speed UHC")
              printStat("Kills", suhc.get("kills"))
              printStat("Wins", suhc.get("wins"))
              printStat("Deaths", suhc.get("deaths"))
              printStat("Coins", suhc.get("coins"))
              try {
                printStat("Selected Mastery", suhc.get("activeMasteryPerk").getAsString.split("_")(1))
              } catch { case _: Exception => /* ignored */ }
              printStat("Winstreak", suhc.get("winstreak"))
            case "sh" | "smash" | "supersmash" | "smashheroes" =>
              val sh = getGameStats(player, "SuperSmash")
              if (sh == null) { api.shutdown(); return }
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
              if (tnt == null) { api.shutdown(); return }
              firstLine(player, "TNT Games")
              printStat("TNT Run Wins", tnt.get("wins_tntrun"))
              printStat("PVP Run Wins", tnt.get("win_pvprun"))
              printStat("Bowspleef Wins", tnt.get("wins_bowspleef"))
              printStat("TNT Wizards Wins", tnt.get("wins_capture"))
              printStat("TNT Tag Wins", tnt.get("wins_tnttag"))
              printStat("Coins", tnt.get("coins"))
            case "tc" | "cw" | "crazywalls" | "truecombat" =>
              val cw = getGameStats(player, "TrueCombat")
              if (cw == null) { api.shutdown(); return }
              firstLine(player, "Crazy Walls")
              printStat("Kills", cw.get("kills"))
              printStat("Wins", cw.get("wins"))
              printStat("Deaths", cw.get("deaths"))
              printStat("Coins", cw.get("coins"))
              printStat("Golden Skulls", cw.get("golden_skulls"))
              printStat("Gold Dust", cw.get("gold_dust"))
            case "uhc" =>
              val uhc = getGameStats(player, "UHC")
              if (uhc == null) { api.shutdown(); return }
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
              if (vz == null) { api.shutdown(); return }
              firstLine(player, "VampireZ")
              printStat("Human Wins", vz.get("human_wins"))
              printStat("Vampire Wins", vz.get("vampire_wins"))
              printStat("Zombie Kills", vz.get("zombie_kills"))
              printStat("Coins", vz.get("coins"))
            case "walls" =>
              val walls = getGameStats(player, "Walls")
              if (walls == null) { api.shutdown(); return }
              firstLine(player, "Walls")
              printStat("Kills", walls.get("kills"))
              printStat("Wins", walls.get("wins"))
              printStat("Losses", walls.get("losses"))
              printStat("Coins", walls.get("coins"))
              printStat("Insane Farmer", walls.get("insane_farmer"))
            case "walls3" | "mw" | "mega" | "megawalls" =>
              val mw = getGameStats(player, "Walls3")
              if (mw == null) { api.shutdown(); return }
              firstLine(player, "Mega Walls")
              printStat("Kills", mw.get("kills"))
              printStat("Wins", mw.get("wins"))
              printStat("Losses", mw.get("losses"))
              printStat("Finals", mw.get("total_final_kills"))
              printStat("Final Deaths", mw.get("final_deaths"))
              printStat("Selected Class", mw.get("chosen_class"))
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
              printStat("Name", guild.getName)
              printStat("Tag", s"[${guild.getTag}]")
              printStat("Level", getGuildLevel(guild.getExp))
              printStat("Created", Utils.parseTime(Timestamp.valueOf(guild.getCreated.toLocalDateTime).getTime))
              printStat("Members", s"${guild.getMembers.size}/125")
              g = (true, guild.getName)
            case _ =>
              b = false
              Utils.breakline()
              Utils.error(s"${args(1)} is not a valid game.")
              Utils.error("Try one of these:")
              List(
                "arcade", "arenabrawl", "warlords", "bedwars", "duels", "tkr", "blitz", "legacy", "cvc", "paintball",
                "quake", "skywars", "speeduhc", "smash", "tnt", "crazywalls", "uhc", "vampirez", "walls", "megawalls",
                "guild"
              ).foreach { it => Utils.put(s"\u00a78- \u00a7a$it") }
          }
          if (b) Utils.breakline()
          lines.foreach { it => Utils.put(it) }
          if (g._1) Minecraft.getMinecraft.thePlayer.addChatMessage(
            ChatComponentBuilder.of("\u00a7aClick to view on \u00a73plancke.io")
              .setHoverEvent(s"Click here to view ${g._2} on plancke.io")
              .setClickEvent(
                ClickEvent.Action.OPEN_URL,
                s"https://ncke.io/hypixel/guild/player/${player.get("uuid").getAsString}"
              ).build)
          Utils.breakline()
        }
        api.shutdown()
      }
    })
    thread.start()
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = args.isEmpty | args.length == 1

  private def getGuildLevel(experience: Long): Double = {
    // credit to littlemissmadivirus on sk1er discord.
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

  private def getGameStats(player: JsonObject, game: String): JsonObject = try {
    player.get("stats").getAsJsonObject.get(game).getAsJsonObject
  } catch {
    case _: Exception =>
      Utils.error(s"${player.get("displayname").getAsString} has no stats in $game")
      Utils.breakline()
      null
  }

  private def firstLine(player: JsonObject, game: String = "", guild: Boolean = false): Unit = {
    val s = if (game == "" && !guild) "Stats" else if (guild) "Guild" else s"$game stats"
    val rank = try { Utils.getRank(player) } catch { case e: Exception => e.printStackTrace(); "" }
    lines.append(s"$s of ${if (rank.endsWith("]")) s"$rank " else rank}${player.get("displayname").getAsString}")
  }

  private def printStat(name: String, value: Any): Unit = {
    // this is really ugly
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
    val statColour = s"\u00a7${f(value)}"
    lines.append(s"$name: ${if (value == null) "\u00a7cN/A" else s"$statColour$value"}")
  }
}
