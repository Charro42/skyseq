package dev.skyseq;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * Works out where you are on Hypixel SkyBlock by reading the tab list lines such as
 * "Area: Garden" or "Dungeon: Catacombs". Only read when needed (on a keypress or when the editor opens).
 */
public final class Area {

    private Area() {
    }

    /** Lower-case values after "Area:" / "Dungeon:" in the tab list, e.g. ["garden"]. Empty if not found. */
    public static List<String> values() {
        List<String> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return out;
        }
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            Component name = info.getTabListDisplayName();
            if (name == null) {
                continue;
            }
            String text = name.getString().trim().toLowerCase(Locale.ROOT);
            if (text.startsWith("area:") || text.startsWith("dungeon:")) {
                String value = text.substring(text.indexOf(':') + 1)
                        .replaceAll("[^a-z0-9 '\\-]", "")
                        .trim();
                if (!value.isEmpty()) {
                    out.add(value);
                }
            }
        }
        return out;
    }

    /** Short text for the editor screen. */
    public static String describe() {
        List<String> v = values();
        return v.isEmpty() ? "(not detected)" : String.join(" / ", v);
    }

    /**
     * Does the filter allow the current area? Blank filter = everywhere.
     * Comma-separated names; "garden" matches any area containing that word, "=hub" must match exactly.
     */
    public static boolean matches(String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        List<String> current = values();
        if (current.isEmpty()) {
            return false;
        }
        for (String raw : filter.split(",")) {
            String token = raw.trim().toLowerCase(Locale.ROOT);
            if (token.isEmpty()) {
                continue;
            }
            boolean exact = token.startsWith("=");
            if (exact) {
                token = token.substring(1).trim();
            }
            for (String value : current) {
                if (exact ? value.equals(token) : value.contains(token)) {
                    return true;
                }
            }
        }
        return false;
    }
}
