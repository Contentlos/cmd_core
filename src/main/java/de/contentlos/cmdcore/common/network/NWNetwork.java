package de.contentlos.cmdcore.common.network;

import de.contentlos.cmdcore.CMDCore;
import de.contentlos.cmdcore.common.network.payload.ClientboundActionResultPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundConfigPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundDebugPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundLogListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundModuleListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundOpenPanelPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundOverviewPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPermissionListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPlayerListPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundActionPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundToggleModulePayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundUpdateConfigPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundUpdatePermissionPayload;
import de.contentlos.cmdcore.common.network.handler.ServerboundHandlers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Zentrale Registrierung aller Netzwerk-Payloads.
 *
 * <p>Wird beim {@link RegisterPayloadHandlersEvent} aufgerufen. Die
 * Versionierung des Protokolls befindet sich in {@link #PROTOCOL_VERSION}.</p>
 */
public final class NWNetwork {

    public static final String PROTOCOL_VERSION = "1";

    private NWNetwork() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CMDCore.MOD_ID, path);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Serverbound (Client → Server)
        registrar.playToServer(ServerboundRequestPayload.TYPE,
                ServerboundRequestPayload.STREAM_CODEC,
                ServerboundHandlers::handleRequest);

        registrar.playToServer(ServerboundActionPayload.TYPE,
                ServerboundActionPayload.STREAM_CODEC,
                ServerboundHandlers::handleAction);

        registrar.playToServer(ServerboundUpdatePermissionPayload.TYPE,
                ServerboundUpdatePermissionPayload.STREAM_CODEC,
                ServerboundHandlers::handleUpdatePermission);

        registrar.playToServer(ServerboundUpdateConfigPayload.TYPE,
                ServerboundUpdateConfigPayload.STREAM_CODEC,
                ServerboundHandlers::handleUpdateConfig);

        registrar.playToServer(ServerboundToggleModulePayload.TYPE,
                ServerboundToggleModulePayload.STREAM_CODEC,
                ServerboundHandlers::handleToggleModule);

        // Clientbound (Server → Client) – nur registrieren, Handler werden
        // via Dist.CLIENT-Subscriber aktiviert. So fliegt kein Client-Code
        // auf den Dedicated-Server.
        registrar.playToClient(ClientboundOpenPanelPayload.TYPE,
                ClientboundOpenPanelPayload.STREAM_CODEC,
                ClientHandlerBridge::onOpenPanel);

        registrar.playToClient(ClientboundOverviewPayload.TYPE,
                ClientboundOverviewPayload.STREAM_CODEC,
                ClientHandlerBridge::onOverview);

        registrar.playToClient(ClientboundPlayerListPayload.TYPE,
                ClientboundPlayerListPayload.STREAM_CODEC,
                ClientHandlerBridge::onPlayerList);

        registrar.playToClient(ClientboundPermissionListPayload.TYPE,
                ClientboundPermissionListPayload.STREAM_CODEC,
                ClientHandlerBridge::onPermissionList);

        registrar.playToClient(ClientboundLogListPayload.TYPE,
                ClientboundLogListPayload.STREAM_CODEC,
                ClientHandlerBridge::onLogList);

        registrar.playToClient(ClientboundModuleListPayload.TYPE,
                ClientboundModuleListPayload.STREAM_CODEC,
                ClientHandlerBridge::onModuleList);

        registrar.playToClient(ClientboundConfigPayload.TYPE,
                ClientboundConfigPayload.STREAM_CODEC,
                ClientHandlerBridge::onConfig);

        registrar.playToClient(ClientboundDebugPayload.TYPE,
                ClientboundDebugPayload.STREAM_CODEC,
                ClientHandlerBridge::onDebug);

        registrar.playToClient(ClientboundActionResultPayload.TYPE,
                ClientboundActionResultPayload.STREAM_CODEC,
                ClientHandlerBridge::onActionResult);
    }
}
