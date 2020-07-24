package zone.nora.simplestats.util

import net.minecraft.event.HoverEvent.Action
import net.minecraft.event.{ClickEvent, HoverEvent}
import net.minecraft.util.{ChatComponentText, ChatStyle, IChatComponent}
import zone.nora.simplestats.util.ChatComponentBuilder.Inheritance
import zone.nora.simplestats.util.ChatComponentBuilder.Inheritance.Inheritance

/**
 * Utility class to easily build IChatComponents.
 *
 * @author Semx11
 * @author WaningMatrix
 */
object ChatComponentBuilder {
  def of(text: String) = new ChatComponentBuilder(text)

  object Inheritance extends Enumeration {
    type Inheritance = Value
    val DEEP, SHALLOW, NONE = Value
  }

}

class ChatComponentBuilder(text: String, val parent: IChatComponent, val inheritance: Inheritance) {
  var style: ChatStyle = _

  def this(text: String) {
    this(text, null, Inheritance.SHALLOW)
  }

  inheritance match {
    case Inheritance.DEEP =>
      this.style = if (parent != null) parent.getChatStyle else new ChatStyle
    case _ =>
    case Inheritance.SHALLOW =>
      this.style = new ChatStyle
    case Inheritance.NONE =>
      this.style = new ChatStyle()
        .setColor(null)
        .setBold(false)
        .setItalic(false)
        .setStrikethrough(false)
        .setUnderlined(false)
        .setObfuscated(false)
        .setChatClickEvent(null)
        .setChatHoverEvent(null)
        .setInsertion(null)
  }

  def setClickEvent(action: ClickEvent.Action, value: String): ChatComponentBuilder = {
    style.setChatClickEvent(new ClickEvent(action, value))
    this
  }

  def setHoverEvent(value: String): ChatComponentBuilder = this.setHoverEvent(new ChatComponentText(value))

  def setHoverEvent(value: IChatComponent): ChatComponentBuilder = this.setHoverEvent(Action.SHOW_TEXT, value)

  def setHoverEvent(action: HoverEvent.Action, value: IChatComponent): ChatComponentBuilder = {
    style.setChatHoverEvent(new HoverEvent(action, value))
    this
  }

  def build: IChatComponent = {
    val thisComponent = new ChatComponentText(text).setChatStyle(style)
    if (parent != null) parent.appendSibling(thisComponent)
    else thisComponent
  }
}


