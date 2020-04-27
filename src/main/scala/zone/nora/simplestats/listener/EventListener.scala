package zone.nora.simplestats.listener

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.event.ClickEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import zone.nora.simplestats.util.{ChatComponentBuilder, Utils}

class EventListener {
  @SubscribeEvent
  def onLogin(e: EntityJoinWorldEvent): Unit = {
    if (!Minecraft.getMinecraft.isSingleplayer && e.entity.isInstanceOf[EntityPlayer])
      if (e.entity.getName == Minecraft.getMinecraft.thePlayer.getName) {
        Utils.breakline()
        Utils.put("An update is available for SimpleStats!")
        Utils.put("You can Download it here:")
        Minecraft.getMinecraft.thePlayer.addChatMessage(
          ChatComponentBuilder.of(s"${Utils.PREFIX}\u00a76>> \u00a79Click Here \u00a76<<")
            .setHoverEvent("\u00a79Click!")
            .setClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/mew/SimpleStats/releases")
            .build()
        )
        Utils.breakline()
        MinecraftForge.EVENT_BUS.unregister(this)
      }
  }
}
