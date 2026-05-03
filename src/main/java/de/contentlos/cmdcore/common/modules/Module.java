package de.contentlos.cmdcore.common.modules;

import de.contentlos.cmdcore.common.permissions.PermissionLevel;

/**
 * Schnittstelle für CMD-Core-Module.
 *
 * <p>Ein Modul kapselt eine logische Funktionsgruppe (z.B. Permissions,
 * Logging, Admin-Panel) und kann zur Laufzeit aktiviert/deaktiviert oder
 * neu geladen werden.</p>
 */
public interface Module {

    /** Eindeutige ID, klein geschrieben, ohne Leerzeichen. */
    String id();

    /** Anzeigename für das Panel. */
    String displayName();

    /** Kurze Beschreibung in Deutsch. */
    String description();

    /** Mindeststufe, um Modul-Status zu sehen oder zu ändern. */
    default PermissionLevel requiredLevel() {
        return PermissionLevel.ADMIN;
    }

    /** Ist dieses Modul abschaltbar? CoreModule typischerweise nicht. */
    default boolean canBeDisabled() {
        return true;
    }

    /** Aktivierungsstatus. */
    boolean isEnabled();

    /** Aktiviert das Modul. */
    void enable();

    /** Deaktiviert das Modul. */
    void disable();

    /** Lädt das Modul neu (z.B. Configs). */
    default void reload() {
        // Standard: Disable + Enable
        if (isEnabled()) {
            disable();
            enable();
        }
    }

    /** Optionaler Fehlerstatus, leer wenn alles OK. */
    default String errorStatus() {
        return "";
    }

    /** Versionsstring, leer für interne Kernmodule. */
    default String version() {
        return "";
    }
}
