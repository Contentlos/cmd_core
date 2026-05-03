package de.contentlos.cmdcore.common.modules;

import de.contentlos.cmdcore.common.permissions.PermissionLevel;

/**
 * Sammlung der eingebauten Module außer dem Kernmodul.
 */
public final class StandardModules {

    private StandardModules() {}

    public static Module permissions() {
        return new AbstractModule("permissions",
                "Berechtigungsmodul",
                "Verwaltet die CMD-Core-Berechtigungsstufen und JSON-Persistenz.",
                PermissionLevel.OWNER, true, true) {};
    }

    public static Module playerManagement() {
        return new AbstractModule("player_management",
                "Spielerverwaltung",
                "Heilen, Füttern, Teleport, Gamemode, Kick und weitere Aktionen.",
                PermissionLevel.MODERATOR, true, true) {};
    }

    public static Module adminPanel() {
        return new AbstractModule("admin_panel",
                "Admin-Panel",
                "Server-seitig validiertes Admin-GUI mit allen Tabs.",
                PermissionLevel.MODERATOR, true, true) {};
    }

    public static Module logging() {
        return new AbstractModule("logging",
                "Audit-Logger",
                "Schreibt Befehls- und Panel-Aktionen in den Verlauf.",
                PermissionLevel.ADMIN, true, true) {};
    }

    public static Module debug() {
        return new AbstractModule("debug",
                "Debug",
                "Zusätzliche Diagnose-Werkzeuge für Owner und Entwickler.",
                PermissionLevel.OWNER, true, false) {};
    }

    public static Module server() {
        return new AbstractModule("server",
                "Serververwaltung",
                "Welt speichern, Wetter, Tageszeit, Broadcast und mehr.",
                PermissionLevel.ADMIN, true, true) {};
    }
}
