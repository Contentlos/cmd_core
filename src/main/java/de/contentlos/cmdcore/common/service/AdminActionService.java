package de.contentlos.cmdcore.common.service;

import de.contentlos.cmdcore.common.config.NWConfig;
import de.contentlos.cmdcore.common.logging.AuditService;
import de.contentlos.cmdcore.common.permissions.PermissionLevel;
import de.contentlos.cmdcore.common.permissions.PermissionService;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Backend-Logik aller Admin-Aktionen. Wird sowohl vom Command-System
 * als auch vom Admin-Panel benutzt – einheitliche Quelle der Wahrheit.
 *
 * <p>Jede Aktion gibt ein {@link ActionResult} zurück, das Erfolg, eine
 * deutschsprachige Meldung und optional einen Hinweis enthält.</p>
 */
public final class AdminActionService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final MinecraftServer server;
    private final PermissionService permissions;
    private final AuditService audit;

    public AdminActionService(MinecraftServer server, PermissionService permissions, AuditService audit) {
        this.server = server;
        this.permissions = permissions;
        this.audit = audit;
    }

    public record ActionResult(boolean success, String message, String hint) {
        public static ActionResult ok(String msg) { return new ActionResult(true, msg, ""); }
        public static ActionResult ok(String msg, String hint) { return new ActionResult(true, msg, hint); }
        public static ActionResult fail(String msg) { return new ActionResult(false, msg, ""); }
    }

    public ActionResult heal(ServerPlayer target) {
        if (target == null) return ActionResult.fail("Zielspieler nicht gefunden.");
        target.setHealth(target.getMaxHealth());
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20f);
        target.setRemainingFireTicks(0);
        target.removeAllEffects();
        return ActionResult.ok(target.getGameProfile().getName() + " wurde geheilt.");
    }

    public ActionResult feed(ServerPlayer target) {
        if (target == null) return ActionResult.fail("Zielspieler nicht gefunden.");
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20f);
        return ActionResult.ok(target.getGameProfile().getName() + " wurde gefüttert.");
    }

    public ActionResult toggleFlight(ServerPlayer target) {
        if (target == null) return ActionResult.fail("Zielspieler nicht gefunden.");
        boolean newAllow = !target.getAbilities().mayfly;
        target.getAbilities().mayfly = newAllow;
        if (!newAllow) {
            target.getAbilities().flying = false;
        }
        target.onUpdateAbilities();
        return ActionResult.ok(target.getGameProfile().getName()
                + " kann " + (newAllow ? "jetzt" : "nicht mehr") + " fliegen.");
    }

    public ActionResult setGamemode(ServerPlayer target, String gameModeName) {
        if (!NWConfig.ADMIN.allowGamemodeChange.get()) {
            return ActionResult.fail("Gamemode-Änderungen sind in der Config deaktiviert.");
        }
        if (target == null) return ActionResult.fail("Zielspieler nicht gefunden.");
        if (gameModeName == null) return ActionResult.fail("Kein Gamemode angegeben.");
        GameType type = parseGameMode(gameModeName);
        if (type == null) {
            return ActionResult.fail("Unbekannter Gamemode: " + gameModeName);
        }
        target.setGameMode(type);
        return ActionResult.ok(target.getGameProfile().getName() + " ist jetzt im "
                + type.getName().toUpperCase(Locale.ROOT) + "-Modus.");
    }

    public ActionResult teleportToTarget(ServerPlayer admin, ServerPlayer target) {
        if (!NWConfig.ADMIN.allowTeleport.get()) {
            return ActionResult.fail("Teleport ist deaktiviert.");
        }
        if (admin == null || target == null) {
            return ActionResult.fail("Spieler nicht gefunden.");
        }
        teleportTo(admin, target);
        return ActionResult.ok("Teleportiert zu " + target.getGameProfile().getName() + ".");
    }

    public ActionResult teleportHere(ServerPlayer admin, ServerPlayer target) {
        if (!NWConfig.ADMIN.allowTeleport.get()) {
            return ActionResult.fail("Teleport ist deaktiviert.");
        }
        if (admin == null || target == null) {
            return ActionResult.fail("Spieler nicht gefunden.");
        }
        teleportTo(target, admin);
        return ActionResult.ok(target.getGameProfile().getName() + " wurde zu dir teleportiert.");
    }

    private void teleportTo(ServerPlayer who, Entity to) {
        ServerLevel level = (ServerLevel) to.level();
        Vec3 pos = to.position();
        who.teleportTo(level, pos.x, pos.y, pos.z, who.getYRot(), who.getXRot());
    }

    public ActionResult kick(ServerPlayer target, String reason) {
        if (!NWConfig.ADMIN.allowKick.get()) {
            return ActionResult.fail("Kicks sind deaktiviert.");
        }
        if (target == null) return ActionResult.fail("Zielspieler nicht gefunden.");
        String r = (reason == null || reason.isBlank()) ? "Du wurdest vom Server entfernt." : reason;
        target.connection.disconnect(Component.literal(r));
        return ActionResult.ok(target.getGameProfile().getName() + " wurde gekickt.");
    }

    public ActionResult broadcast(String message) {
        if (message == null || message.isBlank()) {
            return ActionResult.fail("Nachricht darf nicht leer sein.");
        }
        Component msg = Component.literal("[Server] " + message);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(msg);
        }
        return ActionResult.ok("Broadcast gesendet.");
    }

    public ActionResult setDay() {
        return setTime(1000L);
    }

    public ActionResult setNight() {
        return setTime(13000L);
    }

    private ActionResult setTime(long time) {
        if (!NWConfig.ADMIN.allowTimeControl.get()) {
            return ActionResult.fail("Zeit-Kontrolle ist deaktiviert.");
        }
        for (ServerLevel level : server.getAllLevels()) {
            level.setDayTime(time);
        }
        return ActionResult.ok("Zeit gesetzt.");
    }

    public ActionResult setWeather(String type) {
        if (!NWConfig.ADMIN.allowWeatherControl.get()) {
            return ActionResult.fail("Wetter-Kontrolle ist deaktiviert.");
        }
        if (type == null) return ActionResult.fail("Kein Wettertyp angegeben.");
        String t = type.toLowerCase(Locale.ROOT);
        for (ServerLevel level : server.getAllLevels()) {
            switch (t) {
                case "clear", "klar" -> level.setWeatherParameters(6000, 0, false, false);
                case "rain", "regen" -> level.setWeatherParameters(0, 6000, true, false);
                case "thunder", "gewitter" -> level.setWeatherParameters(0, 6000, true, true);
                default -> {
                    return ActionResult.fail("Unbekanntes Wetter: " + type);
                }
            }
        }
        return ActionResult.ok("Wetter gesetzt: " + t);
    }

    public ActionResult saveWorld() {
        boolean ok = server.saveAllChunks(false, true, true);
        return ok ? ActionResult.ok("Welt gespeichert.")
                : ActionResult.fail("Welt konnte nicht vollständig gespeichert werden.");
    }

    public ActionResult savePlayerData() {
        try {
            server.getPlayerList().saveAll();
            return ActionResult.ok("Spielerdaten gespeichert.");
        } catch (Exception e) {
            LOGGER.error("CMD-Core: Spielerdaten speichern fehlgeschlagen", e);
            return ActionResult.fail("Spielerdaten speichern fehlgeschlagen: " + e.getMessage());
        }
    }

    public ActionResult titleToTarget(ServerPlayer target, String title) {
        if (target == null) return ActionResult.fail("Zielspieler nicht gefunden.");
        target.sendSystemMessage(Component.literal(title == null ? "" : title));
        return ActionResult.ok("Nachricht gesendet.");
    }

    public ActionResult playerInfo(ServerPlayer target) {
        if (target == null) return ActionResult.fail("Zielspieler nicht gefunden.");
        StringBuilder sb = new StringBuilder();
        sb.append("Spieler: ").append(target.getGameProfile().getName()).append('\n');
        sb.append("UUID: ").append(target.getUUID()).append('\n');
        sb.append("Gamemode: ").append(target.gameMode.getGameModeForPlayer().getName()).append('\n');
        sb.append("Welt: ").append(target.level().dimension().location()).append('\n');
        sb.append("Position: ").append(formatPos(target.position())).append('\n');
        sb.append("Leben: ").append(target.getHealth()).append('/').append(target.getMaxHealth()).append('\n');
        sb.append("Hunger: ").append(target.getFoodData().getFoodLevel()).append('/').append(20).append('\n');
        sb.append("Permission: ").append(permissions.levelOf(target.getUUID()).name());
        return ActionResult.ok("Spielerinfo abgefragt", sb.toString());
    }

    public String formatPos(Vec3 v) {
        return String.format(Locale.ROOT, "%.1f / %.1f / %.1f", v.x, v.y, v.z);
    }

    public boolean checkPermission(ServerPlayer admin, PermissionLevel required) {
        if (admin == null) return false;
        return permissions.levelOf(admin.getUUID()).atLeast(required);
    }

    /** GameRule für Day/Night-Cycle (nur informativ). */
    public boolean dayLightCycle() {
        try {
            return server.overworld().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        } catch (Throwable t) {
            return true;
        }
    }

    @Nullable
    public ServerPlayer findPlayer(String name) {
        return server.getPlayerList().getPlayerByName(name);
    }

    @Nullable
    public ServerPlayer findPlayer(UUID uuid) {
        return server.getPlayerList().getPlayer(uuid);
    }

    @Nullable
    public static GameType parseGameMode(String name) {
        if (name == null) return null;
        switch (name.toLowerCase(Locale.ROOT)) {
            case "survival", "0", "s" -> { return GameType.SURVIVAL; }
            case "creative", "1", "c" -> { return GameType.CREATIVE; }
            case "adventure", "2", "a" -> { return GameType.ADVENTURE; }
            case "spectator", "3", "spec", "sp" -> { return GameType.SPECTATOR; }
            default -> { return null; }
        }
    }
}
