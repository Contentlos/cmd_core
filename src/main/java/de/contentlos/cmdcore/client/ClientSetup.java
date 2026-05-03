package de.contentlos.cmdcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.contentlos.cmdcore.CMDCore;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Clientseitige Initialisierung.
 *
 * <p>Registriert das Hotkey-Mapping und reagiert pro Tick auf einen
 * Tastendruck, indem es eine {@link ServerboundRequestPayload}
 * {@code OPEN_PANEL} an den Server schickt. Die Berechtigung wird
 * serverseitig geprüft.</p>
 */
@Mod(value = CMDCore.MOD_ID, dist = Dist.CLIENT)
public final class ClientSetup {

    public static final KeyMapping OPEN_PANEL_KEY = new KeyMapping(
            "key.nightwatch_core.open_panel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            "key.categories.nightwatch_core"
    );

    public ClientSetup(IEventBus modBus, ModContainer container) {
        modBus.addListener(ClientSetup::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.register(ClientForgeEvents.class);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PANEL_KEY);
    }

    @EventBusSubscriber(modid = CMDCore.MOD_ID, value = Dist.CLIENT)
    public static final class ClientForgeEvents {
        @SubscribeEvent
        public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.screen != null) return;
            while (OPEN_PANEL_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new ServerboundRequestPayload(
                        ServerboundRequestPayload.Kind.OPEN_PANEL, "", 0));
            }
        }
    }
}
