package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundDebugPayload(List<Entry> rows) implements CustomPacketPayload {

    public record Entry(String key, String value) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::key,
                ByteBufCodecs.STRING_UTF8, Entry::value,
                Entry::new
        );
    }

    public static final CustomPacketPayload.Type<ClientboundDebugPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("s2c_debug"));

    public static final StreamCodec<ByteBuf, ClientboundDebugPayload> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(ClientboundDebugPayload::new, ClientboundDebugPayload::rows);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
