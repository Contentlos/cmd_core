package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Ergebnis einer Aktion an den Client zurückgemeldet. */
public record ClientboundActionResultPayload(boolean success, String message, String hint) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundActionResultPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("s2c_action_result"));

    public static final StreamCodec<ByteBuf, ClientboundActionResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ClientboundActionResultPayload::success,
            ByteBufCodecs.STRING_UTF8, ClientboundActionResultPayload::message,
            ByteBufCodecs.STRING_UTF8, ClientboundActionResultPayload::hint,
            ClientboundActionResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
