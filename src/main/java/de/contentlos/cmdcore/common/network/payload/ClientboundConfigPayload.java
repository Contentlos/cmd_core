package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundConfigPayload(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(String key, String value, String type, String description, boolean requiresRestart) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::key,
                ByteBufCodecs.STRING_UTF8, Entry::value,
                ByteBufCodecs.STRING_UTF8, Entry::type,
                ByteBufCodecs.STRING_UTF8, Entry::description,
                ByteBufCodecs.BOOL, Entry::requiresRestart,
                Entry::new
        );
    }

    public static final CustomPacketPayload.Type<ClientboundConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("s2c_config"));

    public static final StreamCodec<ByteBuf, ClientboundConfigPayload> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(ClientboundConfigPayload::new, ClientboundConfigPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
