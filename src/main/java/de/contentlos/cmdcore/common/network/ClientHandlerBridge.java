package de.contentlos.cmdcore.common.network;

import de.contentlos.cmdcore.common.network.payload.ClientboundActionResultPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundConfigPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundDebugPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundLogListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundModuleListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundOpenPanelPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundOverviewPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPermissionListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPlayerListPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Trennt Server- von Client-Code in den Payload-Handlern.
 *
 * <p>Auf einem Dedicated-Server existiert {@code DistExecutor.SafeRunnable}
 * für Client-Code nicht – stattdessen prüfen wir das aktuelle Dist und
 * delegieren nur dann an {@code de.contentlos.cmdcore.client.network.ClientNetworkHandler}.</p>
 *
 * <p>So vermeiden wir, dass Client-Klassen vom Server-Code geladen werden,
 * obwohl die Payload-Klassen selbst (Records) Common-Code sind.</p>
 */
public final class ClientHandlerBridge {

    private ClientHandlerBridge() {}

    public static void onOpenPanel(ClientboundOpenPanelPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            de.contentlos.cmdcore.client.network.ClientNetworkHandler.handleOpenPanel(payload, context);
        }
    }

    public static void onOverview(ClientboundOverviewPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            de.contentlos.cmdcore.client.network.ClientNetworkHandler.handleOverview(payload, context);
        }
    }

    public static void onPlayerList(ClientboundPlayerListPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            de.contentlos.cmdcore.client.network.ClientNetworkHandler.handlePlayerList(payload, context);
        }
    }

    public static void onPermissionList(ClientboundPermissionListPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            de.contentlos.cmdcore.client.network.ClientNetworkHandler.handlePermissionList(payload, context);
        }
    }

    public static void onLogList(ClientboundLogListPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            de.contentlos.cmdcore.client.network.ClientNetworkHandler.handleLogList(payload, context);
        }
    }

    public static void onModuleList(ClientboundModuleListPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            de.contentlos.cmdcore.client.network.ClientNetworkHandler.handleModuleList(payload, context);
        }
    }

    public static void onConfig(ClientboundConfigPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            de.contentlos.cmdcore.client.network.ClientNetworkHandler.handleConfig(payload, context);
        }
    }

    public static void onDebug(ClientboundDebugPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            de.contentlos.cmdcore.client.network.ClientNetworkHandler.handleDebug(payload, context);
        }
    }

    public static void onActionResult(ClientboundActionResultPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            de.contentlos.cmdcore.client.network.ClientNetworkHandler.handleActionResult(payload, context);
        }
    }
}
