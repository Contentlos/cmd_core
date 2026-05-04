package de.contentlos.cmdcore.common.network.handler;

import com.mojang.logging.LogUtils;
import de.contentlos.cmdcore.common.config.NWConfig;
import de.contentlos.cmdcore.common.logging.AuditEntry;
import de.contentlos.cmdcore.common.logging.AuditService;
import de.contentlos.cmdcore.common.modules.Module;
import de.contentlos.cmdcore.common.modules.ModuleRegistry;
import de.contentlos.cmdcore.common.network.payload.ClientboundActionResultPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundConfigPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundDebugPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundLogListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundModuleListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundOpenPanelPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundOverviewPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPermissionListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPlayerListPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundActionPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundToggleModulePayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundUpdateConfigPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundUpdatePermissionPayload;
import de.contentlos.cmdcore.common.permissions.PermissionEntry;
import de.contentlos.cmdcore.common.permissions.PermissionLevel;
import de.contentlos.cmdcore.common.permissions.PermissionService;
import de.contentlos.cmdcore.common.service.AdminActionService;
import de.contentlos.cmdcore.common.service.CMDCoreServices;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

/**
 * Serverseitige Handler für alle Serverbound-Payloads.
 *
 * <p>Jeder Handler:</p>
 * <ol>
 *   <li>Stellt sicher, dass der Sender ein Spieler ist.</li>
 *   <li>Prüft die Berechtigung serverseitig.</li>
 *   <li>Führt die Aktion aus.</li>
 *   <li>Schreibt einen Audit-Eintrag.</li>
 *   <li>Sendet ggf. ein Antwort-Payload.</li>
 * </ol>
 */
public final class ServerboundHandlers {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ServerboundHandlers() {}

    private static @Nullable ServerPlayer playerOf(IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) return sp;
        return null;
    }

    /**
     * Hilfsfunktion: Sendet Payload an einen Spieler.
     */
    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void send(
            ServerPlayer player, T payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }

    public static void handleRequest(ServerboundRequestPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = playerOf(ctx);
            if (player == null) return;
            CMDCoreServices svc = CMDCoreServices.get();
            if (svc == null) return;
            PermissionService perms = svc.permissions();
            PermissionLevel level = perms.levelOf(player.getUUID());
            PermissionLevel minPanel = NWConfig.ADMIN.minPanelPermissionLevel.get();
            if (!NWConfig.ADMIN.enableAdminSystem.get() || !level.atLeast(minPanel)) {
                send(player, new ClientboundActionResultPayload(false,
                        "Du hast keine Berechtigung dafür.", "Mindeststufe: " + minPanel.displayName()));
                svc.audit().recordPanel(player, level, null, null,
                        "Verweigert: REQUEST " + payload.kind(), false,
                        "Stufe nicht ausreichend", "admin_panel");
                return;
            }
            // Pro-Aktion-Mindeststufe (zusätzlich zur Panel-Mindeststufe).
            PermissionLevel kindRequired = requiredLevelForRequest(payload.kind());
            if (!level.atLeast(kindRequired)) {
                send(player, new ClientboundActionResultPayload(false,
                        "Diese Daten benötigen Stufe " + kindRequired.displayName() + ".",
                        "Deine Stufe: " + level.displayName()));
                svc.audit().recordPanel(player, level, null, null,
                        "Verweigert: REQUEST " + payload.kind(), false,
                        "Stufe nicht ausreichend", "admin_panel");
                return;
            }
            switch (payload.kind()) {
                case OPEN_PANEL -> handleOpenPanel(player, svc);
                case OVERVIEW -> send(player, svc.status().buildOverview(svc));
                case PLAYERS -> send(player, svc.players().buildPlayerList());
                case PERMISSIONS -> sendPermissionList(player, svc);
                case MODULES -> sendModuleList(player, svc);
                case CONFIG -> send(player, new ClientboundConfigPayload(svc.configs().snapshot()));
                case LOGS -> sendLogs(player, svc, payload.parameter(), payload.limit());
                case HISTORY -> sendLogs(player, svc, payload.parameter(), payload.limit());
                case DEBUG -> sendDebug(player, svc);
                case SAVE_ALL -> {
                    svc.permissions().save();
                    svc.modules().save();
                    svc.audit().save();
                    svc.audit().recordPanel(player, level, null, null,
                            "Speichern (alle Daten)", true, null, "core");
                    send(player, new ClientboundActionResultPayload(true,
                            "Alle Daten wurden gespeichert.", ""));
                }
                case REFRESH -> {
                    svc.permissions().invalidateOpCache();
                    send(player, new ClientboundActionResultPayload(true, "Caches geleert.", ""));
                }
            }
        });
    }

    private static void handleOpenPanel(ServerPlayer player, CMDCoreServices svc) {
        PermissionLevel level = svc.permissions().levelOf(player.getUUID());
        PermissionLevel minPanel = NWConfig.ADMIN.minPanelPermissionLevel.get();
        if (!level.atLeast(minPanel)) {
            send(player, new ClientboundActionResultPayload(false,
                    "Du hast keine Berechtigung, das Admin-Panel zu öffnen.",
                    "Mindeststufe: " + minPanel.displayName()));
            return;
        }
        String accent = NWConfig.PANEL.panelAccentColor.get();
        String defaultTab = NWConfig.PANEL.panelDefaultTab.get();
        send(player, new ClientboundOpenPanelPayload(defaultTab, accent, level.name()));
        // Direkt erste Daten mitliefern
        send(player, svc.status().buildOverview(svc));
        svc.audit().recordPanel(player, level, null, null,
                "Admin-Panel geöffnet", true, null, "admin_panel");
    }

    private static void sendPermissionList(ServerPlayer player, CMDCoreServices svc) {
        List<ClientboundPermissionListPayload.Entry> list = new ArrayList<>();
        for (PermissionEntry e : svc.permissions().listEntries()) {
            list.add(new ClientboundPermissionListPayload.Entry(
                    e.name(), e.uuid().toString(), e.level().name(), e.source().name(), e.updatedAt()));
        }
        send(player, new ClientboundPermissionListPayload(list,
                svc.permissions().opFallbackEnabled(),
                svc.permissions().defaultLevel().name()));
    }

    private static void sendModuleList(ServerPlayer player, CMDCoreServices svc) {
        ModuleRegistry registry = svc.modules();
        List<ClientboundModuleListPayload.Entry> entries = new ArrayList<>();
        for (Module module : registry.sorted()) {
            entries.add(new ClientboundModuleListPayload.Entry(
                    module.id(), module.displayName(), module.description(),
                    module.isEnabled(), module.canBeDisabled(),
                    module.requiredLevel().name(),
                    module.version(), module.errorStatus()));
        }
        send(player, new ClientboundModuleListPayload(entries));
    }

    private static void sendLogs(ServerPlayer player, CMDCoreServices svc, String filter, int limit) {
        AuditService audit = svc.audit();
        int max = limit <= 0 ? 100 : Math.min(limit, 250);
        List<de.contentlos.cmdcore.common.logging.AuditEntry> filtered = audit.filter(e -> {
            if (filter == null || filter.isBlank()) return true;
            String f = filter.toLowerCase();
            return (e.action() != null && e.action().toLowerCase().contains(f))
                    || (e.adminName() != null && e.adminName().toLowerCase().contains(f))
                    || (e.targetName() != null && e.targetName().toLowerCase().contains(f))
                    || e.source().name().toLowerCase().contains(f);
        }, max);
        List<ClientboundLogListPayload.Entry> entries = new ArrayList<>();
        for (AuditEntry e : filtered) {
            entries.add(new ClientboundLogListPayload.Entry(
                    e.timestamp(),
                    nullSafe(e.adminName()),
                    nullSafe(e.adminLevel()),
                    nullSafe(e.targetName()),
                    nullSafe(e.action()),
                    e.source().name(),
                    e.success(),
                    nullSafe(e.details()),
                    nullSafe(e.module())
            ));
        }
        send(player, new ClientboundLogListPayload(filter == null ? "audit" : filter, entries));
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    private static void sendDebug(ServerPlayer player, CMDCoreServices svc) {
        List<ClientboundDebugPayload.Entry> rows = new ArrayList<>();
        rows.add(new ClientboundDebugPayload.Entry("Mod", "NightWatch Core (CMD Core)"));
        rows.add(new ClientboundDebugPayload.Entry("Debug aktiv", String.valueOf(svc.debugActive())));
        rows.add(new ClientboundDebugPayload.Entry("Permissions im Cache", String.valueOf(svc.permissions().listEntries().size())));
        rows.add(new ClientboundDebugPayload.Entry("Audit-Einträge", String.valueOf(svc.audit().size())));
        rows.add(new ClientboundDebugPayload.Entry("Online-Spieler",
                String.valueOf(svc.server().getPlayerCount())));
        rows.add(new ClientboundDebugPayload.Entry("Welt", svc.server().getWorldData().getLevelName()));
        rows.add(new ClientboundDebugPayload.Entry("TPS", String.valueOf(svc.status().averageTps())));
        rows.add(new ClientboundDebugPayload.Entry("Datenpfad",
                de.contentlos.cmdcore.common.data.CMDDataPaths.rootDir(svc.server()).toString()));
        send(player, new ClientboundDebugPayload(rows));
    }

    public static void handleAction(ServerboundActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = playerOf(ctx);
            if (player == null) return;
            CMDCoreServices svc = CMDCoreServices.get();
            if (svc == null) return;
            PermissionLevel adminLevel = svc.permissions().levelOf(player.getUUID());

            // Mindeststufe pro Aktion
            PermissionLevel required = requiredLevel(payload.action());
            if (!adminLevel.atLeast(required)) {
                send(player, new ClientboundActionResultPayload(false,
                        "Diese Aktion benötigt Stufe " + required.displayName() + ".",
                        "Deine Stufe: " + adminLevel.displayName()));
                svc.audit().recordPanel(player, adminLevel, null, null,
                        "Verweigert: " + payload.action(), false,
                        "Stufe nicht ausreichend", "admin_panel");
                return;
            }

            ServerPlayer target = resolveTarget(payload.targetUuid(), svc);
            AdminActionService actions = svc.adminActions();
            AdminActionService.ActionResult result;

            try {
                result = switch (payload.action()) {
                    case HEAL -> actions.heal(target);
                    case FEED -> actions.feed(target);
                    case FLY_TOGGLE -> actions.toggleFlight(target);
                    case SET_GAMEMODE -> actions.setGamemode(target, payload.stringArg());
                    case TELEPORT_TO_TARGET -> actions.teleportToTarget(player, target);
                    case TELEPORT_HERE -> actions.teleportHere(player, target);
                    case KICK -> actions.kick(target, payload.stringArg());
                    case BROADCAST -> actions.broadcast(payload.stringArg());
                    case TITLE_TO_TARGET -> actions.titleToTarget(target, payload.stringArg());
                    case SET_DAY -> actions.setDay();
                    case SET_NIGHT -> actions.setNight();
                    case SET_WEATHER -> actions.setWeather(payload.stringArg());
                    case SAVE_WORLD -> actions.saveWorld();
                    case SAVE_DATA -> {
                        svc.permissions().save();
                        svc.modules().save();
                        svc.audit().save();
                        yield AdminActionService.ActionResult.ok("Daten gespeichert.");
                    }
                    case RELOAD_CONFIG -> {
                        try {
                            NWConfig.SPEC.afterReload();
                        } catch (Throwable ignored) {}
                        svc.permissions().setOpFallbackEnabled(NWConfig.PERMISSIONS.allowOpFallback.get());
                        svc.permissions().setDefaultLevel(NWConfig.PERMISSIONS.defaultPermissionLevel.get());
                        svc.audit().setMaxEntries(NWConfig.LOGGING.maxHistoryEntries.get());
                        yield AdminActionService.ActionResult.ok("Konfiguration neu geladen.");
                    }
                    case RELOAD_PERMS -> {
                        svc.permissions().load();
                        svc.permissions().invalidateOpCache();
                        yield AdminActionService.ActionResult.ok("Permissions neu geladen.");
                    }
                    case RELOAD_MODULES -> {
                        svc.modules().reloadAll();
                        yield AdminActionService.ActionResult.ok("Module neu geladen.");
                    }
                    case TOGGLE_DEBUG -> {
                        boolean now = !svc.debugActive();
                        svc.setDebugActive(now);
                        NWConfig.GENERAL.enableDebugMode.set(now);
                        NWConfig.SPEC.save();
                        yield AdminActionService.ActionResult.ok(
                                "Debug-Modus " + (now ? "aktiviert" : "deaktiviert") + ".");
                    }
                    case REFRESH_OP_CACHE -> {
                        svc.permissions().invalidateOpCache();
                        yield AdminActionService.ActionResult.ok("OP-Cache geleert.");
                    }
                    case CLEAR_HISTORY -> {
                        svc.audit().clear();
                        svc.audit().save();
                        yield AdminActionService.ActionResult.ok("Verlauf gelöscht.");
                    }
                    case SHOW_INFO -> actions.playerInfo(target);
                    case COPY_COORDINATES -> {
                        if (target == null) yield AdminActionService.ActionResult.fail("Zielspieler nicht gefunden.");
                        yield AdminActionService.ActionResult.ok("Koordinaten",
                                actions.formatPos(target.position()));
                    }
                    case DEBUG_TEST_MESSAGE -> {
                        player.sendSystemMessage(Component.literal(
                                "[NightWatch Debug] Testnachricht an " + player.getGameProfile().getName()));
                        yield AdminActionService.ActionResult.ok("Testnachricht gesendet.");
                    }
                };
            } catch (Exception e) {
                LOGGER.error("CMD-Core: Aktion fehlgeschlagen: {}", payload.action(), e);
                result = AdminActionService.ActionResult.fail("Fehler: " + e.getMessage());
            }

            String targetName = target != null ? target.getGameProfile().getName() : null;
            UUID targetUuid = target != null ? target.getUUID() : null;
            svc.audit().recordPanel(player, adminLevel, targetName, targetUuid,
                    payload.action().name(), result.success(),
                    result.success() ? null : result.message(), "admin_panel");
            send(player, new ClientboundActionResultPayload(
                    result.success(), result.message(), result.hint()));
        });
    }

    private static @Nullable ServerPlayer resolveTarget(String uuid, CMDCoreServices svc) {
        if (uuid == null || uuid.isBlank()) return null;
        try {
            return svc.server().getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return svc.server().getPlayerList().getPlayerByName(uuid);
        }
    }

    /**
     * Mindeststufe für Datenanfragen (Lesezugriff im Panel).
     *
     * <p>Liegt zusätzlich zur Panel-Mindeststufe ({@code admin.minPanelPermissionLevel})
     * an, damit sensible Datenkanäle (Permissions, Logs, Debug, Config) nicht für
     * jede:n Spieler:in erreichbar sind, sobald die Panel-Mindeststufe niedriger
     * gesetzt wird.</p>
     */
    private static PermissionLevel requiredLevelForRequest(ServerboundRequestPayload.Kind kind) {
        return switch (kind) {
            case OPEN_PANEL, OVERVIEW, PLAYERS, MODULES -> PermissionLevel.MODERATOR;
            case LOGS, HISTORY, CONFIG, PERMISSIONS, REFRESH -> PermissionLevel.ADMIN;
            case DEBUG -> PermissionLevel.ADMIN;
            case SAVE_ALL -> PermissionLevel.MODERATOR;
        };
    }

    private static PermissionLevel requiredLevel(ServerboundActionPayload.Action action) {
        return switch (action) {
            case HEAL, FEED, SHOW_INFO, COPY_COORDINATES, BROADCAST, TITLE_TO_TARGET -> PermissionLevel.HELPER;
            case FLY_TOGGLE, SET_GAMEMODE, TELEPORT_TO_TARGET, TELEPORT_HERE,
                 KICK, SET_DAY, SET_NIGHT, SET_WEATHER, SAVE_WORLD, SAVE_DATA -> PermissionLevel.MODERATOR;
            case RELOAD_CONFIG, RELOAD_PERMS, RELOAD_MODULES, TOGGLE_DEBUG,
                 REFRESH_OP_CACHE, DEBUG_TEST_MESSAGE -> PermissionLevel.ADMIN;
            case CLEAR_HISTORY -> PermissionLevel.OWNER;
        };
    }

    public static void handleUpdatePermission(ServerboundUpdatePermissionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = playerOf(ctx);
            if (player == null) return;
            CMDCoreServices svc = CMDCoreServices.get();
            if (svc == null) return;
            PermissionLevel adminLevel = svc.permissions().levelOf(player.getUUID());
            if (!adminLevel.atLeast(PermissionLevel.OWNER)) {
                // Nur OWNER darf Permissions setzen
                send(player, new ClientboundActionResultPayload(false,
                        "Nur OWNER dürfen Berechtigungen ändern.", ""));
                svc.audit().recordPanel(player, adminLevel, payload.targetName(), null,
                        "Verweigert: Permission-Änderung", false, null, "permissions");
                return;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(payload.targetUuid());
            } catch (Exception e) {
                send(player, new ClientboundActionResultPayload(false,
                        "Ungültige Spieler-UUID.", ""));
                return;
            }
            String action = payload.action() == null ? "SET" : payload.action().toUpperCase();
            String message;
            if ("REMOVE".equals(action)) {
                boolean removed = svc.permissions().removeEntry(uuid);
                message = removed ? "Eintrag entfernt." : "Kein Eintrag gefunden.";
                svc.audit().recordPanel(player, adminLevel, payload.targetName(), uuid,
                        "Permission entfernt", removed, null, "permissions");
                send(player, new ClientboundActionResultPayload(removed, message, ""));
                return;
            }
            PermissionLevel newLevel = PermissionLevel.parseStrict(payload.level());
            if (newLevel == null) {
                svc.audit().recordPanel(player, adminLevel, payload.targetName(), uuid,
                        "Verweigert: Permission-Set mit unbekannter Stufe \""
                                + payload.level() + "\"", false,
                        "Stufe ungültig", "permissions");
                send(player, new ClientboundActionResultPayload(false,
                        "Unbekannte Stufe: " + payload.level()
                                + ". Erlaubt: USER, HELPER, MODERATOR, ADMIN, OWNER.",
                        ""));
                return;
            }
            if (!newLevel.isAssignable()) {
                svc.audit().recordPanel(player, adminLevel, payload.targetName(), uuid,
                        "Verweigert: Permission auf " + newLevel.name(), false,
                        "Stufe nicht zuweisbar", "permissions");
                send(player, new ClientboundActionResultPayload(false,
                        "Stufe " + newLevel.name() + " ist reserviert für die Server-Konsole und nicht zuweisbar.",
                        ""));
                return;
            }
            svc.permissions().setLevel(uuid, payload.targetName(), newLevel,
                    PermissionEntry.Source.JSON);
            svc.audit().recordPanel(player, adminLevel, payload.targetName(), uuid,
                    "Permission auf " + newLevel.name() + " gesetzt", true, null, "permissions");
            send(player, new ClientboundActionResultPayload(true,
                    payload.targetName() + " ist nun " + newLevel.displayName() + ".", ""));
        });
    }

    public static void handleUpdateConfig(ServerboundUpdateConfigPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = playerOf(ctx);
            if (player == null) return;
            CMDCoreServices svc = CMDCoreServices.get();
            if (svc == null) return;
            PermissionLevel adminLevel = svc.permissions().levelOf(player.getUUID());
            if (!adminLevel.atLeast(PermissionLevel.ADMIN)) {
                send(player, new ClientboundActionResultPayload(false,
                        "Nur ADMIN/OWNER dürfen Configs ändern.", ""));
                return;
            }
            String result = svc.configs().applyChange(payload.key(), payload.value());
            boolean ok = result.startsWith("OK");
            svc.audit().recordPanel(player, adminLevel, null, null,
                    "Config " + payload.key() + " = " + payload.value(),
                    ok, ok ? null : result, "config");
            send(player, new ClientboundActionResultPayload(ok, result, ""));
        });
    }

    public static void handleToggleModule(ServerboundToggleModulePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = playerOf(ctx);
            if (player == null) return;
            CMDCoreServices svc = CMDCoreServices.get();
            if (svc == null) return;
            PermissionLevel adminLevel = svc.permissions().levelOf(player.getUUID());
            if (!adminLevel.atLeast(PermissionLevel.ADMIN)) {
                send(player, new ClientboundActionResultPayload(false,
                        "Nur ADMIN/OWNER dürfen Module steuern.", ""));
                return;
            }
            ModuleRegistry registry = svc.modules();
            String id = payload.moduleId();
            String action = payload.action() == null ? "TOGGLE" : payload.action().toUpperCase();
            Module module = registry.get(id).orElse(null);
            if (module == null) {
                send(player, new ClientboundActionResultPayload(false,
                        "Unbekanntes Modul: " + id, ""));
                return;
            }
            String message;
            boolean success;
            switch (action) {
                case "ENABLE" -> {
                    success = registry.setEnabled(id, true);
                    message = success ? "Modul " + module.displayName() + " aktiviert."
                            : "Modul konnte nicht aktiviert werden.";
                }
                case "DISABLE" -> {
                    if (!module.canBeDisabled()) {
                        success = false;
                        message = "Modul " + module.displayName() + " kann nicht deaktiviert werden.";
                    } else {
                        success = registry.setEnabled(id, false);
                        message = success ? "Modul " + module.displayName() + " deaktiviert."
                                : "Modul konnte nicht deaktiviert werden.";
                    }
                }
                case "RELOAD" -> {
                    registry.reload(id);
                    success = true;
                    message = "Modul " + module.displayName() + " neu geladen.";
                }
                default -> {
                    success = registry.setEnabled(id, !module.isEnabled());
                    message = success ? "Modul " + module.displayName() + " umgeschaltet."
                            : "Umschalten fehlgeschlagen.";
                }
            }
            svc.audit().recordPanel(player, adminLevel, null, null,
                    "Modul " + id + " " + action, success, success ? null : message, "modules");
            send(player, new ClientboundActionResultPayload(success, message, ""));
        });
    }
}
