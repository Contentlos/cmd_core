package de.contentlos.cmdcore.common.logging;

import java.util.UUID;

/**
 * Ein Audit-Log-Eintrag für Admin-Aktionen.
 * Wird sowohl im RAM gehalten als auch in der admin_history.json persistiert.
 */
public final class AuditEntry {

    public enum Source {
        COMMAND,
        PANEL,
        SYSTEM
    }

    private long timestamp;
    private String adminName;
    private UUID adminUuid;
    private String adminLevel;
    private String targetName;
    private UUID targetUuid;
    private String action;
    private Source source;
    private boolean success;
    private String details;
    private String module;

    public AuditEntry() {
        // Für Gson.
    }

    public AuditEntry(long timestamp, String adminName, UUID adminUuid, String adminLevel,
                      String targetName, UUID targetUuid, String action,
                      Source source, boolean success, String details, String module) {
        this.timestamp = timestamp;
        this.adminName = adminName;
        this.adminUuid = adminUuid;
        this.adminLevel = adminLevel;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
        this.action = action;
        this.source = source;
        this.success = success;
        this.details = details;
        this.module = module;
    }

    public long timestamp() { return timestamp; }
    public String adminName() { return adminName; }
    public UUID adminUuid() { return adminUuid; }
    public String adminLevel() { return adminLevel; }
    public String targetName() { return targetName; }
    public UUID targetUuid() { return targetUuid; }
    public String action() { return action; }
    public Source source() { return source == null ? Source.SYSTEM : source; }
    public boolean success() { return success; }
    public String details() { return details; }
    public String module() { return module; }
}
