package zone.nora.simplestats.commands.hidden

import java.io.ByteArrayInputStream
import java.util.Base64

import com.google.gson.JsonObject
import net.hypixel.api.HypixelAPI
import net.minecraft.client.Minecraft
import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.event.ClickEvent
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraft.util.{ChatComponentText, IChatComponent}
import net.minecraftforge.fml.common.Loader
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.util.{ChatComponentBuilder, Constants, Utils}

import scala.collection.JavaConversions.{asScalaSet, iterableAsScalaIterable}
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks.{break, breakable}
import scala.util.control.NonFatal

// minecraft requires you to run a command on click event :|
class HiddenSkyBlockCommand extends CommandBase {
  private val buffer: ListBuffer[IChatComponent] = new ListBuffer[IChatComponent]

  override def getCommandName: String = "$skyblock_stats"

  override def getCommandUsage(sender: ICommandSender): String = "nope"

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    if (args.length == 2) {
      new Thread(new Runnable {
        override def run(): Unit = {
          val api = new HypixelAPI(SimpleStats.key)
          val profileId = args(0)
          val playerUuid = args(1)

          val skyblockProfileReply = api.getSkyBlockProfile(profileId).get()
          if (skyblockProfileReply.isSuccess) {
            val profile = skyblockProfileReply.getProfile
            val profileMembers = profile.get("members").getAsJsonObject
            if (profileMembers.has(playerUuid)) {
              val member = profileMembers.get(playerUuid).getAsJsonObject
              val stats = member.get("stats").getAsJsonObject

              /* skills */
              addBreaklineToBuffer('5')
              addTitleToBuffer("Skills", '5')
              if (member.has("experience_skill_combat")) {
                val skills = "Combat" :: "Runecrafting" :: "Mining" :: "Alchemy" :: "Farming" :: "Taming" :: "Enchanting" :: "Fishing" :: "Foraging" :: "Carpentry" :: Nil
                var skillAvg = 0.0
                skills.foreach { it =>
                  val skillExp = getDouble(member, s"experience_skill_${it.toLowerCase}")
                  var skill, total = 0
                  val map = if (it == "Runecrafting") (Constants.RUNECRAFTING_LEVELS, 25) else (Constants.SKILL_LEVELS, 50)
                  breakable { for (i <- 1 to map._2) { val j = map._1(i) + total; if (skillExp > j) { skill = i; total = j } else break } }
                  addStatToBuffer(it, skill, if (skill == map._2) '6' else '5')
                  if (map._2 == 50 && it != "Carpentry") skillAvg += skill
                }
                buffer.append(new ChatComponentText(""))
                addStatToBuffer("Skill Average", skillAvg / 8, 'a')
              } else {
                buffer.append(new ChatComponentText("\u00a7cNo skills found. They may be hidden!"))
              }

              val viewStatBoost = Loader.isModLoaded("StatBoostViewer") || Loader.isModLoaded("leirvika")

              /* armour */
              addBreaklineToBuffer('b')
              addTitleToBuffer("Armour", 'b')
              val armour = getNbtCompound(member.get("inv_armor").getAsJsonObject.get("data").getAsString).getTagList("i", 10)
              val armourMap = Map(0 -> "Boots", 1 -> "Leggings", 2 -> "Chestplate", 3 -> "Helmet")
              for (i <- (0 to 3).reverse) {
                val piece = armour.getCompoundTagAt(i)
                if (piece.hasNoTags) addStatToBuffer(armourMap(i), "\u00a7cNone", 'b') else {
                  val display = piece.getCompoundTag("tag").getCompoundTag("display")
                  val pieceName = display.getString("Name")
                  val sb = new StringBuilder
                  sb.append(s"$pieceName\n")
                  if (display.hasKey("Lore")) {
                    val lore = display.getTagList("Lore", 8)
                    for (j <- 0 until lore.tagCount())
                      sb.append(s"${lore.getStringTagAt(j)}${if (j == lore.tagCount() - 1) "" else "\n"}")
                  }
                  if (viewStatBoost) {
                    val extraAttributes = piece.getCompoundTag("tag").getCompoundTag("ExtraAttributes")
                    val flags = (extraAttributes.hasKey("baseStatBoostPercentage"), extraAttributes.hasKey("item_tier"))
                    if (flags._1 || flags._2) {
                      sb.append("\n")
                      if (flags._1)
                        sb.append(s"\n\u00a76Stat Boost Percentage: ${extraAttributes.getInteger("baseStatBoostPercentage")}/50")
                      if (flags._2)
                        sb.append(s"\n\u00a76Found on Floor ${extraAttributes.getInteger("item_tier")}")
                    }
                  }
                  buffer.append(ChatComponentBuilder.of(s"\u00a7b${armourMap(i)}\u00a7r: $pieceName")
                    .setHoverEvent(sb.toString())
                    .build()
                  )
                }
              }

              /* weapons */
              addBreaklineToBuffer('d')
              addTitleToBuffer("Weapons", 'd')
              if (member.has("inv_contents")) {
                val weapons = new ListBuffer[(String, StringBuilder)]
                val rods = new ListBuffer[(String, StringBuilder)]
                val inventories = new ListBuffer[(String, Int)]
                //val inventories = ("inv", 35) :: ("ender_chest", 64) :: Nil
                (("inv", 35) :: ("ender_chest", 64) :: Nil).foreach(it => inventories.append(it))
                if (member.has("personal_vault_contents")) inventories.append(("personal_vault", 27))
                inventories.foreach { it =>
                  val invContents = getNbtCompound(member.get(s"${it._1}_contents").getAsJsonObject.get("data").getAsString).getTagList("i", 10)
                  for (i <- 0 to it._2) {
                    breakable {
                      try {
                        val item = invContents.getCompoundTagAt(i)
                        if (item.hasNoTags) break else {
                          var rod = false
                          var weapon = false
                          val tag = item.getCompoundTag("tag")
                          val display = tag.getCompoundTag("display")
                          val name = display.getString("Name")
                          val sb = new StringBuilder
                          sb.append(s"$name\n")
                          if (display.hasKey("Lore")) {
                            val lore = display.getTagList("Lore", 8)
                            for (j <- 0 until lore.tagCount()) {
                              val l = lore.getStringTagAt(j)
                              //sb.append(s"$l${if (j == lore.tagCount() - 1) "" else "\n"}")
                              if (j == lore.tagCount() - 1) {
                                val l_ = l.toLowerCase()
                                if (l_.contains("sword") || l_.contains("bow")) weapon = true else if (l_.contains("rod")) rod = true else break
                                sb.append(l)
                              } else sb.append(s"$l\n")
                            }
                          } else break
                          val extraAttributes = tag.getCompoundTag("ExtraAttributes")
                          if (viewStatBoost) {
                            val flags = (extraAttributes.hasKey("baseStatBoostPercentage"), extraAttributes.hasKey("item_tier"))
                            if (flags._1 || flags._2) {
                              sb.append("\n")
                              if (flags._1)
                                sb.append(s"\n\u00a76Stat Boost Percentage: ${extraAttributes.getInteger("baseStatBoostPercentage")}/50")
                              if (flags._2)
                                sb.append(s"\n\u00a76Found on Floor ${extraAttributes.getInteger("item_tier")}")
                            }
                          }
                          if (weapon) weapons.append((name, sb)) else if (rod) rods.append((name, sb))

                          //buffer.append(ChatComponentBuilder.of(s"  \u00a78\u27a4 \u00a7r$name")
                          //  .setHoverEvent(sb.toString())
                          //  .build()
                          //)
                        }
                      } catch { case NonFatal(_) => /* nothing */}
                    }
                  }
                }
                def itemIter(lb: ListBuffer[(String, StringBuilder)]): Unit = {
                  lb.foreach(it => buffer.append(ChatComponentBuilder.of(s"  \u00a78\u27a4 \u00a7r${it._1}").setHoverEvent(it._2.toString()).build()))
                }
                itemIter(weapons)
                if (rods.nonEmpty) {
                  buffer.append(new ChatComponentText(""))
                  addTitleToBuffer("Fishing Rods", 'd')
                  itemIter(rods)
                }
              } else buffer.append(new ChatComponentText("\u00a7cNo inventory found. It may be hidden!"))

              /* pets */
              addBreaklineToBuffer('7')
              addTitleToBuffer("Pets", '7')
              try {
                if (member.has("pets")) {
                  val pets = new ListBuffer[SkyblockPet]
                  var activePet: SkyblockPet = null
                  member.get("pets").getAsJsonArray.foreach { obj =>
                    val petObj = obj.getAsJsonObject
                    val pet = SkyblockPet(
                      petObj.get("type").getAsString,
                      getDouble(petObj, "exp"),
                      petObj.get("tier").getAsString.toLowerCase
                    )
                    if (petObj.get("active").getAsBoolean) activePet = pet else pets.append(pet)
                  }
                  // TODO have pet info on hover.
                  if (activePet != null) buffer.append(new ChatComponentText(s"  \u00a78\u27a4 \u00a7r${activePet.getFormattedName}\u00a7a\u00a7l - ACTIVE PET"))
                  val pets_ = pets.sortBy(it => it.exp).reverse
                  if (pets.size < 10)
                    pets_.foreach(it => buffer.append(new ChatComponentText(s"  \u00a78\u27a4 \u00a7r${it.getFormattedName}")))
                  else {
                    pets_.slice(0, 9).foreach(it => buffer.append(new ChatComponentText(s"  \u00a78\u27a4 \u00a7r${it.getFormattedName}")))
                    var morePets = ""
                    pets_.slice(9, pets.size).foreach(it => morePets += s"${it.getFormattedName}${if (it != pets_.last) "\n" else ""}")
                    buffer.append(ChatComponentBuilder.of(s"  \u00a78\u27a4 \u00a7cAnd ${pets.size - 9} more..").setHoverEvent(morePets).build())
                  }
                } else buffer.append(new ChatComponentText("\u00a7cNo pets found."))
              } catch { case NonFatal(_) => /* nothing */ }

              /* pet milestones */
              buffer.append(new ChatComponentText(""))
              addTitleToBuffer("Pet Milestones", '7')
              addStatToBuffer("Sea Creatures Killed", getInt(stats, "pet_milestone_sea_creatures_killed"), '7')
              addStatToBuffer("Ores Mined", getInt(stats, "pet_milestone_ores_mined"), '7')

              /* Dungeons */
              addBreaklineToBuffer('3')
              addTitleToBuffer("Dungeons", '3')
              if (member.has("dungeons")) {
                def calcDungeonLevel(exp: Double): Int = {
                  var i, j = 0
                  breakable(for (k <- 1 to 50) { val l = Constants.DUNGEON_LEVELS(k) + j; if (exp > l) { i = k; j = l } else break })
                  i
                }
                val dungeons = member.get("dungeons").getAsJsonObject
                val catacombs = dungeons.get("dungeon_types").getAsJsonObject.get("catacombs").getAsJsonObject
                val sdc = if (dungeons.has("selected_dungeon_class")) dungeons.get("selected_dungeon_class").getAsString else "none"
                val cl = calcDungeonLevel(getDouble(catacombs, "experience"))
                val classes = "healer" :: "mage" :: "berserk" :: "archer" :: "tank" :: Nil
                addStatToBuffer("Catacombs Level", cl, if (cl > 29) '6' else '3')
                buffer.append(new ChatComponentText(""))
                if (dungeons.has("player_classes")) {
                  val playerClasses = dungeons.get("player_classes").getAsJsonObject
                  classes.foreach { it =>
                    if (playerClasses.has(it)) {
                      val c = it.charAt(0)
                      val u = s"${it.replaceFirst(c.toString, c.toUpper.toString)} Level"
                      val lvl = calcDungeonLevel(getDouble(playerClasses.get(it).getAsJsonObject, "experience"))
                      buffer.append(new ChatComponentText(s"\u00a7${if (lvl > 29) '6' else '3'}${Utils.formatStat(u, lvl)}${if (it == sdc) " \u00a7a\u00a7l- ACTIVE CLASS" else ""}"))
                    }
                  }
                }
                buffer.append(new ChatComponentText(""))
                buffer.append(new ChatComponentText("\u00a73Floor Stats\u00a7f:"))
                def getFloorStat(name: String, floor: Int): Int = if (catacombs.has(name)) {
                  val obj = catacombs.get(name).getAsJsonObject
                  if (obj.has(floor.toString)) obj.get(floor.toString).getAsInt else 0
                } else 0
                def readableTime(ms: Int): String = {
                  val s = ms / 1000
                  var t = s"${s / 60}m"
                  if (s % 60 != 0) t += s"${s % 60}s"
                  t
                }
                for (i <- 0 to 10) {
                  breakable {
                    if (!catacombs.get("times_played").getAsJsonObject.has(i.toString)) break
                    val formattedFloor = if (i == 0) "\u00a7cDungeon Entrance" else s"\u00a7cFloor \u00a77${Constants.ROMAN_NUMERALS(i)}"
                    var hoverText = s"\u00a78\u00a7kAN\u00a7r $formattedFloor \u00a78\u00a7kNA\n" // <3
                    val timesPlayed = getFloorStat("times_played", i)
                    val tierCompletions = getFloorStat("tier_completions", i)
                    hoverText += s"\u00a7cTimes Played\u00a7e: $timesPlayed\n\u00a7cTimes Completed\u00a7e: $tierCompletions\n\u00a7cCompletion %\u00a7e: ${((tierCompletions.toFloat / timesPlayed.toFloat) * 100).toInt}%\n"
                    hoverText += s"\u00a7cBest Score\u00a7e: ${getFloorStat("best_score", i)}\n"
                    hoverText += s"\u00a7cMini-Boss Kills\u00a7e: ${getFloorStat("watcher_kills", i)}\n"
                    val t = "fastest_time" :: "fastest_time_s" :: "fastest_time_s_plus" :: Nil
                    val m = Map("fastest_time" -> "Fastest Time\u00a7e: ", "fastest_time_s" -> "Fastest Time (\u00a76S\u00a7c)\u00a7e: ", "fastest_time_s_plus" -> "Fastest Time (\u00a76S+\u00a7c)\u00a7e: ")
                    t.foreach { it =>
                      val ms = getFloorStat(it, i)
                      //if (ms == 0) hoverText += s"${m(it)}\u00a7cN/A\n" else {
                      hoverText += s"\u00a7c${m(it)}${if (ms == 0) "\u00a7cN/A" else readableTime(ms)}\n"
                        //hoverText.patch(hoverText.lastIndexOf('\n'), "", 1)
                    }
                    buffer.append(ChatComponentBuilder.of(s"  \u00a78\u27a4 $formattedFloor").setHoverEvent(hoverText.patch(hoverText.lastIndexOf('\n'), "", 1)).build())
                  }
                }
              } else buffer.append(new ChatComponentText("No dungeon stats found for this profile."))

              /* slayer */
              addBreaklineToBuffer('2')
              addTitleToBuffer("Slayer", '2')
              if (member.has("slayer_bosses")) {
                val slayerBosses = member.get("slayer_bosses").getAsJsonObject
                val slayers = "Zombie" :: "Spider" :: "Wolf" :: Nil
                var total = 0
                slayers.foreach { it =>
                  val slayer = slayerBosses.get(it.toLowerCase).getAsJsonObject
                  val xp = getInt(slayer, "xp")
                  total += xp
                  val level = xp match {
                    case xp if 5 to 14 contains xp => (1, 15)
                    case xp if 15 to 199 contains xp => (2, 200)
                    case xp if 200 to 999 contains xp => (3, 1000)
                    case xp if 1000 to 4999 contains xp => (4, 5000)
                    case xp if 5000 to 19999 contains xp => (5, 20000)
                    case xp if 20000 to 99999 contains xp => (6, 100000)
                    case xp if 100000 to 399999 contains xp => (7, 400000)
                    case xp if 400000 to 999999 contains xp => (8, 1000000)
                    case xp if xp >= 1000000 => (9, -1)
                    case _ => (0, 5)
                  }
                  val c = if (level._1 == 9) '6' else '2'
                  addStatToBuffer(s"$it Level", level._1, c)
                  addStatToBuffer(s"$it Total XP", s"\u00a7e$xp${if (level._1 == 9) "" else s"/${level._2}"}", c)
                  buffer.append(new ChatComponentText(""))
                }
                addStatToBuffer("Total Slayer XP", total, 'd')
              } else {
                buffer.append(new ChatComponentText("\u00a7cNo Slayer information found."))
              }


              /* bank */
              addBreaklineToBuffer('6')
              addTitleToBuffer("Bank", '6')
              addStatToBuffer("Coins in Purse", BigDecimal.valueOf(getDouble(member, "coin_purse")).setScale(1, BigDecimal.RoundingMode.DOWN), '6')
              if (profile.has("banking")) {
                addStatToBuffer("Coins in Bank", BigDecimal.valueOf(getDouble(profile.get("banking").getAsJsonObject, "balance")).setScale(1, BigDecimal.RoundingMode.DOWN), '6')
              } else {
                buffer.append(new ChatComponentText("\u00a7cThis profile has it's bank information disabled."))
              }

              /* finish */
              addBreaklineToBuffer()
              buffer.append(ChatComponentBuilder.of("\u00a73Click to view on \u00a7a\u00a7nhttps://sky.shiiyu.moe/")
                .setHoverEvent("Click to open \u00a79https://sky.shiiyu.moe\u00a7r!")
                .setClickEvent(ClickEvent.Action.OPEN_URL, s"https://sky.shiiyu.moe/stats/$playerUuid/$profileId")
                .build()
              )
              buffer.append(ChatComponentBuilder.of("\u00a73Click to view on \u00a7e\u00a7nskyblock.matdoes.dev")
                .setHoverEvent("Click to open \u00a79https://skyblock.matdoes.dev\u00a7r!")
                .setClickEvent(ClickEvent.Action.OPEN_URL, s"https://skyblock.matdoes.dev/profile/$playerUuid/$profileId")
                .build()
              )
              addBreaklineToBuffer()

              /* print */
              buffer.foreach { it =>
                Minecraft.getMinecraft.thePlayer.addChatMessage(it)
              }
              buffer.clear()
            } else {
              profileMembers.entrySet().foreach { it =>
                println(it.getKey)
                println(playerUuid)
              }
              Utils.error("Player not found in profile...", prefix = true)
            }
          } else Utils.error(skyblockProfileReply.getCause, prefix = true)
          api.shutdown()
        }
      }).start()
    } else {
      Utils.error("Do /stats [player] skyblock", prefix = true)
    }
  }

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  private def getDouble(jsonObject: JsonObject, name: String): Double =
    try jsonObject.get(name).getAsDouble catch { case NonFatal(_) => 0.0 }

  private def getInt(jsonObject: JsonObject, name: String): Int =
    try jsonObject.get(name).getAsInt catch { case NonFatal(_) => 0 }

  private def addStatToBuffer(name: String, value: Any, colour: Char = 'f'): Unit = {
    if (value == null) return
    if (value.isInstanceOf[String] && value.toString.isEmpty) return
    buffer.append(new ChatComponentText(s"\u00a7$colour${Utils.formatStat(name, value)}"))
  }

  private def addTitleToBuffer(title: String, colour: Char): Unit = {
    buffer.append(new ChatComponentText(s"\u00a7$colour\u00a7l\u00a7n$title:\n"))
  }

  private def addBreaklineToBuffer(colour: Char = '9'): Unit = {
    val dashes = new StringBuilder
    val dash = Math.floor((280 * Minecraft.getMinecraft.gameSettings.chatWidth + 40) / 320 * (1 / Minecraft.getMinecraft.gameSettings.chatScale) * 53).toInt - 6
    for (i <- 1 to dash)
      if (i == (dash / 2)) dashes.append(s"\u00a7$colour[\u00a76SIMPLE\u00a7$colour]\u00a7m") else dashes.append("-")
    buffer.append(new ChatComponentText(s"\u00a7$colour\u00a7m$dashes"))
  }

  private def getNbtCompound(compressed: String): NBTTagCompound = {
    val inputStream = new ByteArrayInputStream(Base64.getDecoder.decode(compressed))
    CompressedStreamTools.readCompressed(inputStream)
  }

  private case class SkyblockPet(private val name: String, exp: Double, private val tier: String) {
    private val offset: Int = tier match {
      case "uncommon" => 5
      case "rare" => 10
      case "epic" => 15
      case "legendary" => 19
      case _ => 0
    }

    private def calculateLevel(): Int = {
      var totalExp, lvl = 0
      breakable {
        for (i <- offset to offset + 100) {
          lvl = i - offset
          totalExp += (try { Constants.PET_LEVELS(i) } catch { case NonFatal(_) => 0 })
          if ((totalExp - Constants.PET_LEVELS(offset)) > exp) break
        }
      }
      lvl
    }

    private def formatUpperSnake(str: String): String = {
      // TODO make this better
      var str_ = str.toLowerCase
      str_ = str_.replaceFirst(str_.charAt(0).toString, str_.charAt(0).toUpper.toString)
      while (str_.contains("_"))
        str_ = str_.replaceFirst("_[a-z]", " " + Character.toUpperCase(str_.charAt(str_.indexOf("_") + 1)))

      str_
    }

    def getFormattedName: String = {
      val colour = tier match {
        case "uncommon" => 'a'
        case "rare" => '9'
        case "epic" => '5'
        case "legendary" => '6'
        case _ => 'f'
      }
      s"\u00a77[Lvl ${calculateLevel()}] \u00a7$colour${formatUpperSnake(name)}"
    }
  }
}
