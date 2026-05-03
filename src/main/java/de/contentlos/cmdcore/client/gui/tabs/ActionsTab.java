package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.PanelClient;
import de.contentlos.cmdcore.common.network.payload.ServerboundActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ActionsTab extends PanelTab {

    private EditBox targetField;
    private EditBox argField;

    @Override
    public String title() { return "Admin-Aktionen"; }

    @Override
    public String subtitle() { return "Sammlung aller Aktionen mit Zielspieler und freiem Argument."; }

    @Override
    protected void onInit() {
        var font = Minecraft.getInstance().font;
        int x = screen.contentX();
        int y = screen.contentY() + 26;
        targetField = new EditBox(font, x, y + 4, 160, 20, Component.literal("Zielspieler"));
        targetField.setHint(Component.literal("Zielspieler"));
        widgets.add(targetField);
        argField = new EditBox(font, x + 168, y + 4, 320, 20, Component.literal("Argument"));
        argField.setHint(Component.literal("Argument (z.B. Nachricht / Modus / Grund)"));
        widgets.add(argField);

        int gx = x;
        int gy = y + 32;
        int gap = 4;
        gx = btn(gx, gy, "Heilen", ServerboundActionPayload.Action.HEAL);
        gx = btn(gx, gy, "Füttern", ServerboundActionPayload.Action.FEED);
        gx = btn(gx, gy, "Flug", ServerboundActionPayload.Action.FLY_TOGGLE);
        gx = btn(gx, gy, "Tp zu", ServerboundActionPayload.Action.TELEPORT_TO_TARGET);
        gx = btn(gx, gy, "Tp her", ServerboundActionPayload.Action.TELEPORT_HERE);
        gx = btn(gx, gy, "Kick", ServerboundActionPayload.Action.KICK);
        gx = btn(gx, gy, "Info", ServerboundActionPayload.Action.SHOW_INFO);
        gx = btn(gx, gy, "Koordinaten", ServerboundActionPayload.Action.COPY_COORDINATES);

        gx = x; gy += 24;
        gx = btn(gx, gy, "Survival", ServerboundActionPayload.Action.SET_GAMEMODE, "survival");
        gx = btn(gx, gy, "Creative", ServerboundActionPayload.Action.SET_GAMEMODE, "creative");
        gx = btn(gx, gy, "Adventure", ServerboundActionPayload.Action.SET_GAMEMODE, "adventure");
        gx = btn(gx, gy, "Spectator", ServerboundActionPayload.Action.SET_GAMEMODE, "spectator");
        gx = btn(gx, gy, "Tag", ServerboundActionPayload.Action.SET_DAY);
        gx = btn(gx, gy, "Nacht", ServerboundActionPayload.Action.SET_NIGHT);

        gx = x; gy += 24;
        gx = btn(gx, gy, "Klar", ServerboundActionPayload.Action.SET_WEATHER, "clear");
        gx = btn(gx, gy, "Regen", ServerboundActionPayload.Action.SET_WEATHER, "rain");
        gx = btn(gx, gy, "Gewitter", ServerboundActionPayload.Action.SET_WEATHER, "thunder");
        gx = btn(gx, gy, "Welt speichern", ServerboundActionPayload.Action.SAVE_WORLD);
        gx = btn(gx, gy, "Daten speichern", ServerboundActionPayload.Action.SAVE_DATA);

        gx = x; gy += 24;
        gx = btnArg(gx, gy, "Broadcast", ServerboundActionPayload.Action.BROADCAST);
        gx = btnArg(gx, gy, "Titel an Ziel", ServerboundActionPayload.Action.TITLE_TO_TARGET);
        gx = btn(gx, gy, "Test-Nachricht", ServerboundActionPayload.Action.DEBUG_TEST_MESSAGE);
    }

    private int btn(int x, int y, String label, ServerboundActionPayload.Action action) {
        return btn(x, y, label, action, "");
    }

    private int btn(int x, int y, String label, ServerboundActionPayload.Action action, String fixedArg) {
        int w = Math.max(60, Minecraft.getInstance().font.width(label) + 12);
        widgets.add(Button.builder(Component.literal(label), b -> {
            String target = targetField == null ? "" : targetField.getValue().trim();
            String arg = fixedArg.isEmpty() ? (argField == null ? "" : argField.getValue().trim()) : fixedArg;
            PanelClient.sendAction(action, target, arg, 0);
        }).bounds(x, y, w, 20).build());
        return x + w + 4;
    }

    private int btnArg(int x, int y, String label, ServerboundActionPayload.Action action) {
        int w = Math.max(80, Minecraft.getInstance().font.width(label) + 12);
        widgets.add(Button.builder(Component.literal(label), b -> {
            String target = targetField == null ? "" : targetField.getValue().trim();
            String arg = argField == null ? "" : argField.getValue().trim();
            PanelClient.sendAction(action, target, arg, 0);
        }).bounds(x, y, w, 20).build());
        return x + w + 4;
    }

    @Override
    protected void renderContent(GuiGraphics g, Font font, int x, int y, int w, int h,
                                 int mouseX, int mouseY, float partialTick) {
        g.drawString(font, "§7Hinweis: Server prüft jede Aktion und schreibt Audit-Log.", x, y, screen.mutedColor(), false);
        g.drawString(font, "§7Argument-Feld dient für Broadcast, Titel, Kick-Grund usw.", x, y + 12, screen.mutedColor(), false);
    }
}
