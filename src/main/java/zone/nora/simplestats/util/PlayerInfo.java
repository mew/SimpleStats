package zone.nora.simplestats.util;

import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlayerInfo {

    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();

    /**
     * Gets the list of players in the current server.
     *
     * @param max Maximum number of players to put into the list.
     */
    public static List<String> getPlayers(int max) {
        return MINECRAFT
                .getNetHandler()
                .getPlayerInfoMap()
                .stream()
                .limit(max)
                .filter(Objects::nonNull)
                .map(player -> player.getGameProfile().getName())
                .collect(Collectors.toList());
    }
}
