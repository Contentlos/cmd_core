package de.contentlos.cmdcore.common.service;

import de.contentlos.cmdcore.common.config.NWConfig;
import de.contentlos.cmdcore.common.logging.AuditService;
import de.contentlos.cmdcore.common.modules.CoreModule;
import de.contentlos.cmdcore.common.modules.Module;
import de.contentlos.cmdcore.common.modules.ModuleRegistry;
import de.contentlos.cmdcore.common.modules.StandardModules;
import de.contentlos.cmdcore.common.permissions.PermissionLevel;
import de.contentlos.cmdcore.common.permissions.PermissionService;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * Lebensdauer-Container aller serverseitigen Services.
 *
 * <p>Wird beim ServerStarted-Event gebaut und beim ServerStopping wieder
 * abgebaut, um Memory-Leaks zu vermeiden.</p>
 *
 * <p>Hier liegt auch der Zugriff auf {@link MinecraftServer}, sodass die
 * Services nicht überall den Server selbst herumreichen müssen.</p>
 */
public final class CMDCoreServices {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile CMDCoreServices instance;

    private final MinecraftServer server;
    private final PermissionService permissions;
    private final AuditService audit;
    private final ModuleRegistry modules;
    private final ServerStatusService status;
    private final PlayerService players;
    private final AdminActionService adminActions;
    private final ConfigSnapshotService configs;
    private final long bootTimeMs;
    private boolean debugActive;

    private CMDCoreServices(MinecraftServer server) {
        this.server = server;
        this.permissions = new PermissionService(server);
        this.audit = new AuditService(server);
        this.modules = new ModuleRegistry(server);
        this.status = new ServerStatusService(server);
        this.players = new PlayerService(server, permissions);
        this.adminActions = new AdminActionService(server, permissions, audit);
        this.configs = new ConfigSnapshotService();
        this.bootTimeMs = System.currentTimeMillis();
    }

    /** Initialisiert die Services beim Serverstart. */
    public static CMDCoreServices init(MinecraftServer server) {
        CMDCoreServices svc = new CMDCoreServices(server);
        instance = svc;

        // Configs einlesen
        try {
            svc.permissions.setOpFallbackEnabled(NWConfig.PERMISSIONS.allowOpFallback.get());
            svc.permissions.setDefaultLevel(NWConfig.PERMISSIONS.defaultPermissionLevel.get());
            svc.audit.setMaxEntries(NWConfig.LOGGING.maxHistoryEntries.get());
            svc.debugActive = NWConfig.GENERAL.enableDebugMode.get();
        } catch (Exception e) {
            LOGGER.warn("CMD-Core: Konnte Config nicht direkt anwenden – nutze Defaults", e);
        }

        // Module registrieren
        svc.modules.register(new CoreModule());
        svc.modules.register(StandardModules.permissions());
        svc.modules.register(StandardModules.playerManagement());
        svc.modules.register(StandardModules.adminPanel());
        svc.modules.register(StandardModules.logging());
        svc.modules.register(StandardModules.server());
        Module debugModule = StandardModules.debug();
        svc.modules.register(debugModule);
        if (svc.debugActive) {
            debugModule.enable();
        }

        // Daten einlesen
        svc.permissions.load();
        svc.audit.load();
        svc.modules.load();

        // Owner-UUIDs aus Config in Permission-Liste forcieren
        try {
            for (Object raw : NWConfig.PERMISSIONS.ownerUUIDs.get()) {
                if (raw instanceof String s) {
                    try {
                        UUID uuid = UUID.fromString(s.trim());
                        if (svc.permissions.levelOf(uuid) != PermissionLevel.OWNER) {
                            svc.permissions.setLevel(uuid, "?", PermissionLevel.OWNER,
                                    de.contentlos.cmdcore.common.permissions.PermissionEntry.Source.JSON);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CMD-Core: ownerUUIDs konnten nicht angewendet werden", e);
        }

        svc.audit.recordSystem("Server gestartet", true,
                "NightWatch Core / CMD Core initialisiert", "core");
        return svc;
    }

    public static @Nullable CMDCoreServices get() {
        return instance;
    }

    /** Wirft eine Runtime-Exception wenn der Container nicht aktiv ist. */
    public static CMDCoreServices required() {
        CMDCoreServices svc = instance;
        if (svc == null) {
            throw new IllegalStateException("CMD-Core Services sind nicht initialisiert");
        }
        return svc;
    }

    /** Fährt die Services herunter. */
    public static void shutdown() {
        CMDCoreServices svc = instance;
        if (svc == null) return;
        try {
            svc.permissions.save();
            svc.modules.save();
            svc.audit.recordSystem("Server stoppt", true, null, "core");
            svc.audit.save();
        } catch (Exception e) {
            LOGGER.error("CMD-Core: Fehler beim Speichern beim Shutdown", e);
        }
        instance = null;
    }

    public MinecraftServer server() { return server; }
    public PermissionService permissions() { return permissions; }
    public AuditService audit() { return audit; }
    public ModuleRegistry modules() { return modules; }
    public ServerStatusService status() { return status; }
    public PlayerService players() { return players; }
    public AdminActionService adminActions() { return adminActions; }
    public ConfigSnapshotService configs() { return configs; }
    public long bootTimeMs() { return bootTimeMs; }
    public boolean debugActive() { return debugActive; }

    public void setDebugActive(boolean v) {
        this.debugActive = v;
    }
}
