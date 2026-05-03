package de.contentlos.cmdcore.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;

/**
 * Robustes JSON-Lesen und Schreiben für CMD-Core-Daten.
 *
 * <p>Eigenschaften:</p>
 * <ul>
 *   <li>Atomar speichern via Temp-Datei + Rename</li>
 *   <li>Backup kaputter JSON-Dateien (.broken-&lt;timestamp&gt;.json)</li>
 *   <li>Fehlende Dateien führen zu null/leerem Objekt, kein Crash</li>
 * </ul>
 */
public final class JsonStorage {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter BROKEN_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private JsonStorage() {
    }

    public static Gson gson() {
        return GSON;
    }

    /**
     * Liest und parst eine JSON-Datei. Gibt {@code null} zurück, wenn die
     * Datei nicht existiert. Bei kaputter JSON wird die Datei umbenannt
     * (Backup) und {@code null} zurückgegeben.
     */
    public static JsonElement readJson(Path file) {
        if (!Files.exists(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (JsonSyntaxException syntax) {
            LOGGER.error("CMD-Core: Defekte JSON-Datei {} – sichere als .broken Backup", file, syntax);
            backupBroken(file);
            return null;
        } catch (IOException io) {
            LOGGER.error("CMD-Core: Konnte JSON-Datei nicht lesen: {}", file, io);
            return null;
        }
    }

    /**
     * Liest und deserialisiert eine JSON-Datei in den angegebenen Typ.
     * Liefert {@code null}, wenn die Datei fehlt oder ungültig ist.
     */
    public static <T> T readObject(Path file, Class<T> type) {
        JsonElement root = readJson(file);
        if (root == null || !root.isJsonObject() && !root.isJsonArray()) {
            return null;
        }
        try {
            return GSON.fromJson(root, type);
        } catch (Exception e) {
            LOGGER.error("CMD-Core: JSON konnte nicht in {} deserialisiert werden: {}", type.getSimpleName(), file, e);
            backupBroken(file);
            return null;
        }
    }

    /**
     * Schreibt das Objekt atomar als JSON in die Datei.
     * Erstellt fehlende Verzeichnisse automatisch.
     */
    public static void writeObject(Path file, Object value) {
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException ignored) {
            // Wenn das schon scheitert, scheitert auch das Write – fangen wir gleich.
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        } catch (IOException io) {
            LOGGER.error("CMD-Core: Konnte temporäre JSON-Datei nicht schreiben: {}", tmp, io);
            return;
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFail) {
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException io) {
                LOGGER.error("CMD-Core: JSON-Speichern fehlgeschlagen für {}", file, io);
            }
        }
    }

    private static void backupBroken(Path file) {
        try {
            Path target = file.resolveSibling(file.getFileName().toString()
                    + ".broken-" + LocalDateTime.now().format(BROKEN_SUFFIX) + ".json");
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException io) {
            LOGGER.error("CMD-Core: Konnte defekte Datei nicht sichern: {}", file, io);
        }
    }
}
