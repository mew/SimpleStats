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
import zone.nora.simplestats.SimpleStats
import zone.nora.simplestats.util.{ChatComponentBuilder, Storage, Utils}

import scala.collection.JavaConversions.asScalaSet
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
                  var skill = 0
                  var total = 0
                  val map = if (it == "Runecrafting") (Storage.runecraftingLevels, 25) else (Storage.skillLevels, 50)
                  breakable { for (i <- 1 to map._2) { val j = map._1(i) + total; if (skillExp > j) { skill = i; total = j } else break } }
                  addStatToBuffer(it, skill, if (skill == map._2) '6' else '5')
                  if (map._2 == 50 && it != "Carpentry") skillAvg += skill
                }
                buffer.append(new ChatComponentText(""))
                addStatToBuffer("Skill Average", skillAvg / 8, 'a')
              } else {
                buffer.append(new ChatComponentText("\u00a7cNo skills found. They may be hidden!"))
              }

              /* armour */
              addBreaklineToBuffer('b')
              addTitleToBuffer("Armour", 'b')
              val armour = getNbtCompound(member.get("inv_armor").getAsJsonObject.get("data").getAsString).getTagList("i", 10)
              val armourMap = Map(0 -> "Boots", 1 -> "Leggings", 2 -> "Chestplate", 3 -> "Helmet")
              for (i <- 0 to 3) {
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
                val invContents = getNbtCompound(member.get("inv_contents").getAsJsonObject.get("data").getAsString).getTagList("i", 10)
                for (i <- 0 to 35) {
                  breakable {
                    try {
                      val item = invContents.getCompoundTagAt(i)
                      if (item.hasNoTags) break else {
                        val tag = item.getCompoundTag("tag")
                        if (Storage.weapons.contains(tag.getCompoundTag("ExtraAttributes").getString("id")) || item.getInteger("id") == 346) {
                          val display = tag.getCompoundTag("display")
                          val name = display.getString("Name")
                          val sb = new StringBuilder
                          sb.append(s"$name\n")
                          if (display.hasKey("Lore")) {
                            val lore = display.getTagList("Lore", 8)
                            for (j <- 0 until lore.tagCount())
                              sb.append(s"${lore.getStringTagAt(j)}${if (j == lore.tagCount() - 1) "" else "\n"}")
                          }
                          buffer.append(ChatComponentBuilder.of(s"  \u00a78\u27a4 \u00a7r$name")
                            .setHoverEvent(sb.toString())
                            .build()
                          )
                        }
                      }
                    } catch { case NonFatal(_) => }
                  }
                }
              } else buffer.append(new ChatComponentText("\u00a7cNo inventory found. It may be hidden!"))

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

              /* pet milestones */
              addBreaklineToBuffer('7')
              addTitleToBuffer("Pet Milestones", '7')
              addStatToBuffer("Sea Creatures Killed", getInt(stats, "pet_milestone_sea_creatures_killed"), '7')
              addStatToBuffer("Ores Mined", getInt(stats, "pet_milestone_ores_mined"), '7')


              /* bank */
              addBreaklineToBuffer('6')
              addTitleToBuffer("Bank", '6')
              addStatToBuffer("Coins in Purse", BigDecimal.valueOf(getDouble(member, "coin_purse")), '6')
              if (profile.has("banking")) {
                addStatToBuffer("Coins in Bank", BigDecimal.valueOf(getDouble(profile.get("banking").getAsJsonObject, "balance")), '6')
              } else {
                buffer.append(new ChatComponentText("\u00a7cThis profile has it's bank information disabled."))
              }

              /* finish */
              addBreaklineToBuffer()
              buffer.append(ChatComponentBuilder.of("\u00a73Click to view on \u00a7d\u00a7nsky.lea.moe")
                .setHoverEvent("Click to open \u00a79https://sky.lea.moe\u00a7r!")
                .setClickEvent(ClickEvent.Action.OPEN_URL, s"https://sky.lea.moe/stats/$playerUuid/$profileId")
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
    try { jsonObject.get(name).getAsDouble } catch { case NonFatal(_) => 0.0 }

  private def getInt(jsonObject: JsonObject, name: String): Int =
    try { jsonObject.get(name).getAsInt } catch { case NonFatal(_) => 0 }

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
    val dash = Math.floor((280 * Minecraft.getMinecraft.gameSettings.chatWidth + 40) / 320 * (1 / Minecraft.getMinecraft.gameSettings.chatScale) * 53).toInt - 3
    for (i <- 1 to dash)
      if (i == (dash / 2)) dashes.append(s"\u00a7$colour[\u00a76SS\u00a7$colour]\u00a7m") else dashes.append("-")
    buffer.append(new ChatComponentText(s"\u00a7$colour\u00a7m$dashes"))
  }

  private def getNbtCompound(compressed: String): NBTTagCompound = {
    val inputStream = new ByteArrayInputStream(Base64.getDecoder.decode(compressed))
    CompressedStreamTools.readCompressed(inputStream)
  }
}