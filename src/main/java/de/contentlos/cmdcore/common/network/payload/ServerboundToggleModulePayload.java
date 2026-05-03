package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Aktiviert/Deaktiviert/Reload eines Moduls. */
public record ServerboundToggleModulePayload(String moduleId, String action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundToggleModulePayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("c2s_toggle_module"));

    public static final StreamCodec<ByteBuf, ServerboundToggleModulePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerboundToggleModulePayload::moduleId,
            ByteBufCodecs.STRING_UTF8, ServerboundToggleModulePayload::action,
            ServerboundToggleModulePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
