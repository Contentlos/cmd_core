package de.contentlos.cmdcore.common.data;

import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Zentrale Pfadberechnung für persistente CMD-Core-Daten.
 *
 * <p>Alle Daten landen unter <code>&lt;welt&gt;/serverconfig/nightwatch/</code>.
 * Dadurch sind sie pro Welt getrennt und werden mit dem Welt-Backup
 * mitgesichert.</p>
 */
public final class CMDDataPaths {

    public static final String DIRECTORY_NAME = "nightwatch";

    public static final String FILE_PERMISSIONS = "permissions.json";
    public static final String FILE_ADMIN_HISTORY = "admin_history.json";
    public static final String FILE_PLAYER_CACHE = "player_cache.json";
    public static final String FILE_MODULE_STATES = "module_states.json";
    public static final String FILE_PANEL_SETTINGS = "panel_settings.json";

    private CMDDataPaths() {
    }

    /** Wurzelpfad für CMD-Core-Daten in der aktuellen Welt. */
    public static Path rootDir(MinecraftServer server) {
        Path serverConfig = server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig");
        return serverConfig.resolve(DIRECTORY_NAME);
    }

    public static Path file(MinecraftServer server, String fileName) {
        return rootDir(server).resolve(fileName);
    }
}
