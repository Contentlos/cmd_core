package de.contentlos.cmdcore.common.permissions;

import java.util.UUID;

/**
 * Ein gespeicherter Permission-Eintrag pro Spieler.
 *
 * <p>Wird in <code>permissions.json</code> persistiert.</p>
 */
public final class PermissionEntry {

    public enum Source {
        /** Aus der JSON-Datei geladen. */
        JSON,
        /** Über Vanilla-OP fallback bestimmt. */
        OP_FALLBACK,
        /** Direkt von der Konsole gesetzt. */
        CONSOLE,
        /** Standardwert ohne expliziten Eintrag. */
        DEFAULT
    }

    private UUID uuid;
    private String name;
    private PermissionLevel level;
    private long updatedAt;
    private transient Source source = Source.JSON;

    public PermissionEntry() {
        // Für Gson.
    }

    public PermissionEntry(UUID uuid, String name, PermissionLevel level) {
        this.uuid = uuid;
        this.name = name;
        this.level = level;
        this.updatedAt = System.currentTimeMillis();
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public PermissionLevel level() { return level == null ? PermissionLevel.USER : level; }
    public long updatedAt() { return updatedAt; }
    public Source source() { return source == null ? Source.JSON : source; }

    public void setName(String name) { this.name = name; }
    public void setLevel(PermissionLevel level) {
        this.level = level;
        this.updatedAt = System.currentTimeMillis();
    }
    public void setSource(Source source) { this.source = source; }
}
