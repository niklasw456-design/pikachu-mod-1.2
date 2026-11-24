package de.texturemixer.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Die Pakete, mit denen Spieler mit Pikachu Styles einander erkennen und ihre Nametag-Farben austauschen. */
public final class Net {
    private Net() {
    }

    /** Client -> Server: "ich habe die Mod". */
    public record HelloC2S() implements CustomPayload {
        public static final CustomPayload.Id<HelloC2S> ID = new CustomPayload.Id<>(Identifier.of("texturemixer", "hello"));
        public static final HelloC2S INSTANCE = new HelloC2S();
        public static final PacketCodec<PacketByteBuf, HelloC2S> CODEC = PacketCodec.unit(INSTANCE);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Server -> Client: alle Spieler (UUIDs), die gerade die Mod haben. */
    public record UsersS2C(List<UUID> users) implements CustomPayload {
        public static final CustomPayload.Id<UsersS2C> ID = new CustomPayload.Id<>(Identifier.of("texturemixer", "users"));
        public static final PacketCodec<PacketByteBuf, UsersS2C> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeVarInt(value.users().size());
                    for (UUID u : value.users()) buf.writeUuid(u);
                },
                buf -> {
                    int n = Math.min(buf.readVarInt(), 4096);
                    List<UUID> l = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) l.add(buf.readUuid());
                    return new UsersS2C(l);
                });

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Client -> Server: "das ist die Farbe meines Nametags" (RGB, -1 = Standard). */
    public record NameColorC2S(int color) implements CustomPayload {
        public static final CustomPayload.Id<NameColorC2S> ID = new CustomPayload.Id<>(Identifier.of("texturemixer", "name_color"));
        public static final PacketCodec<PacketByteBuf, NameColorC2S> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeInt(value.color()),
                buf -> new NameColorC2S(buf.readInt()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Server -> Client: die Nametag-Farben aller Mod-Spieler, die eine Farbe gewählt haben. */
    public record NameColorsS2C(Map<UUID, Integer> colors) implements CustomPayload {
        public static final CustomPayload.Id<NameColorsS2C> ID = new CustomPayload.Id<>(Identifier.of("texturemixer", "name_colors"));
        public static final PacketCodec<PacketByteBuf, NameColorsS2C> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeVarInt(value.colors().size());
                    for (Map.Entry<UUID, Integer> e : value.colors().entrySet()) {
                        buf.writeUuid(e.getKey());
                        buf.writeInt(e.getValue());
                    }
                },
                buf -> {
                    int n = Math.min(buf.readVarInt(), 4096);
                    Map<UUID, Integer> m = new LinkedHashMap<>();
                    for (int i = 0; i < n; i++) {
                        UUID id = buf.readUuid();
                        m.put(id, buf.readInt());
                    }
                    return new NameColorsS2C(m);
                });

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
