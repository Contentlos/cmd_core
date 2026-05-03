package de.contentlos.cmdcore.common.modules;

import de.contentlos.cmdcore.common.permissions.PermissionLevel;

/**
 * Basisklasse für Module mit Standardimplementierung.
 */
public abstract class AbstractModule implements Module {

    private final String id;
    private final String displayName;
    private final String description;
    private final PermissionLevel requiredLevel;
    private final boolean canBeDisabled;
    private boolean enabled;

    protected AbstractModule(String id, String displayName, String description,
                             PermissionLevel requiredLevel, boolean canBeDisabled,
                             boolean defaultEnabled) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.requiredLevel = requiredLevel;
        this.canBeDisabled = canBeDisabled;
        this.enabled = defaultEnabled;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public String description() { return description; }
    @Override public PermissionLevel requiredLevel() { return requiredLevel; }
    @Override public boolean canBeDisabled() { return canBeDisabled; }

    @Override
    public synchronized boolean isEnabled() { return enabled; }

    @Override
    public synchronized void enable() {
        if (!enabled) {
            enabled = true;
            onEnable();
        }
    }

    @Override
    public synchronized void disable() {
        if (!canBeDisabled) {
            return;
        }
        if (enabled) {
            enabled = false;
            onDisable();
        }
    }

    /** Hook für konkrete Module beim Aktivieren. */
    protected void onEnable() {}

    /** Hook für konkrete Module beim Deaktivieren. */
    protected void onDisable() {}
}
