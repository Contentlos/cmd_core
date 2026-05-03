package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Generische Anfrage des Clients an den Server: Daten für einen Tab nachladen
 * oder das Panel überhaupt erst öffnen.
 *
 * <p>Optionaler {@code parameter} wird je nach {@link Kind} interpretiert
 * (z.B. Logfilter, Spielername-Suche).</p>
 */
public record ServerboundRequestPayload(Kind kind, String parameter, int limit) implements CustomPacketPayload {

    public enum Kind {
        OPEN_PANEL,
        OVERVIEW,
        PLAYERS,
        PERMISSIONS,
        MODULES,
        CONFIG,
        LOGS,
        HISTORY,
        DEBUG,
        SAVE_ALL,
        REFRESH
    }

    public static final CustomPacketPayload.Type<ServerboundRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("c2s_request"));

    public static final StreamCodec<ByteBuf, ServerboundRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Kind.values()[i & 0xFF], Enum::ordinal),
            ServerboundRequestPayload::kind,
            ByteBufCodecs.STRING_UTF8,
            ServerboundRequestPayload::parameter,
            ByteBufCodecs.VAR_INT,
            ServerboundRequestPayload::limit,
            ServerboundRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
