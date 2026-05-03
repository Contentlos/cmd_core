package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Generische Admin-Aktion vom Client an den Server.
 *
 * <p>{@code targetUuid} ist eine String-Repräsentation oder leer, wenn es
 * keinen Zielspieler gibt. {@code stringArg} wird je nach Action interpretiert
 * (z.B. Gamemode-Name, Wettertyp, Broadcast-Text).</p>
 */
public record ServerboundActionPayload(Action action, String targetUuid, String stringArg, int intArg) implements CustomPacketPayload {

    public enum Action {
        HEAL,
        FEED,
        FLY_TOGGLE,
        SET_GAMEMODE,         // stringArg = SURVIVAL/CREATIVE/ADVENTURE/SPECTATOR
        TELEPORT_TO_TARGET,   // teleportiere Admin zu Target
        TELEPORT_HERE,        // teleportiere Target zu Admin
        KICK,                 // stringArg = Grund
        BROADCAST,            // stringArg = Nachricht
        TITLE_TO_TARGET,      // stringArg = Titel
        SET_DAY,
        SET_NIGHT,
        SET_WEATHER,          // stringArg = clear/rain/thunder
        SAVE_WORLD,
        SAVE_DATA,
        RELOAD_CONFIG,
        RELOAD_PERMS,
        RELOAD_MODULES,
        TOGGLE_DEBUG,
        REFRESH_OP_CACHE,
        CLEAR_HISTORY,
        SHOW_INFO,            // gibt Spielerinfo nur an Admin zurück
        COPY_COORDINATES,     // bestätigt Koordinaten an Admin
        DEBUG_TEST_MESSAGE
    }

    public static final CustomPacketPayload.Type<ServerboundActionPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("c2s_action"));

    public static final StreamCodec<ByteBuf, ServerboundActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Action.values()[i], Enum::ordinal),
            ServerboundActionPayload::action,
            ByteBufCodecs.STRING_UTF8,
            ServerboundActionPayload::targetUuid,
            ByteBufCodecs.STRING_UTF8,
            ServerboundActionPayload::stringArg,
            ByteBufCodecs.VAR_INT,
            ServerboundActionPayload::intArg,
            ServerboundActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
