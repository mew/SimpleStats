package zone.nora.simplestats.util;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.event.HoverEvent.Action;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

/**
 * Utility class to easily build IChatComponents (Text).
 *
 * @author Semx11
 */
public class ChatComponentBuilder {
    private final IChatComponent parent;

    private String text;
    private ChatStyle style;

    private ChatComponentBuilder(String text) {
        this(text, null, Inheritance.SHALLOW);
    }

    private ChatComponentBuilder(String text, IChatComponent parent, Inheritance inheritance) {
        this.parent = parent;
        this.text = text;

        switch (inheritance) {
            case DEEP:
                this.style = parent != null ? parent.getChatStyle() : new ChatStyle();
                break;
            default:
            case SHALLOW:
                this.style = new ChatStyle();
                break;
            case NONE:
                this.style = new ChatStyle().setColor(null).setBold(false).setItalic(false)
                        .setStrikethrough(false).setUnderlined(false).setObfuscated(false)
                        .setChatClickEvent(null).setChatHoverEvent(null).setInsertion(null);
                break;
        }
    }

    public static ChatComponentBuilder of(String text) {
        return new ChatComponentBuilder(text);
    }

    public ChatComponentBuilder setClickEvent(ClickEvent.Action action, String value) {
        style.setChatClickEvent(new ClickEvent(action, value));
        return this;
    }

    public ChatComponentBuilder setHoverEvent(String value) {
        return this.setHoverEvent(new ChatComponentText(value));
    }

    public ChatComponentBuilder setHoverEvent(IChatComponent value) {
        return this.setHoverEvent(Action.SHOW_TEXT, value);
    }

    public ChatComponentBuilder setHoverEvent(HoverEvent.Action action, IChatComponent value) {
        style.setChatHoverEvent(new HoverEvent(action, value));
        return this;
    }

    public IChatComponent build() {
        IChatComponent thisComponent = new ChatComponentText(text).setChatStyle(style);
        return parent != null ? parent.appendSibling(thisComponent) : thisComponent;
    }

    public enum Inheritance {
        DEEP, SHALLOW, NONE
    }
}