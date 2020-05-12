package zone.nora.simplestats.commands

import net.minecraft.client.Minecraft
import net.minecraft.command.{CommandBase, ICommandSender}

class HypixelStatsAlias extends CommandBase {
  override def getCommandName: String = "hstats"

  override def getCommandUsage(sender: ICommandSender): String = "/hstats [player]"

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit =
    Minecraft.getMinecraft.thePlayer.sendChatMessage(s"/hypixelcommand:stats ${args.mkString(" ")}")

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = true

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = true
}
