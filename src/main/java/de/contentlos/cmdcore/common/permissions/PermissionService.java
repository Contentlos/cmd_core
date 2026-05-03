package de.contentlos.cmdcore.common.permissions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import de.contentlos.cmdcore.common.data.CMDDataPaths;
import de.contentlos.cmdcore.common.data.JsonStorage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;

/**
 * Zentrale Verwaltung der Permission-Level.
 *
 * <p>Eigenschaften:</p>
 * <ul>
 *   <li>Pro Welt persistiert in <code>permissions.json</code>.</li>
 *   <li>Cache läuft im Hauptspeicher, lazy nachgeladen.</li>
 *   <li>Vanilla-OP-Fallback optional aktivierbar.</li>
 *   <li>Konsole hat immer {@link PermissionLevel#CONSOLE}.</li>
 * </ul>
 */
public final class PermissionService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final MinecraftServer server;
    private final Map<UUID, PermissionEntry> entries = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionEntry> opFallbackCache = new ConcurrentHashMap<>();
    private boolean opFallbackEnabled = true;
    private PermissionLevel defaultLevel = PermissionLevel.USER;

    public PermissionService(MinecraftServer server) {
        this.server = server;
    }

    /** Lädt Permissions aus der JSON-Datei. Erstellt fehlende Datei. */
    public synchronized void load() {
        entries.clear();
        Path file = CMDDataPaths.file(server, CMDDataPaths.FILE_PERMISSIONS);
        JsonElement root = JsonStorage.readJson(file);
        if (root == null) {
            saveSilently();
            return;
        }
        try {
            if (root.isJsonObject() && root.getAsJsonObject().has("entries")) {
                JsonArray array = root.getAsJsonObject().getAsJsonArray("entries");
                for (JsonElement element : array) {
                    parseEntry(element);
                }
            } else if (root.isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray()) {
                    parseEntry(element);
                }
            }
        } catch (Exception e) {
            LOGGER.error("CMD-Core: Permission-JSON konnte nicht vollständig geladen werden", e);
        }
    }

    private void parseEntry(JsonElement element) {
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject obj = element.getAsJsonObject();
        try {
            UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
            String name = obj.has("name") ? obj.get("name").getAsString() : "?";
            PermissionLevel level = PermissionLevel.parse(obj.has("level") ? obj.get("level").getAsString() : "USER");
            PermissionEntry entry = new PermissionEntry(uuid, name, level);
            entry.setSource(PermissionEntry.Source.JSON);
            if (obj.has("updatedAt")) {
                // Wir behalten Timestamp aus JSON. Setzt level() ohne den Timestamp zu überschreiben.
                // Der einfachste Weg: neuen Eintrag, dann level setzen, dann updatedAt nicht erneut anfassen.
                // PermissionEntry.setLevel() überschreibt updatedAt – wir akzeptieren den aktuellen Zeitstempel
                // hier bewusst nicht, da updatedAt rein informativ ist.
            }
            entries.put(uuid, entry);
        } catch (Exception e) {
            LOGGER.warn("CMD-Core: Permission-Eintrag wird übersprungen: {}", obj, e);
        }
    }

    /** Speichert alle Einträge. */
    public synchronized void save() {
        try {
            Path file = CMDDataPaths.file(server, CMDDataPaths.FILE_PERMISSIONS);
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (PermissionEntry entry : entries.values()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("uuid", entry.uuid().toString());
                obj.addProperty("name", entry.name());
                obj.addProperty("level", entry.level().name());
                obj.addProperty("updatedAt", entry.updatedAt());
                array.add(obj);
            }
            root.add("entries", array);
            JsonStorage.writeObject(file, root);
        } catch (Exception e) {
            LOGGER.error("CMD-Core: Konnte Permissions nicht speichern", e);
        }
    }

    private void saveSilently() {
        try {
            save();
        } catch (Exception ignored) {
        }
    }

    public void setOpFallbackEnabled(boolean enabled) { this.opFallbackEnabled = enabled; }
    public boolean opFallbackEnabled() { return opFallbackEnabled; }
    public void setDefaultLevel(PermissionLevel level) {
        this.defaultLevel = level == null ? PermissionLevel.USER : level;
    }
    public PermissionLevel defaultLevel() { return defaultLevel; }

    /** Liefert die Stufe eines Spielers (mit OP-Fallback wenn aktiviert). */
    public PermissionLevel levelOf(UUID uuid) {
        if (uuid == null) {
            return PermissionLevel.USER;
        }
        PermissionEntry entry = entries.get(uuid);
        if (entry != null) {
            return entry.level();
        }
        if (opFallbackEnabled) {
            PermissionEntry op = opFallbackEntry(uuid);
            if (op != null) {
                return op.level();
            }
        }
        return defaultLevel;
    }

    /** Liefert die zugehörige Quelle der Permission. */
    public PermissionEntry.Source sourceOf(UUID uuid) {
        if (uuid == null) {
            return PermissionEntry.Source.DEFAULT;
        }
        if (entries.containsKey(uuid)) {
            return PermissionEntry.Source.JSON;
        }
        if (opFallbackEnabled && opFallbackEntry(uuid) != null) {
            return PermissionEntry.Source.OP_FALLBACK;
        }
        return PermissionEntry.Source.DEFAULT;
    }

    /** Setzt eine Stufe und persistiert. */
    public synchronized void setLevel(UUID uuid, String displayName, PermissionLevel level, PermissionEntry.Source source) {
        if (uuid == null || level == null) {
            return;
        }
        if (level == PermissionLevel.USER && entries.containsKey(uuid)) {
            entries.remove(uuid);
            save();
            return;
        }
        PermissionEntry entry = entries.computeIfAbsent(uuid, u -> new PermissionEntry(u, displayName != null ? displayName : "?", PermissionLevel.USER));
        if (displayName != null) {
            entry.setName(displayName);
        }
        entry.setLevel(level);
        entry.setSource(source != null ? source : PermissionEntry.Source.JSON);
        save();
    }

    /** Entfernt einen Spieler aus der Liste (er fällt auf default zurück). */
    public synchronized boolean removeEntry(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        boolean removed = entries.remove(uuid) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    /** Liefert alle gespeicherten Einträge (Snapshot). */
    public List<PermissionEntry> listEntries() {
        List<PermissionEntry> out = new ArrayList<>(entries.values());
        out.sort((a, b) -> Integer.compare(b.level().weight(), a.level().weight()));
        return out;
    }

    /** Sucht eine UUID per Spielername (case-insensitive) im UserCache des Servers. */
    public Optional<UUID> resolveUuid(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        // Online-Spieler zuerst
        PlayerList list = server.getPlayerList();
        ServerPlayer online = list.getPlayerByName(name);
        if (online != null) {
            return Optional.of(online.getUUID());
        }
        var profileCache = server.getProfileCache();
        if (profileCache == null) {
            return Optional.empty();
        }
        return profileCache.get(name).map(p -> p.getId());
    }

    public Optional<String> resolveName(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return Optional.of(online.getGameProfile().getName());
        }
        var profileCache = server.getProfileCache();
        if (profileCache == null) {
            return Optional.empty();
        }
        return profileCache.get(uuid).map(p -> p.getName());
    }

    /** Prüft ob ein Command-Source-Stack die geforderte Stufe erreicht. */
    public boolean has(CommandSourceStack source, PermissionLevel required) {
        return levelOfSource(source).atLeast(required);
    }

    /** Stufe eines CommandSource (Spieler/Konsole). */
    public PermissionLevel levelOfSource(CommandSourceStack source) {
        if (source == null) {
            return PermissionLevel.USER;
        }
        var entity = source.getEntity();
        if (entity instanceof ServerPlayer player) {
            return levelOf(player.getUUID());
        }
        // Kein Spieler dahinter: Konsole, CommandBlock, RCON, Function ...
        // Für Sicherheit nur echte Server-Konsole als CONSOLE erkennen, alles
        // andere als ADMIN behandeln (z.B. CommandBlocks). OWNER-only-Aktionen
        // verlangen explizit OWNER.
        String sourceName = source.getTextName();
        if ("Server".equalsIgnoreCase(sourceName) || "Konsole".equalsIgnoreCase(sourceName)) {
            return PermissionLevel.CONSOLE;
        }
        return PermissionLevel.ADMIN;
    }

    @Nullable
    private PermissionEntry opFallbackEntry(UUID uuid) {
        return opFallbackCache.computeIfAbsent(uuid, this::computeOpFallback);
    }

    @Nullable
    private PermissionEntry computeOpFallback(UUID uuid) {
        try {
            com.mojang.authlib.GameProfile profile = null;
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            if (online != null) {
                profile = online.getGameProfile();
            } else {
                var profileCache = server.getProfileCache();
                if (profileCache != null) {
                    profile = profileCache.get(uuid).orElse(null);
                }
            }
            if (profile == null) {
                return null;
            }
            int opLevel = server.getProfilePermissions(profile);
            if (opLevel <= 0) {
                return null;
            }
            PermissionLevel mapped = mapOpLevel(opLevel);
            String name = profile.getName() != null ? profile.getName() : "?";
            PermissionEntry entry = new PermissionEntry(uuid, name, mapped);
            entry.setSource(PermissionEntry.Source.OP_FALLBACK);
            return entry;
        } catch (Exception e) {
            return null;
        }
    }

    /** Vanilla-OP 1..4 → CMD Permission Level. */
    private PermissionLevel mapOpLevel(int opLevel) {
        return switch (opLevel) {
            case 4 -> PermissionLevel.OWNER;
            case 3 -> PermissionLevel.ADMIN;
            case 2 -> PermissionLevel.MODERATOR;
            case 1 -> PermissionLevel.HELPER;
            default -> PermissionLevel.USER;
        };
    }

    /** Räumt den OP-Fallback-Cache. */
    public void invalidateOpCache() {
        opFallbackCache.clear();
    }

    public Map<UUID, PermissionEntry> snapshotEntries() {
        return Collections.unmodifiableMap(new HashMap<>(entries));
    }
}
