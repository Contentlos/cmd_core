package de.contentlos.cmdcore.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.contentlos.cmdcore.CMDCore;
import de.contentlos.cmdcore.common.config.NWConfig;
import de.contentlos.cmdcore.common.logging.AuditEntry;
import de.contentlos.cmdcore.common.logging.AuditService;
import de.contentlos.cmdcore.common.network.payload.ClientboundOpenPanelPayload;
import de.contentlos.cmdcore.common.permissions.PermissionEntry;
import de.contentlos.cmdcore.common.permissions.PermissionLevel;
import de.contentlos.cmdcore.common.permissions.PermissionService;
import de.contentlos.cmdcore.common.service.AdminActionService;
import de.contentlos.cmdcore.common.service.CMDCoreServices;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Befehlsregistrierung für /nw und /nightwatch.
 */
public final class NWCommands {

    private NWCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext buildContext,
                                 Commands.CommandSelection selection) {
        dispatcher.register(buildRoot("nw"));
        dispatcher.register(buildRoot("nightwatch"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot(String name) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name)
                .requires(NWCommands::canUseAny)
                .executes(NWCommands::runHelp);

        root.then(Commands.literal("help").executes(NWCommands::runHelp));
        root.then(Commands.literal("version").executes(NWCommands::runVersion));
        root.then(Commands.literal("status").executes(NWCommands::runStatus));
        root.then(Commands.literal("save").executes(NWCommands::runSave));
        root.then(Commands.literal("reload").executes(NWCommands::runReload));
        root.then(Commands.literal("panel").executes(NWCommands::runPanel));
        root.then(Commands.literal("debug").executes(NWCommands::runDebug));
        root.then(Commands.literal("modules").executes(NWCommands::runListModules));

        // Permissions
        LiteralArgumentBuilder<CommandSourceStack> perms = Commands.literal("perms")
                .requires(s -> hasLevel(s, PermissionLevel.ADMIN));
        perms.then(Commands.literal("get")
                .then(Commands.argument("spieler", EntityArgument.player())
                        .executes(NWCommands::permsGet)));
        perms.then(Commands.literal("set")
                .requires(s -> hasLevel(s, PermissionLevel.OWNER))
                .then(Commands.argument("spieler", EntityArgument.player())
                        .then(Commands.argument("level", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> {
                                    for (PermissionLevel l : PermissionLevel.assignableValues()) {
                                        b.suggest(l.name());
                                    }
                                    return b.buildFuture();
                                })
                                .executes(NWCommands::permsSet))));
        perms.then(Commands.literal("list").executes(NWCommands::permsList));
        perms.then(Commands.literal("remove")
                .requires(s -> hasLevel(s, PermissionLevel.OWNER))
                .then(Commands.argument("spieler", EntityArgument.player())
                        .executes(NWCommands::permsRemove)));
        perms.then(Commands.literal("reload").executes(NWCommands::permsReload));
        perms.then(Commands.literal("save").executes(NWCommands::permsSave));
        root.then(perms);

        // Admin-Aktionen — Tor auf der Brigadier-Ebene ist HELPER (niedrigste
        // tatsächlich verlangte Stufe), die einzelnen Subkommandos prüfen ihre
        // konkrete Mindeststufe danach selbst (siehe runAction/adminBroadcast/...).
        LiteralArgumentBuilder<CommandSourceStack> admin = Commands.literal("admin")
                .requires(s -> hasLevel(s, PermissionLevel.HELPER));
        admin.then(Commands.literal("heal")
                .then(Commands.argument("spieler", EntityArgument.player())
                        .executes(NWCommands::adminHeal)));
        admin.then(Commands.literal("feed")
                .then(Commands.argument("spieler", EntityArgument.player())
                        .executes(NWCommands::adminFeed)));
        admin.then(Commands.literal("fly")
                .then(Commands.argument("spieler", EntityArgument.player())
                        .executes(NWCommands::adminFlyToggle)));
        admin.then(Commands.literal("gamemode")
                .then(Commands.argument("spieler", EntityArgument.player())
                        .then(Commands.argument("modus", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> {
                                    b.suggest("survival"); b.suggest("creative");
                                    b.suggest("adventure"); b.suggest("spectator");
                                    return b.buildFuture();
                                })
                                .executes(NWCommands::adminGamemode))));
        admin.then(Commands.literal("tp")
                .then(Commands.argument("spieler", EntityArgument.player())
                        .then(Commands.argument("ziel", EntityArgument.player())
                                .executes(NWCommands::adminTp))));
        admin.then(Commands.literal("tphere")
                .then(Commands.argument("spieler", EntityArgument.player())
                        .executes(NWCommands::adminTpHere)));
        admin.then(Commands.literal("kick")
                .then(Commands.argument("spieler", EntityArgument.player())
                        .then(Commands.argument("grund", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                .executes(NWCommands::adminKick))));
        admin.then(Commands.literal("broadcast")
                .then(Commands.argument("nachricht", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(NWCommands::adminBroadcast)));
        admin.then(Commands.literal("day").executes(NWCommands::adminDay));
        admin.then(Commands.literal("night").executes(NWCommands::adminNight));
        admin.then(Commands.literal("weather")
                .then(Commands.argument("typ", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> {
                            b.suggest("clear"); b.suggest("rain"); b.suggest("thunder");
                            return b.buildFuture();
                        })
                        .executes(NWCommands::adminWeather)));
        admin.then(Commands.literal("info")
                .then(Commands.argument("spieler", EntityArgument.player())
                        .executes(NWCommands::adminInfo)));
        root.then(admin);

        // Logs / History
        root.then(Commands.literal("logs")
                .requires(s -> hasLevel(s, PermissionLevel.MODERATOR))
                .then(Commands.literal("latest").executes(c -> showLogs(c, null, 50)))
                .then(Commands.literal("audit").executes(c -> showLogs(c, null, 100))));
        root.then(Commands.literal("history")
                .requires(s -> hasLevel(s, PermissionLevel.MODERATOR))
                .executes(c -> showLogs(c, null, 50))
                .then(Commands.argument("spieler", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(c -> showLogs(c,
                                com.mojang.brigadier.arguments.StringArgumentType.getString(c, "spieler"),
                                50))));

        return root;
    }

    private static boolean canUseAny(CommandSourceStack source) {
        // /nw help muss für alle erreichbar sein, weshalb requires nicht zu strikt sein darf.
        // Tatsächliche Aktionen prüfen ihre Stufe selbst.
        return true;
    }

    private static boolean hasLevel(CommandSourceStack source, PermissionLevel required) {
        CMDCoreServices svc = CMDCoreServices.get();
        if (svc == null) {
            return source.hasPermission(2);
        }
        return svc.permissions().has(source, required);
    }

    private static int runHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        send(src, Component.literal("== NightWatch Core Hilfe ==").withStyle(ChatFormatting.AQUA));
        send(src, "/nw status – Serverstatus anzeigen");
        send(src, "/nw panel – Admin-Panel öffnen");
        send(src, "/nw save – Daten speichern");
        send(src, "/nw reload – Configs/Permissions neu laden");
        send(src, "/nw debug – Debug-Modus umschalten");
        send(src, "/nw modules – Module auflisten");
        send(src, "/nw perms get|set|list|remove|reload|save – Permissions");
        send(src, "/nw admin heal|feed|fly|gamemode|tp|tphere|kick|broadcast|day|night|weather|info");
        send(src, "/nw history|logs latest – Verlauf");
        return 1;
    }

    private static int runVersion(CommandContext<CommandSourceStack> ctx) {
        send(ctx.getSource(), Component.literal(
                "NightWatch Core (CMD Core) – " + CMDCore.VERSION).withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static int runStatus(CommandContext<CommandSourceStack> ctx) {
        CMDCoreServices svc = CMDCoreServices.required();
        send(ctx.getSource(), Component.literal("=== Serverstatus ===").withStyle(ChatFormatting.AQUA));
        send(ctx.getSource(), "Version: " + CMDCore.VERSION);
        send(ctx.getSource(), "Spieler: " + svc.server().getPlayerCount() + "/" + svc.server().getMaxPlayers());
        send(ctx.getSource(), "TPS: " + svc.status().averageTps());
        send(ctx.getSource(), "Module aktiv: " + svc.modules().sorted().stream()
                .filter(m -> m.isEnabled()).count() + " / " + svc.modules().sorted().size());
        send(ctx.getSource(), "Audit-Einträge: " + svc.audit().size());
        send(ctx.getSource(), "Debug: " + (svc.debugActive() ? "AN" : "AUS"));
        return 1;
    }

    private static int runSave(CommandContext<CommandSourceStack> ctx) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.ADMIN)) return denied(ctx);
        CMDCoreServices svc = CMDCoreServices.required();
        svc.permissions().save();
        svc.modules().save();
        svc.audit().save();
        svc.audit().recordCommand(ctx.getSource(), levelOf(ctx), null, null,
                "Daten gespeichert", true, null, "core");
        send(ctx.getSource(), Component.literal("Daten gespeichert.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int runReload(CommandContext<CommandSourceStack> ctx) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.ADMIN)) return denied(ctx);
        CMDCoreServices svc = CMDCoreServices.required();
        svc.permissions().load();
        svc.permissions().invalidateOpCache();
        svc.modules().load();
        svc.audit().recordCommand(ctx.getSource(), levelOf(ctx), null, null,
                "Reload", true, null, "core");
        send(ctx.getSource(), Component.literal("CMD-Core neu geladen.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int runPanel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            send(ctx.getSource(), Component.literal(
                    "Das Admin-Panel ist nur als Spieler im Spiel nutzbar.").withStyle(ChatFormatting.RED));
            return 0;
        }
        CMDCoreServices svc = CMDCoreServices.required();
        PermissionLevel level = svc.permissions().levelOf(player.getUUID());
        PermissionLevel min = NWConfig.ADMIN.minPanelPermissionLevel.get();
        if (!level.atLeast(min)) {
            send(ctx.getSource(), Component.literal(
                    "Du hast keine Berechtigung für das Admin-Panel.").withStyle(ChatFormatting.RED));
            svc.audit().recordCommand(ctx.getSource(), level, null, null,
                    "Verweigert: Panel öffnen", false,
                    "Stufe " + level.name(), "admin_panel");
            return 0;
        }
        PacketDistributor.sendToPlayer(player, new ClientboundOpenPanelPayload(
                NWConfig.PANEL.panelDefaultTab.get(),
                NWConfig.PANEL.panelAccentColor.get(),
                level.name()));
        PacketDistributor.sendToPlayer(player, svc.status().buildOverview(svc));
        svc.audit().recordCommand(ctx.getSource(), level, null, null,
                "Panel geöffnet", true, null, "admin_panel");
        return 1;
    }

    private static int runDebug(CommandContext<CommandSourceStack> ctx) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.ADMIN)) return denied(ctx);
        CMDCoreServices svc = CMDCoreServices.required();
        boolean now = !svc.debugActive();
        svc.setDebugActive(now);
        NWConfig.GENERAL.enableDebugMode.set(now);
        NWConfig.SPEC.save();
        svc.audit().recordCommand(ctx.getSource(), levelOf(ctx), null, null,
                "Debug " + (now ? "AN" : "AUS"), true, null, "debug");
        send(ctx.getSource(), Component.literal(
                "Debug-Modus: " + (now ? "AN" : "AUS")).withStyle(now ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        return 1;
    }

    private static int runListModules(CommandContext<CommandSourceStack> ctx) {
        CMDCoreServices svc = CMDCoreServices.required();
        send(ctx.getSource(), Component.literal("=== Module ===").withStyle(ChatFormatting.AQUA));
        for (var m : svc.modules().sorted()) {
            send(ctx.getSource(), (m.isEnabled() ? "[AN] " : "[AUS] ") + m.id() + " – " + m.displayName());
        }
        return 1;
    }

    private static int permsGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!hasLevel(ctx.getSource(), PermissionLevel.MODERATOR)) return denied(ctx);
        ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
        CMDCoreServices svc = CMDCoreServices.required();
        PermissionLevel level = svc.permissions().levelOf(target.getUUID());
        send(ctx.getSource(), target.getGameProfile().getName() + " ist " + level.displayName()
                + " (Quelle: " + svc.permissions().sourceOf(target.getUUID()).name() + ")");
        return 1;
    }

    private static int permsSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!hasLevel(ctx.getSource(), PermissionLevel.OWNER)) return denied(ctx);
        ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
        String levelName = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "level");
        PermissionLevel newLevel = PermissionLevel.parse(levelName);
        if (!newLevel.isAssignable()) {
            send(ctx.getSource(), Component.literal(
                    "Stufe " + newLevel.name() + " ist reserviert für die Server-Konsole und nicht zuweisbar.")
                    .withStyle(ChatFormatting.RED));
            CMDCoreServices.required().audit().recordCommand(ctx.getSource(), levelOf(ctx),
                    target.getGameProfile().getName(), target.getUUID(),
                    "Verweigert: Permission auf " + newLevel.name(), false,
                    "Stufe nicht zuweisbar", "permissions");
            return 0;
        }
        CMDCoreServices svc = CMDCoreServices.required();
        svc.permissions().setLevel(target.getUUID(), target.getGameProfile().getName(), newLevel,
                PermissionEntry.Source.JSON);
        svc.audit().recordCommand(ctx.getSource(), levelOf(ctx),
                target.getGameProfile().getName(), target.getUUID(),
                "Permission auf " + newLevel.name() + " gesetzt", true, null, "permissions");
        send(ctx.getSource(), target.getGameProfile().getName() + " ist nun " + newLevel.displayName() + ".");
        return 1;
    }

    private static int permsList(CommandContext<CommandSourceStack> ctx) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.MODERATOR)) return denied(ctx);
        CMDCoreServices svc = CMDCoreServices.required();
        var entries = svc.permissions().listEntries();
        send(ctx.getSource(), Component.literal("=== Berechtigungen (" + entries.size() + ") ===")
                .withStyle(ChatFormatting.AQUA));
        for (PermissionEntry e : entries) {
            send(ctx.getSource(), e.name() + " – " + e.level().displayName());
        }
        return 1;
    }

    private static int permsRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!hasLevel(ctx.getSource(), PermissionLevel.OWNER)) return denied(ctx);
        ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
        CMDCoreServices svc = CMDCoreServices.required();
        boolean removed = svc.permissions().removeEntry(target.getUUID());
        svc.audit().recordCommand(ctx.getSource(), levelOf(ctx),
                target.getGameProfile().getName(), target.getUUID(),
                "Permission entfernt", removed, null, "permissions");
        send(ctx.getSource(), removed ? "Eintrag entfernt." : "Kein Eintrag vorhanden.");
        return 1;
    }

    private static int permsReload(CommandContext<CommandSourceStack> ctx) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.ADMIN)) return denied(ctx);
        CMDCoreServices svc = CMDCoreServices.required();
        svc.permissions().load();
        svc.permissions().invalidateOpCache();
        send(ctx.getSource(), Component.literal("Permissions neu geladen.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int permsSave(CommandContext<CommandSourceStack> ctx) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.ADMIN)) return denied(ctx);
        CMDCoreServices.required().permissions().save();
        send(ctx.getSource(), Component.literal("Permissions gespeichert.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int adminHeal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return runAction(ctx, PermissionLevel.HELPER, EntityArgument.getPlayer(ctx, "spieler"),
                t -> CMDCoreServices.required().adminActions().heal(t), "HEAL");
    }
    private static int adminFeed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return runAction(ctx, PermissionLevel.HELPER, EntityArgument.getPlayer(ctx, "spieler"),
                t -> CMDCoreServices.required().adminActions().feed(t), "FEED");
    }
    private static int adminFlyToggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return runAction(ctx, PermissionLevel.MODERATOR, EntityArgument.getPlayer(ctx, "spieler"),
                t -> CMDCoreServices.required().adminActions().toggleFlight(t), "FLY_TOGGLE");
    }
    private static int adminGamemode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
        String mode = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "modus");
        return runAction(ctx, PermissionLevel.MODERATOR, target,
                t -> CMDCoreServices.required().adminActions().setGamemode(t, mode),
                "SET_GAMEMODE:" + mode);
    }
    private static int adminTp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer who = EntityArgument.getPlayer(ctx, "spieler");
        ServerPlayer to = EntityArgument.getPlayer(ctx, "ziel");
        return runAction(ctx, PermissionLevel.MODERATOR, who,
                t -> CMDCoreServices.required().adminActions().teleportToTarget(t, to),
                "TELEPORT_TO:" + to.getGameProfile().getName());
    }
    private static int adminTpHere(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
        ServerPlayer self = ctx.getSource().getPlayer();
        if (self == null) {
            send(ctx.getSource(), Component.literal("Nur als Spieler nutzbar.").withStyle(ChatFormatting.RED));
            return 0;
        }
        return runAction(ctx, PermissionLevel.MODERATOR, target,
                t -> CMDCoreServices.required().adminActions().teleportHere(self, t),
                "TELEPORT_HERE");
    }
    private static int adminKick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
        String reason = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "grund");
        return runAction(ctx, PermissionLevel.MODERATOR, target,
                t -> CMDCoreServices.required().adminActions().kick(t, reason), "KICK");
    }
    private static int adminBroadcast(CommandContext<CommandSourceStack> ctx) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.HELPER)) return denied(ctx);
        String msg = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "nachricht");
        AdminActionService.ActionResult r = CMDCoreServices.required().adminActions().broadcast(msg);
        feedback(ctx, r);
        CMDCoreServices.required().audit().recordCommand(ctx.getSource(), levelOf(ctx),
                null, null, "BROADCAST", r.success(), r.success() ? null : r.message(), "admin");
        return r.success() ? 1 : 0;
    }
    private static int adminDay(CommandContext<CommandSourceStack> ctx) {
        return adminTime(ctx, true);
    }
    private static int adminNight(CommandContext<CommandSourceStack> ctx) {
        return adminTime(ctx, false);
    }
    private static int adminTime(CommandContext<CommandSourceStack> ctx, boolean day) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.MODERATOR)) return denied(ctx);
        var svc = CMDCoreServices.required();
        AdminActionService.ActionResult r = day ? svc.adminActions().setDay() : svc.adminActions().setNight();
        feedback(ctx, r);
        svc.audit().recordCommand(ctx.getSource(), levelOf(ctx), null, null,
                day ? "SET_DAY" : "SET_NIGHT", r.success(), null, "admin");
        return r.success() ? 1 : 0;
    }
    private static int adminWeather(CommandContext<CommandSourceStack> ctx) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.MODERATOR)) return denied(ctx);
        String type = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "typ");
        AdminActionService.ActionResult r = CMDCoreServices.required().adminActions().setWeather(type);
        feedback(ctx, r);
        CMDCoreServices.required().audit().recordCommand(ctx.getSource(), levelOf(ctx),
                null, null, "SET_WEATHER:" + type, r.success(), null, "admin");
        return r.success() ? 1 : 0;
    }
    private static int adminInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!hasLevel(ctx.getSource(), PermissionLevel.HELPER)) return denied(ctx);
        ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
        AdminActionService.ActionResult r = CMDCoreServices.required().adminActions().playerInfo(target);
        send(ctx.getSource(), r.message());
        if (!r.hint().isEmpty()) {
            for (String line : r.hint().split("\n")) {
                send(ctx.getSource(), line);
            }
        }
        return 1;
    }

    private static int showLogs(CommandContext<CommandSourceStack> ctx, String filter, int limit) {
        if (!hasLevel(ctx.getSource(), PermissionLevel.MODERATOR)) return denied(ctx);
        AuditService audit = CMDCoreServices.required().audit();
        List<AuditEntry> entries = audit.filter(e -> {
            if (filter == null || filter.isBlank()) return true;
            String f = filter.toLowerCase();
            return (e.adminName() != null && e.adminName().toLowerCase().contains(f))
                    || (e.targetName() != null && e.targetName().toLowerCase().contains(f))
                    || (e.action() != null && e.action().toLowerCase().contains(f));
        }, limit);
        send(ctx.getSource(), Component.literal("=== Verlauf (" + entries.size() + ") ===")
                .withStyle(ChatFormatting.AQUA));
        for (AuditEntry e : entries) {
            String prefix = e.success() ? "[OK] " : "[FAIL] ";
            String line = prefix + e.adminName() + " (" + e.adminLevel() + ") - " + e.action();
            if (e.targetName() != null) line += " -> " + e.targetName();
            line += " (" + e.source() + ")";
            send(ctx.getSource(), line);
        }
        return 1;
    }

    @FunctionalInterface
    private interface ActionFn {
        AdminActionService.ActionResult apply(ServerPlayer target);
    }

    private static int runAction(CommandContext<CommandSourceStack> ctx,
                                 PermissionLevel required,
                                 ServerPlayer target,
                                 ActionFn fn, String actionLabel) {
        if (!hasLevel(ctx.getSource(), required)) return denied(ctx);
        AdminActionService.ActionResult r = fn.apply(target);
        feedback(ctx, r);
        var svc = CMDCoreServices.required();
        svc.audit().recordCommand(ctx.getSource(), levelOf(ctx),
                target != null ? target.getGameProfile().getName() : null,
                target != null ? target.getUUID() : null,
                actionLabel, r.success(), r.success() ? null : r.message(), "admin");
        return r.success() ? 1 : 0;
    }

    private static void feedback(CommandContext<CommandSourceStack> ctx, AdminActionService.ActionResult r) {
        MutableComponent msg = Component.literal(r.message());
        msg.withStyle(r.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
        ctx.getSource().sendSuccess(() -> msg, true);
    }

    private static PermissionLevel levelOf(CommandContext<CommandSourceStack> ctx) {
        PermissionService perms = CMDCoreServices.required().permissions();
        return perms.levelOfSource(ctx.getSource());
    }

    private static int denied(CommandContext<CommandSourceStack> ctx) {
        send(ctx.getSource(), Component.literal(
                "Du hast keine Berechtigung dafür.").withStyle(ChatFormatting.RED));
        return 0;
    }

    private static void send(CommandSourceStack source, String text) {
        send(source, Component.literal(text));
    }
    private static void send(CommandSourceStack source, Component component) {
        source.sendSuccess(() -> component, false);
    }

    private static UUID parseUuidOrNull(String s) {
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}
