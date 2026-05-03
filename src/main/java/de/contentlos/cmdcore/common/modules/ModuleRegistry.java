package de.contentlos.cmdcore.common.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import de.contentlos.cmdcore.common.data.CMDDataPaths;
import de.contentlos.cmdcore.common.data.JsonStorage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * Verwaltung aller registrierten Module.
 *
 * <p>Persistiert den Aktivierungsstatus in <code>module_states.json</code>.</p>
 */
public final class ModuleRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final MinecraftServer server;
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleRegistry(MinecraftServer server) {
        this.server = server;
    }

    /** Registriert ein Modul. Beim ersten Lauf wird der gespeicherte Status angewendet. */
    public synchronized void register(Module module) {
        if (module == null || module.id() == null) {
            return;
        }
        if (modules.containsKey(module.id())) {
            LOGGER.warn("CMD-Core: Modul {} ist bereits registriert, ignoriere", module.id());
            return;
        }
        modules.put(module.id(), module);
    }

    public Optional<Module> get(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    public Collection<Module> all() {
        return new ArrayList<>(modules.values());
    }

    public List<Module> sorted() {
        List<Module> list = new ArrayList<>(modules.values());
        list.sort((a, b) -> {
            // CoreModule immer ganz oben
            if ("core".equals(a.id())) return -1;
            if ("core".equals(b.id())) return 1;
            return a.displayName().compareToIgnoreCase(b.displayName());
        });
        return list;
    }

    /** Lädt gespeicherten Aktivierungsstatus aus JSON. */
    public synchronized void load() {
        Path file = CMDDataPaths.file(server, CMDDataPaths.FILE_MODULE_STATES);
        JsonElement root = JsonStorage.readJson(file);
        if (root == null || !root.isJsonObject()) {
            return;
        }
        JsonObject obj = root.getAsJsonObject();
        for (Module module : modules.values()) {
            if (!obj.has(module.id())) continue;
            try {
                boolean want = obj.get(module.id()).getAsBoolean();
                if (want != module.isEnabled()) {
                    if (want) {
                        module.enable();
                    } else if (module.canBeDisabled()) {
                        module.disable();
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("CMD-Core: Modulstatus konnte nicht geladen werden: {}", module.id(), e);
            }
        }
    }

    /** Persistiert den aktuellen Status. */
    public synchronized void save() {
        Path file = CMDDataPaths.file(server, CMDDataPaths.FILE_MODULE_STATES);
        JsonObject root = new JsonObject();
        for (Module module : modules.values()) {
            root.addProperty(module.id(), module.isEnabled());
        }
        JsonStorage.writeObject(file, root);
    }

    /** Aktiviert oder deaktiviert ein Modul. Liefert true bei Erfolg. */
    public synchronized boolean setEnabled(String id, boolean enabled) {
        Module module = modules.get(id);
        if (module == null) return false;
        if (!enabled && !module.canBeDisabled()) return false;
        if (enabled) {
            module.enable();
        } else {
            module.disable();
        }
        save();
        return true;
    }

    public synchronized void reload(String id) {
        Module module = modules.get(id);
        if (module != null) module.reload();
    }

    public synchronized void reloadAll() {
        for (Module module : modules.values()) {
            module.reload();
        }
    }
}
