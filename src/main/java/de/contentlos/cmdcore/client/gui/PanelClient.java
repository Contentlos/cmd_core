package de.contentlos.cmdcore.client.gui;

import de.contentlos.cmdcore.common.network.payload.ServerboundActionPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundToggleModulePayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundUpdateConfigPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundUpdatePermissionPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Hilfsmethoden, um Anfragen vom Client an den Server zu senden.
 */
@OnlyIn(Dist.CLIENT)
public final class PanelClient {

    private PanelClient() {}

    public static void sendRequest(ServerboundRequestPayload.Kind kind) {
        send(new ServerboundRequestPayload(kind, "", 0));
    }

    public static void sendRequest(ServerboundRequestPayload.Kind kind, String parameter, int limit) {
        send(new ServerboundRequestPayload(kind, parameter == null ? "" : parameter, limit));
    }

    public static void sendAction(ServerboundActionPayload.Action action, String targetUuid, String stringArg, int intArg) {
        send(new ServerboundActionPayload(action,
                targetUuid == null ? "" : targetUuid,
                stringArg == null ? "" : stringArg,
                intArg));
    }

    public static void sendUpdatePermission(String action, String uuid, String name, String level) {
        send(new ServerboundUpdatePermissionPayload(
                nullSafe(action), nullSafe(uuid), nullSafe(name), nullSafe(level)));
    }

    public static void sendToggleModule(String moduleId, String action) {
        send(new ServerboundToggleModulePayload(nullSafe(moduleId), nullSafe(action)));
    }

    public static void sendUpdateConfig(String key, String value) {
        send(new ServerboundUpdateConfigPayload(nullSafe(key), nullSafe(value)));
    }

    private static void send(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
