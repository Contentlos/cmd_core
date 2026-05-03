package de.contentlos.cmdcore.common.modules;

import de.contentlos.cmdcore.CMDCore;
import de.contentlos.cmdcore.common.permissions.PermissionLevel;

/**
 * Kernmodul – darf nicht deaktiviert werden.
 * Es bündelt grundlegende Initialisierung und Konstanten.
 */
public final class CoreModule extends AbstractModule {

    public CoreModule() {
        super("core",
                "Kernmodul",
                "Grundgerüst von CMD Core. Kann nicht deaktiviert werden.",
                PermissionLevel.OWNER,
                false,
                true);
    }

    @Override
    public String version() {
        return CMDCore.VERSION;
    }
}
