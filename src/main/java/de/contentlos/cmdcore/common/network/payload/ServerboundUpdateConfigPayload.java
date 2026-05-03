package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Setzt einen Config-Wert serverseitig. */
public record ServerboundUpdateConfigPayload(String key, String value) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundUpdateConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("c2s_update_config"));

    public static final StreamCodec<ByteBuf, ServerboundUpdateConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerboundUpdateConfigPayload::key,
            ByteBufCodecs.STRING_UTF8, ServerboundUpdateConfigPayload::value,
            ServerboundUpdateConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
