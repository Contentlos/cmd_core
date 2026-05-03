package de.contentlos.cmdcore.common.service;

import de.contentlos.cmdcore.CMDCore;
import de.contentlos.cmdcore.common.config.NWConfig;
import de.contentlos.cmdcore.common.modules.Module;
import de.contentlos.cmdcore.common.modules.ModuleRegistry;
import de.contentlos.cmdcore.common.network.payload.ClientboundOverviewPayload;
import java.util.StringJoiner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Liefert Statusdaten des Servers für das Admin-Panel.
 */
public final class ServerStatusService {

    private final MinecraftServer server;

    public ServerStatusService(MinecraftServer server) {
        this.server = server;
    }

    public double averageTps() {
        try {
            long avgNs = server.getAverageTickTimeNanos();
            if (avgNs <= 0) return 20.0;
            double tps = 1_000_000_000.0 / Math.max(avgNs, 50_000_000.0);
            return Math.min(20.0, Math.round(tps * 100.0) / 100.0);
        } catch (Throwable ignored) {
            return 20.0;
        }
    }

    public ClientboundOverviewPayload buildOverview(CMDCoreServices svc) {
        ModuleRegistry modules = svc.modules();
        StringJoiner mods = new StringJoiner(", ");
        int active = 0;
        for (Module m : modules.sorted()) {
            if (m.isEnabled()) {
                mods.add(m.displayName());
                active++;
            }
        }
        StringJoiner dims = new StringJoiner(", ");
        for (ServerLevel level : server.getAllLevels()) {
            dims.add(level.dimension().location().toString());
        }
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);

        long uptimeSec = (System.currentTimeMillis() - svc.bootTimeMs()) / 1000L;
        String mcVersion = server.getServerVersion();
        String neoVersion = "?";
        try {
            neoVersion = FMLLoader.versionInfo().neoForgeVersion();
        } catch (Throwable ignored) {
            try {
                var info = ModList.get().getModFileById("neoforge");
                if (info != null) {
                    neoVersion = info.toString();
                }
            } catch (Throwable ignored2) {}
        }

        return new ClientboundOverviewPayload(
                server.getMotd(),
                mcVersion,
                neoVersion,
                CMDCore.VERSION,
                server.getPlayerCount(),
                server.getMaxPlayers(),
                averageTps(),
                uptimeSec,
                worldName(),
                dims.toString(),
                active + " aktiv: " + mods,
                NWConfig.GENERAL.enableDebugMode.get(),
                NWConfig.PERMISSIONS.enablePermissionSystem.get() ? "AKTIV" : "DEAKTIVIERT",
                NWConfig.LOGGING.enableAuditLog.get() ? "AKTIV (" + svc.audit().size() + " Einträge)" : "DEAKTIVIERT",
                usedMb,
                maxMb
        );
    }

    private String worldName() {
        try {
            return server.getWorldData().getLevelName();
        } catch (Throwable ignored) {
            return "world";
        }
    }
}
