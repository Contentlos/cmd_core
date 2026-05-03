package de.contentlos.cmdcore.client.network;

import de.contentlos.cmdcore.client.gui.AdminPanelScreen;
import de.contentlos.cmdcore.client.gui.PanelState;
import de.contentlos.cmdcore.common.network.payload.ClientboundActionResultPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundConfigPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundDebugPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundLogListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundModuleListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundOpenPanelPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundOverviewPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPermissionListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPlayerListPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Clientseitiger Empfang aller Server-Payloads.
 *
 * <p>Wird ausschließlich auf {@link Dist#CLIENT} ausgeführt.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientNetworkHandler {

    private ClientNetworkHandler() {}

    public static void handleOpenPanel(ClientboundOpenPanelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            PanelState state = PanelState.get();
            state.setAccentColor(payload.accentColor());
            state.setOwnLevel(payload.playerLevel());
            state.setActiveTab(payload.defaultTab());
            Minecraft.getInstance().setScreen(new AdminPanelScreen());
        });
    }

    public static void handleOverview(ClientboundOverviewPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PanelState.get().setOverview(payload));
    }

    public static void handlePlayerList(ClientboundPlayerListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PanelState.get().setPlayers(payload));
    }

    public static void handlePermissionList(ClientboundPermissionListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PanelState.get().setPermissions(payload));
    }

    public static void handleLogList(ClientboundLogListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PanelState.get().setLogs(payload));
    }

    public static void handleModuleList(ClientboundModuleListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PanelState.get().setModules(payload));
    }

    public static void handleConfig(ClientboundConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PanelState.get().setConfig(payload));
    }

    public static void handleDebug(ClientboundDebugPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PanelState.get().setDebug(payload));
    }

    public static void handleActionResult(ClientboundActionResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            String prefix = payload.success() ? "§a[NightWatch] §r" : "§c[NightWatch] §r";
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(prefix + payload.message()), false);
                if (!payload.hint().isEmpty()) {
                    mc.player.displayClientMessage(Component.literal("§7" + payload.hint()), false);
                }
            }
            PanelState.get().setLastResult(payload);
        });
    }
}
