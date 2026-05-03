package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Permission-Änderung vom Panel.
 *
 * <p>{@code action} = "SET" oder "REMOVE".
 * {@code level} = z.B. "ADMIN" (bei "SET" verpflichtend, sonst leer).
 * {@code targetUuid} = UUID als String.</p>
 */
public record ServerboundUpdatePermissionPayload(String action, String targetUuid, String targetName, String level) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundUpdatePermissionPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("c2s_update_permission"));

    public static final StreamCodec<ByteBuf, ServerboundUpdatePermissionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerboundUpdatePermissionPayload::action,
            ByteBufCodecs.STRING_UTF8, ServerboundUpdatePermissionPayload::targetUuid,
            ByteBufCodecs.STRING_UTF8, ServerboundUpdatePermissionPayload::targetName,
            ByteBufCodecs.STRING_UTF8, ServerboundUpdatePermissionPayload::level,
            ServerboundUpdatePermissionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
