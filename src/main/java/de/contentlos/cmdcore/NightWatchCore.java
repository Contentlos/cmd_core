package de.contentlos.cmdcore;

import com.mojang.logging.LogUtils;
import de.contentlos.cmdcore.common.commands.NWCommands;
import de.contentlos.cmdcore.common.config.NWConfig;
import de.contentlos.cmdcore.common.network.NWNetwork;
import de.contentlos.cmdcore.common.service.CMDCoreServices;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

/**
 * Haupt-Mod-Klasse von NightWatch Core.
 *
 * <p>Lädt Config, registriert Netzwerk-Payloads, initialisiert die Services
 * beim Server-Start und meldet die Befehle an.</p>
 */
@Mod(CMDCore.MOD_ID)
public class NightWatchCore {

    public static final Logger LOGGER = LogUtils.getLogger();

    private long lastSaveCheckMs = 0L;

    public NightWatchCore(IEventBus modBus, ModContainer container) {
        // Config registrieren – pro Server eine eigene TOML.
        container.registerConfig(ModConfig.Type.SERVER, NWConfig.SPEC, "nightwatch_core-server.toml");

        // Mod-Bus-Events
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(NWNetwork::register);

        // Forge-Bus-Events
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        LOGGER.info("NightWatch Core / CMD Core wird geladen (Mod-ID {}).", CMDCore.MOD_ID);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("CMD-Core Common-Setup abgeschlossen.");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        NWCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    private void onServerStarted(ServerStartedEvent event) {
        try {
            CMDCoreServices.init(event.getServer());
            LOGGER.info("CMD-Core Services initialisiert.");
        } catch (Throwable t) {
            LOGGER.error("CMD-Core: Konnte Services nicht initialisieren", t);
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        try {
            CMDCoreServices.shutdown();
            LOGGER.info("CMD-Core Services heruntergefahren.");
        } catch (Throwable t) {
            LOGGER.error("CMD-Core: Fehler beim Herunterfahren", t);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        // Auto-Save in Intervallen
        long now = System.currentTimeMillis();
        if (lastSaveCheckMs == 0L) {
            lastSaveCheckMs = now;
            return;
        }
        long intervalSec = NWConfig.GENERAL.dataSaveIntervalSeconds.get();
        if (intervalSec <= 0) return;
        if (now - lastSaveCheckMs < intervalSec * 1000L) return;
        lastSaveCheckMs = now;
        CMDCoreServices svc = CMDCoreServices.get();
        if (svc == null) return;
        try {
            svc.audit().saveIfDirty();
        } catch (Throwable t) {
            LOGGER.warn("CMD-Core: Auto-Save fehlgeschlagen", t);
        }
    }
}
