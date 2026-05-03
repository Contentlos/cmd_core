package de.contentlos.cmdcore.common.service;

import de.contentlos.cmdcore.common.network.payload.ClientboundPlayerListPayload;
import de.contentlos.cmdcore.common.permissions.PermissionService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Liefert detaillierte Spielerinformationen.
 */
public final class PlayerService {

    private final MinecraftServer server;
    private final PermissionService permissions;

    public PlayerService(MinecraftServer server, PermissionService permissions) {
        this.server = server;
        this.permissions = permissions;
    }

    public ClientboundPlayerListPayload buildPlayerList() {
        List<ClientboundPlayerListPayload.Entry> list = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            list.add(buildEntry(player));
        }
        return new ClientboundPlayerListPayload(list);
    }

    public ClientboundPlayerListPayload.Entry buildEntry(ServerPlayer player) {
        GameType gameType = player.gameMode.getGameModeForPlayer();
        boolean isOp = server.getPlayerList().isOp(player.getGameProfile());
        int ping = 0;
        try {
            ping = player.connection != null ? player.connection.latency() : 0;
        } catch (Throwable ignored) {}
        return new ClientboundPlayerListPayload.Entry(
                player.getGameProfile().getName(),
                player.getUUID().toString(),
                permissions.levelOf(player.getUUID()).name(),
                isOp,
                gameType != null ? gameType.getName() : "?",
                player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getAbilities().flying,
                ping
        );
    }
}
