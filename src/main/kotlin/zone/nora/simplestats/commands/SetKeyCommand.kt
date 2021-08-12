package zone.nora.simplestats.commands

import gg.essential.api.EssentialAPI
import gg.essential.api.utils.Multithreading
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import zone.nora.simplestats.config.Config
import zone.nora.simplestats.util.Utils

object SetKeyCommand : CommandBase() {
    override fun getCommandName(): String = "setkey"

    override fun getCommandUsage(sender: ICommandSender): String = "/setkey [api key]"

    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        if (args.size == 1) {
            val a0 = args[0]
            if (a0.lowercase() == "gui") {
                EssentialAPI.getGuiUtil().openScreen(Config.gui())
            } else {
                Utils.put("Setting API key.")
                Multithreading.runAsync {
                    Config.hypixelKeyS = a0
                    if (Config.validKey) {
                        Utils.put("Successfully set key!")
                    } else {
                        Utils.err("Set key... but it was invalid!!")
                        Utils.err("Double check that you input your key correctly.")
                    }
                    Config.markDirty()
                    Config.writeData()
                }
            }
        }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender): Boolean = true
}