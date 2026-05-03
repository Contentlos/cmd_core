package de.contentlos.cmdcore.common.permissions;

import java.util.Locale;

/**
 * Berechtigungsstufen des CMD-Core-Permission-Systems.
 *
 * <p>Stufen sind numerisch geordnet. Höhere Werte enthalten alle Rechte
 * niedrigerer Werte.</p>
 *
 * <ul>
 *   <li>{@link #USER} – Standardspieler, keine Adminrechte.</li>
 *   <li>{@link #HELPER} – Kleine Moderation (Info, Warn).</li>
 *   <li>{@link #MODERATOR} – Mute/Kick und einfache Admin-Aktionen.</li>
 *   <li>{@link #ADMIN} – Volle Admin-Aktionen, Permission-Änderungen.</li>
 *   <li>{@link #OWNER} – Höchste Spieler-Stufe, kann Owner-Settings ändern.</li>
 *   <li>{@link #CONSOLE} – Reserviert für die Server-Konsole.</li>
 * </ul>
 */
public enum PermissionLevel {

    USER(0, "Spieler"),
    HELPER(1, "Helfer"),
    MODERATOR(2, "Moderator"),
    ADMIN(3, "Admin"),
    OWNER(4, "Owner"),
    CONSOLE(5, "Konsole");

    private final int weight;
    private final String displayName;

    PermissionLevel(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }

    /** Numerisches Gewicht der Stufe (höher = mehr Rechte). */
    public int weight() {
        return weight;
    }

    /** Deutscher Anzeigename. */
    public String displayName() {
        return displayName;
    }

    /**
     * Prüft, ob diese Stufe die geforderte Mindeststufe erfüllt.
     */
    public boolean atLeast(PermissionLevel required) {
        return required == null || this.weight >= required.weight;
    }

    /**
     * Liest eine Stufe aus einem String.
     * Akzeptiert sowohl "ADMIN" als auch "admin".
     * Liefert {@link #USER} bei null/unbekannt.
     */
    public static PermissionLevel parse(String raw) {
        if (raw == null) {
            return USER;
        }
        try {
            return PermissionLevel.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
