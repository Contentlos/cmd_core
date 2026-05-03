package de.contentlos.cmdcore.common.service;

import de.contentlos.cmdcore.common.config.NWConfig;
import de.contentlos.cmdcore.common.network.payload.ClientboundConfigPayload;
import de.contentlos.cmdcore.common.permissions.PermissionLevel;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Sammelt die aktuellen Config-Werte zur Anzeige im Panel.
 *
 * <p>Schreibzugriff erfolgt über {@link #applyChange(String, String)}, das
 * eine konservative Liste erlaubter Schlüssel definiert. Felder, die einen
 * Serverneustart erfordern, sind als solche markiert.</p>
 */
public final class ConfigSnapshotService {

    public List<ClientboundConfigPayload.Entry> snapshot() {
        List<ClientboundConfigPayload.Entry> out = new ArrayList<>();
        out.add(entry("general.enableCore", NWConfig.GENERAL.enableCore.get(), "BOOL",
                "Schaltet das Kern-System ein/aus.", true));
        out.add(entry("general.enableDebugMode", NWConfig.GENERAL.enableDebugMode.get(), "BOOL",
                "Debug-Tab und Debug-Logging.", false));
        out.add(entry("general.dataSaveIntervalSeconds", NWConfig.GENERAL.dataSaveIntervalSeconds.get(), "INT",
                "Speicher-Intervall in Sekunden.", false));

        out.add(entry("admin.enableAdminSystem", NWConfig.ADMIN.enableAdminSystem.get(), "BOOL",
                "Admin-System aktivieren.", false));
        out.add(entry("admin.minPanelPermissionLevel", NWConfig.ADMIN.minPanelPermissionLevel.get(), "ENUM",
                "Mindeststufe zum Öffnen des Panels.", false));
        out.add(entry("admin.allowKick", NWConfig.ADMIN.allowKick.get(), "BOOL",
                "Kick-Aktionen erlauben.", false));
        out.add(entry("admin.allowTeleport", NWConfig.ADMIN.allowTeleport.get(), "BOOL",
                "Teleport-Aktionen erlauben.", false));
        out.add(entry("admin.allowGamemodeChange", NWConfig.ADMIN.allowGamemodeChange.get(), "BOOL",
                "Gamemode-Änderungen erlauben.", false));
        out.add(entry("admin.allowWeatherControl", NWConfig.ADMIN.allowWeatherControl.get(), "BOOL",
                "Wetter-Steuerung erlauben.", false));
        out.add(entry("admin.allowTimeControl", NWConfig.ADMIN.allowTimeControl.get(), "BOOL",
                "Zeit-Steuerung erlauben.", false));

        out.add(entry("permissions.enablePermissionSystem", NWConfig.PERMISSIONS.enablePermissionSystem.get(), "BOOL",
                "Permission-System aktiv.", false));
        out.add(entry("permissions.defaultPermissionLevel", NWConfig.PERMISSIONS.defaultPermissionLevel.get(), "ENUM",
                "Standard-Stufe ohne Eintrag.", false));
        out.add(entry("permissions.allowOpFallback", NWConfig.PERMISSIONS.allowOpFallback.get(), "BOOL",
                "Vanilla-OPs auf CMD-Stufen mappen.", false));

        out.add(entry("logging.enableAuditLog", NWConfig.LOGGING.enableAuditLog.get(), "BOOL",
                "Audit-Log aktiv.", false));
        out.add(entry("logging.logAdminCommands", NWConfig.LOGGING.logAdminCommands.get(), "BOOL",
                "Admin-Befehle in den Verlauf schreiben.", false));
        out.add(entry("logging.logPanelActions", NWConfig.LOGGING.logPanelActions.get(), "BOOL",
                "Panel-Aktionen in den Verlauf schreiben.", false));
        out.add(entry("logging.logPlayerJoin", NWConfig.LOGGING.logPlayerJoin.get(), "BOOL",
                "Beitritt/Verlassen mitloggen.", false));
        out.add(entry("logging.maxHistoryEntries", NWConfig.LOGGING.maxHistoryEntries.get(), "INT",
                "Maximale Verlauf-Einträge.", false));

        out.add(entry("panel.enableAdminPanel", NWConfig.PANEL.enableAdminPanel.get(), "BOOL",
                "Admin-Panel aktiv.", false));
        out.add(entry("panel.panelAccentColor", NWConfig.PANEL.panelAccentColor.get(), "STRING",
                "Akzentfarbe (BLUE/CYAN/VIOLET/RED/GREEN/AMBER).", false));
        out.add(entry("panel.panelAnimations", NWConfig.PANEL.panelAnimations.get(), "BOOL",
                "Animationen im Panel.", false));
        out.add(entry("panel.panelDefaultTab", NWConfig.PANEL.panelDefaultTab.get(), "STRING",
                "Standard-Tab beim Öffnen.", false));
        out.add(entry("panel.panelKeybindEnabled", NWConfig.PANEL.panelKeybindEnabled.get(), "BOOL",
                "Hotkey für das Panel auf dem Client.", false));
        return out;
    }

    private static ClientboundConfigPayload.Entry entry(String key, Object value, String type,
                                                        String desc, boolean restart) {
        return new ClientboundConfigPayload.Entry(key, String.valueOf(value), type, desc, restart);
    }

    /** Anwenden einer Änderung. Liefert eine kurze Beschreibung des Ergebnisses. */
    public String applyChange(String key, String rawValue) {
        if (key == null) return "Kein Schlüssel";
        try {
            switch (key) {
                case "general.enableDebugMode" -> set(NWConfig.GENERAL.enableDebugMode, parseBool(rawValue));
                case "general.dataSaveIntervalSeconds" -> set(NWConfig.GENERAL.dataSaveIntervalSeconds, Integer.parseInt(rawValue));
                case "admin.enableAdminSystem" -> set(NWConfig.ADMIN.enableAdminSystem, parseBool(rawValue));
                case "admin.minPanelPermissionLevel" -> set(NWConfig.ADMIN.minPanelPermissionLevel, PermissionLevel.parse(rawValue));
                case "admin.allowKick" -> set(NWConfig.ADMIN.allowKick, parseBool(rawValue));
                case "admin.allowTeleport" -> set(NWConfig.ADMIN.allowTeleport, parseBool(rawValue));
                case "admin.allowGamemodeChange" -> set(NWConfig.ADMIN.allowGamemodeChange, parseBool(rawValue));
                case "admin.allowWeatherControl" -> set(NWConfig.ADMIN.allowWeatherControl, parseBool(rawValue));
                case "admin.allowTimeControl" -> set(NWConfig.ADMIN.allowTimeControl, parseBool(rawValue));
                case "permissions.enablePermissionSystem" -> set(NWConfig.PERMISSIONS.enablePermissionSystem, parseBool(rawValue));
                case "permissions.defaultPermissionLevel" -> set(NWConfig.PERMISSIONS.defaultPermissionLevel, PermissionLevel.parse(rawValue));
                case "permissions.allowOpFallback" -> set(NWConfig.PERMISSIONS.allowOpFallback, parseBool(rawValue));
                case "logging.enableAuditLog" -> set(NWConfig.LOGGING.enableAuditLog, parseBool(rawValue));
                case "logging.logAdminCommands" -> set(NWConfig.LOGGING.logAdminCommands, parseBool(rawValue));
                case "logging.logPanelActions" -> set(NWConfig.LOGGING.logPanelActions, parseBool(rawValue));
                case "logging.logPlayerJoin" -> set(NWConfig.LOGGING.logPlayerJoin, parseBool(rawValue));
                case "logging.maxHistoryEntries" -> set(NWConfig.LOGGING.maxHistoryEntries, Integer.parseInt(rawValue));
                case "panel.enableAdminPanel" -> set(NWConfig.PANEL.enableAdminPanel, parseBool(rawValue));
                case "panel.panelAccentColor" -> set(NWConfig.PANEL.panelAccentColor, rawValue);
                case "panel.panelAnimations" -> set(NWConfig.PANEL.panelAnimations, parseBool(rawValue));
                case "panel.panelDefaultTab" -> set(NWConfig.PANEL.panelDefaultTab, rawValue);
                case "panel.panelKeybindEnabled" -> set(NWConfig.PANEL.panelKeybindEnabled, parseBool(rawValue));
                default -> {
                    return "Schlüssel ist nicht zur Laufzeit änderbar: " + key;
                }
            }
            NWConfig.SPEC.save();
            return "OK: " + key + " = " + rawValue;
        } catch (NumberFormatException nfe) {
            return "Ungültige Zahl: " + rawValue;
        } catch (Exception e) {
            return "Fehler: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void set(ModConfigSpec.ConfigValue<T> value, T v) {
        value.set(v);
    }

    private static boolean parseBool(String s) {
        if (s == null) return false;
        String n = s.trim().toLowerCase();
        return n.equals("true") || n.equals("1") || n.equals("ja") || n.equals("on");
    }
}
