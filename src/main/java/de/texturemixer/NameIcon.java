package de.texturemixer;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import de.texturemixer.net.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-Seite: welche Spieler haben die Mod, und das Icon am Nametag. */
public final class NameIcon {
    private NameIcon() {
    }

    public static final String OFF = "off";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";

    private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();
    /** Nametag-Farben (RGB) der anderen Mod-Spieler, vom Server geschickt. */
    private static final Map<UUID, Integer> COLORS = new ConcurrentHashMap<>();
    private static Text icon;

    public static void setUsers(Iterable<UUID> users) {
        USERS.clear();
        for (UUID u : users) USERS.add(u);
    }

    public static void clear() {
        USERS.clear();
        COLORS.clear();
    }

    public static void setColors(Map<UUID, Integer> colors) {
        COLORS.clear();
        COLORS.putAll(colors);
    }

    /** Schickt die eigene Nametag-Farbe an den Server (der sie an alle Mod-Spieler verteilt). Ohne Mod auf dem Server passiert nichts. */
    public static void sendColor() {
        if (!ClientPlayNetworking.canSend(Net.NameColorC2S.ID)) return;
        ClientPlayNetworking.send(new Net.NameColorC2S(MixerConfig.get().nameColor));
    }

    /** Farbe des Nametags dieses Spielers (RGB) oder null = Standard. Der eigene Spieler nimmt die Einstellung direkt. */
    private static Integer colorOf(UUID id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getUuid().equals(id)) {
            int own = MixerConfig.get().nameColor;
            return own >= 0 ? Integer.valueOf(own) : null;
        }
        return COLORS.get(id);
    }

    /** Nächster Zustand des Reglers: vor -> hinter -> aus -> vor. */
    public static String next(String mode) {
        if (BEFORE.equals(mode)) return AFTER;
        if (AFTER.equals(mode)) return OFF;
        return BEFORE;
    }

    public static String label(String mode) {
        if (BEFORE.equals(mode)) return "Icon: vor Name";
        if (AFTER.equals(mode)) return "Icon: hinter Name";
        return "Icon: aus";
    }

    private static Text icon() {
        if (icon == null) {
            String json = "{\"text\":\"\\uE000\",\"font\":\"texturemixer:icon\",\"color\":\"white\"}";
            icon = TextCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElse(Text.empty());
        }
        return icon;
    }

    private static boolean isModUser(UUID id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getUuid().equals(id)) return true; // ich selbst habe die Mod ja
        return USERS.contains(id);
    }

    /** Gibt den Namen in der gewählten Farbe und mit Icon zurück (je nach Einstellung und ob der Spieler die Mod hat). */
    public static Text decorate(Text name, UUID player) {
        if (name.getString().indexOf('\uE000') >= 0) return name; // schon dekoriert, nie doppelt
        Integer color = colorOf(player);
        if (color != null) {
            // Name als einfacher Text mit dem Stil des Namens, nur die Farbe wird ersetzt (auch eine Team-Farbe)
            name = Text.literal(name.getString()).setStyle(name.getStyle().withColor(color));
        }
        String mode = MixerConfig.get().nameIcon;
        if (mode == null || OFF.equals(mode) || !isModUser(player)) return name;
        Text sp = Text.literal(" ");
        if (BEFORE.equals(mode)) return Text.empty().append(icon()).append(sp).append(name);
        return Text.empty().append(name).append(sp).append(icon());
    }
}
