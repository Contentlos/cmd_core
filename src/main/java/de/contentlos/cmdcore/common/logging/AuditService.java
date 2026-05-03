package de.contentlos.cmdcore.common.logging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import de.contentlos.cmdcore.common.data.CMDDataPaths;
import de.contentlos.cmdcore.common.data.JsonStorage;
import de.contentlos.cmdcore.common.permissions.PermissionLevel;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Zentrale Audit-Log-Verwaltung.
 *
 * <p>Hält die letzten N Einträge im RAM und persistiert sie regelmäßig in
 * <code>admin_history.json</code>. Die Maximalzahl ist konfigurierbar.</p>
 */
public final class AuditService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final MinecraftServer server;
    private final Deque<AuditEntry> entries = new ArrayDeque<>();
    private int maxEntries = 500;
    private boolean dirty = false;

    public AuditService(MinecraftServer server) {
        this.server = server;
    }

    public void setMaxEntries(int max) {
        this.maxEntries = Math.max(50, max);
        trim();
    }

    public synchronized void load() {
        entries.clear();
        Path file = CMDDataPaths.file(server, CMDDataPaths.FILE_ADMIN_HISTORY);
        JsonElement root = JsonStorage.readJson(file);
        if (root == null) {
            return;
        }
        try {
            JsonArray array;
            if (root.isJsonArray()) {
                array = root.getAsJsonArray();
            } else if (root.isJsonObject() && root.getAsJsonObject().has("entries")) {
                array = root.getAsJsonObject().getAsJsonArray("entries");
            } else {
                return;
            }
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();
                AuditEntry entry = new AuditEntry(
                        get(obj, "timestamp", 0L),
                        getString(obj, "adminName", "?"),
                        getUuid(obj, "adminUuid"),
                        getString(obj, "adminLevel", "USER"),
                        getString(obj, "targetName", null),
                        getUuid(obj, "targetUuid"),
                        getString(obj, "action", "?"),
                        parseSource(getString(obj, "source", "SYSTEM")),
                        obj.has("success") && obj.get("success").getAsBoolean(),
                        getString(obj, "details", null),
                        getString(obj, "module", null)
                );
                entries.add(entry);
            }
            trim();
        } catch (Exception e) {
            LOGGER.error("CMD-Core: Audit-Log konnte nicht vollständig geladen werden", e);
        }
    }

    private static AuditEntry.Source parseSource(String raw) {
        try {
            return AuditEntry.Source.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return AuditEntry.Source.SYSTEM;
        }
    }

    private static String getString(JsonObject obj, String key, String def) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return def;
        return obj.get(key).getAsString();
    }

    private static long get(JsonObject obj, String key, long def) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return def;
        return obj.get(key).getAsLong();
    }

    @Nullable
    private static UUID getUuid(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        try {
            return UUID.fromString(obj.get(key).getAsString());
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized void save() {
        try {
            Path file = CMDDataPaths.file(server, CMDDataPaths.FILE_ADMIN_HISTORY);
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (AuditEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("timestamp", e.timestamp());
                obj.addProperty("adminName", e.adminName());
                if (e.adminUuid() != null) obj.addProperty("adminUuid", e.adminUuid().toString());
                obj.addProperty("adminLevel", e.adminLevel());
                if (e.targetName() != null) obj.addProperty("targetName", e.targetName());
                if (e.targetUuid() != null) obj.addProperty("targetUuid", e.targetUuid().toString());
                obj.addProperty("action", e.action());
                obj.addProperty("source", e.source().name());
                obj.addProperty("success", e.success());
                if (e.details() != null) obj.addProperty("details", e.details());
                if (e.module() != null) obj.addProperty("module", e.module());
                array.add(obj);
            }
            root.add("entries", array);
            JsonStorage.writeObject(file, root);
            dirty = false;
        } catch (Exception e) {
            LOGGER.error("CMD-Core: Audit-Log konnte nicht gespeichert werden", e);
        }
    }

    public synchronized void saveIfDirty() {
        if (dirty) save();
    }

    private void trim() {
        while (entries.size() > maxEntries) {
            entries.pollFirst();
        }
    }

    public synchronized void record(AuditEntry entry) {
        if (entry == null) return;
        entries.addLast(entry);
        trim();
        dirty = true;
    }

    public synchronized List<AuditEntry> snapshot() {
        return new ArrayList<>(entries);
    }

    public synchronized List<AuditEntry> filter(Predicate<AuditEntry> filter, int limit) {
        List<AuditEntry> out = new ArrayList<>();
        Iterable<AuditEntry> reversed = () -> entries.descendingIterator();
        for (AuditEntry e : reversed) {
            if (filter == null || filter.test(e)) {
                out.add(e);
                if (limit > 0 && out.size() >= limit) break;
            }
        }
        return out;
    }

    public synchronized void clear() {
        entries.clear();
        dirty = true;
    }

    public synchronized int size() { return entries.size(); }

    /** Bequemer Konstruktor zum Loggen aus Befehlen. */
    public void recordCommand(CommandSourceStack source, PermissionLevel adminLevel,
                              @Nullable String targetName, @Nullable UUID targetUuid,
                              String action, boolean success, @Nullable String details, String module) {
        record(new AuditEntry(
                System.currentTimeMillis(),
                source.getTextName(),
                source.getEntity() instanceof ServerPlayer p ? p.getUUID() : null,
                adminLevel == null ? "?" : adminLevel.name(),
                targetName, targetUuid, action,
                AuditEntry.Source.COMMAND, success, details, module
        ));
    }

    /** Bequemer Konstruktor zum Loggen aus dem Admin-Panel. */
    public void recordPanel(ServerPlayer admin, PermissionLevel adminLevel,
                            @Nullable String targetName, @Nullable UUID targetUuid,
                            String action, boolean success, @Nullable String details, String module) {
        record(new AuditEntry(
                System.currentTimeMillis(),
                admin != null ? admin.getGameProfile().getName() : "?",
                admin != null ? admin.getUUID() : null,
                adminLevel == null ? "?" : adminLevel.name(),
                targetName, targetUuid, action,
                AuditEntry.Source.PANEL, success, details, module
        ));
    }

    public void recordSystem(String action, boolean success, @Nullable String details, String module) {
        record(new AuditEntry(
                System.currentTimeMillis(),
                "System", null, PermissionLevel.CONSOLE.name(),
                null, null, action, AuditEntry.Source.SYSTEM, success, details, module
        ));
    }
}
