package de.contentlos.cmdcore;

/**
 * Konstanten und Identifikatoren des CMD-Core-Moduls.
 *
 * <p>CMD Core ist der interne Modulname des Mods. Der externe Mod-Identifier
 * ist {@code nightwatch_core}.</p>
 */
public final class CMDCore {

    /** Externe Mod-ID (entspricht der mods.toml-ID). */
    public static final String MOD_ID = "nightwatch_core";

    /** Anzeigename, intern weiterhin "CMD Core" als Modulname. */
    public static final String DISPLAY_NAME = "NightWatch Core";

    /** Interner Modulname. */
    public static final String INTERNAL_MODULE_NAME = "CMD Core";

    /** Version – wird beim Build aus mod_version eingebrannt, hier als Fallback. */
    public static final String VERSION = "0.2.0";

    private CMDCore() {
        // Utility-Klasse, keine Instanzen.
    }
}
