package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Liste der aktuellen Spieler (alle Online-Spieler).
 */
public record ClientboundPlayerListPayload(List<Entry> players) implements CustomPacketPayload {

    public record Entry(
            String name,
            String uuid,
            String level,
            boolean isOp,
            String gamemode,
            String dimension,
            double posX,
            double posY,
            double posZ,
            float health,
            int food,
            boolean flying,
            int ping
    ) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Entry decode(ByteBuf buf) {
                return new Entry(
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.DOUBLE.decode(buf),
                        ByteBufCodecs.DOUBLE.decode(buf),
                        ByteBufCodecs.DOUBLE.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf)
                );
            }

            @Override
            public void encode(ByteBuf buf, Entry e) {
                ByteBufCodecs.STRING_UTF8.encode(buf, e.name);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.uuid);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.level);
                ByteBufCodecs.BOOL.encode(buf, e.isOp);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.gamemode);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.dimension);
                ByteBufCodecs.DOUBLE.encode(buf, e.posX);
                ByteBufCodecs.DOUBLE.encode(buf, e.posY);
                ByteBufCodecs.DOUBLE.encode(buf, e.posZ);
                ByteBufCodecs.FLOAT.encode(buf, e.health);
                ByteBufCodecs.VAR_INT.encode(buf, e.food);
                ByteBufCodecs.BOOL.encode(buf, e.flying);
                ByteBufCodecs.VAR_INT.encode(buf, e.ping);
            }
        };
    }

    public static final CustomPacketPayload.Type<ClientboundPlayerListPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("s2c_player_list"));

    public static final StreamCodec<ByteBuf, ClientboundPlayerListPayload> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(ClientboundPlayerListPayload::new, ClientboundPlayerListPayload::players);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
