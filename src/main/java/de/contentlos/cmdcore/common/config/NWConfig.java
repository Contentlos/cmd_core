package de.contentlos.cmdcore.common.config;

import de.contentlos.cmdcore.common.permissions.PermissionLevel;
import java.util.Arrays;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Zentrale Konfigurationsdefinitionen für CMD Core / NightWatch Core.
 *
 * <p>Die Konfiguration ist in fünf logische Bereiche aufgeteilt, die
 * vereint in einer einzigen Server-Config-Datei abgelegt werden:
 * <code>nightwatch_core-server.toml</code>.</p>
 *
 * <p>Wird zur Laufzeit beim ModConfigEvent geladen.</p>
 */
public final class NWConfig {

    public static final ModConfigSpec SPEC;
    public static final General GENERAL;
    public static final Admin ADMIN;
    public static final Permissions PERMISSIONS;
    public static final Logging LOGGING;
    public static final Panel PANEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        GENERAL = new General(builder);
        ADMIN = new Admin(builder);
        PERMISSIONS = new Permissions(builder);
        LOGGING = new Logging(builder);
        PANEL = new Panel(builder);
        SPEC = builder.build();
    }

    private NWConfig() {}

    /** general.toml – allgemeine Schalter. */
    public static final class General {
        public final ModConfigSpec.BooleanValue enableCore;
        public final ModConfigSpec.BooleanValue enableDebugMode;
        public final ModConfigSpec.IntValue dataSaveIntervalSeconds;

        General(ModConfigSpec.Builder b) {
            b.comment("Allgemeine Einstellungen für NightWatch / CMD Core.").push("general");
            enableCore = b
                    .comment("Aktiviert oder deaktiviert das Kern-System komplett.",
                            "Sicherheits-Fallback: Kern-Module werden auch dann initialisiert,",
                            "können aber Aktionen verweigern.")
                    .define("enableCore", true);
            enableDebugMode = b
                    .comment("Schaltet Debug-Logging und den Debug-Tab im Panel frei.")
                    .define("enableDebugMode", false);
            dataSaveIntervalSeconds = b
                    .comment("Intervall in Sekunden für automatisches Speichern.",
                            "Minimum 30, Maximum 3600.")
                    .defineInRange("dataSaveIntervalSeconds", 300, 30, 3600);
            b.pop();
        }
    }

    /** admin.toml – Admin-System. */
    public static final class Admin {
        public final ModConfigSpec.BooleanValue enableAdminSystem;
        public final ModConfigSpec.EnumValue<PermissionLevel> minPanelPermissionLevel;
        public final ModConfigSpec.BooleanValue allowKick;
        public final ModConfigSpec.BooleanValue allowTeleport;
        public final ModConfigSpec.BooleanValue allowGamemodeChange;
        public final ModConfigSpec.BooleanValue allowWeatherControl;
        public final ModConfigSpec.BooleanValue allowTimeControl;

        Admin(ModConfigSpec.Builder b) {
            b.comment("Admin-System-Optionen.").push("admin");
            enableAdminSystem = b.define("enableAdminSystem", true);
            minPanelPermissionLevel = b
                    .comment("Mindeststufe, um das Admin-Panel zu öffnen.")
                    .defineEnum("minPanelPermissionLevel", PermissionLevel.MODERATOR);
            allowKick = b.define("allowKick", true);
            allowTeleport = b.define("allowTeleport", true);
            allowGamemodeChange = b.define("allowGamemodeChange", true);
            allowWeatherControl = b.define("allowWeatherControl", true);
            allowTimeControl = b.define("allowTimeControl", true);
            b.pop();
        }
    }

    /** permissions.toml – Permission-System. */
    public static final class Permissions {
        public final ModConfigSpec.BooleanValue enablePermissionSystem;
        public final ModConfigSpec.EnumValue<PermissionLevel> defaultPermissionLevel;
        public final ModConfigSpec.BooleanValue allowOpFallback;
        public final ModConfigSpec.ConfigValue<List<? extends String>> ownerUUIDs;

        Permissions(ModConfigSpec.Builder b) {
            b.comment("Permission-System.").push("permissions");
            enablePermissionSystem = b.define("enablePermissionSystem", true);
            defaultPermissionLevel = b
                    .comment("Stufe für Spieler ohne expliziten Eintrag.")
                    .defineEnum("defaultPermissionLevel", PermissionLevel.USER);
            allowOpFallback = b
                    .comment("Wenn aktiv, werden Vanilla-OPs auf CMD-Stufen abgebildet.")
                    .define("allowOpFallback", true);
            ownerUUIDs = b
                    .comment("UUIDs, die immer OWNER sein sollen, unabhängig von der JSON.")
                    .defineList("ownerUUIDs", java.util.Collections.emptyList(),
                            o -> o instanceof String);
            b.pop();
        }
    }

    /** logging.toml – Audit-Log. */
    public static final class Logging {
        public final ModConfigSpec.BooleanValue enableAuditLog;
        public final ModConfigSpec.BooleanValue logAdminCommands;
        public final ModConfigSpec.BooleanValue logPanelActions;
        public final ModConfigSpec.BooleanValue logPlayerJoin;
        public final ModConfigSpec.IntValue maxHistoryEntries;

        Logging(ModConfigSpec.Builder b) {
            b.comment("Audit-Log und Verlauf.").push("logging");
            enableAuditLog = b.define("enableAuditLog", true);
            logAdminCommands = b.define("logAdminCommands", true);
            logPanelActions = b.define("logPanelActions", true);
            logPlayerJoin = b.define("logPlayerJoin", false);
            maxHistoryEntries = b
                    .comment("Maximale Anzahl Verlaufseinträge im Speicher und in der JSON.")
                    .defineInRange("maxHistoryEntries", 500, 50, 10000);
            b.pop();
        }
    }

    /** panel.toml – Admin-Panel. */
    public static final class Panel {
        public final ModConfigSpec.BooleanValue enableAdminPanel;
        public final ModConfigSpec.ConfigValue<String> panelAccentColor;
        public final ModConfigSpec.BooleanValue panelAnimations;
        public final ModConfigSpec.ConfigValue<String> panelDefaultTab;
        public final ModConfigSpec.BooleanValue panelKeybindEnabled;

        Panel(ModConfigSpec.Builder b) {
            b.comment("Admin-Panel-Verhalten und Look.").push("panel");
            enableAdminPanel = b.define("enableAdminPanel", true);
            panelAccentColor = b
                    .comment("Akzentfarbe (BLUE, CYAN, VIOLET, RED, GREEN, AMBER).",
                            "Wird vom Client beim nächsten Panel-Open ausgewertet.")
                    .define("panelAccentColor", "CYAN", o -> o instanceof String s
                            && Arrays.asList("BLUE", "CYAN", "VIOLET", "RED", "GREEN", "AMBER").contains(s));
            panelAnimations = b.define("panelAnimations", true);
            panelDefaultTab = b
                    .comment("Standard-Tab beim Öffnen.")
                    .define("panelDefaultTab", "OVERVIEW",
                            o -> o instanceof String s && Arrays.asList(
                                    "OVERVIEW", "PLAYERS", "PERMISSIONS", "ACTIONS", "SERVER",
                                    "MODULES", "LOGS", "HISTORY", "SETTINGS", "DEBUG").contains(s));
            panelKeybindEnabled = b
                    .comment("Erlaubt das Öffnen per Hotkey im Client.")
                    .define("panelKeybindEnabled", true);
            b.pop();
        }
    }
}
