package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Übersichts-Daten für das Panel.
 * Strings statt strukturierter Felder für möglichst flexible Anzeige.
 */
public record ClientboundOverviewPayload(
        String serverName,
        String mcVersion,
        String neoForgeVersion,
        String coreVersion,
        int playersOnline,
        int playersMax,
        double tps,
        long uptimeSeconds,
        String worldName,
        String dimensions,
        String loadedModules,
        boolean debugActive,
        String permissionStatus,
        String auditStatus,
        long memoryUsedMb,
        long memoryMaxMb
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundOverviewPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("s2c_overview"));

    public static final StreamCodec<ByteBuf, ClientboundOverviewPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ClientboundOverviewPayload decode(ByteBuf buf) {
            return new ClientboundOverviewPayload(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf)
            );
        }

        @Override
        public void encode(ByteBuf buf, ClientboundOverviewPayload p) {
            ByteBufCodecs.STRING_UTF8.encode(buf, p.serverName);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.mcVersion);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.neoForgeVersion);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.coreVersion);
            ByteBufCodecs.VAR_INT.encode(buf, p.playersOnline);
            ByteBufCodecs.VAR_INT.encode(buf, p.playersMax);
            ByteBufCodecs.DOUBLE.encode(buf, p.tps);
            ByteBufCodecs.VAR_LONG.encode(buf, p.uptimeSeconds);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.worldName);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.dimensions);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.loadedModules);
            ByteBufCodecs.BOOL.encode(buf, p.debugActive);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.permissionStatus);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.auditStatus);
            ByteBufCodecs.VAR_LONG.encode(buf, p.memoryUsedMb);
            ByteBufCodecs.VAR_LONG.encode(buf, p.memoryMaxMb);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
