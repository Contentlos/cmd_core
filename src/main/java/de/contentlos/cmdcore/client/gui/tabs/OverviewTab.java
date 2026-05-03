package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.PanelClient;
import de.contentlos.cmdcore.client.gui.PanelState;
import de.contentlos.cmdcore.common.network.payload.ClientboundOverviewPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundActionPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OverviewTab extends PanelTab {

    @Override
    public String title() { return "Übersicht"; }

    @Override
    public String subtitle() { return "Servereckdaten und Schnellzugriffe."; }

    @Override
    protected void onInit() {
        int x = screen.contentX();
        int y = screen.contentY() + screen.contentHeight() - 26;
        int btnW = 130;
        int btnH = 20;
        int gap = 4;
        int cx = x;
        widgets.add(Button.builder(Component.literal("Aktualisieren"),
                b -> PanelClient.sendRequest(ServerboundRequestPayload.Kind.OVERVIEW))
                .bounds(cx, y, btnW, btnH).build());
        cx += btnW + gap;
        widgets.add(Button.builder(Component.literal("Daten speichern"),
                b -> PanelClient.sendAction(ServerboundActionPayload.Action.SAVE_DATA, "", "", 0))
                .bounds(cx, y, btnW, btnH).build());
        cx += btnW + gap;
        widgets.add(Button.builder(Component.literal("Config neu laden"),
                b -> PanelClient.sendAction(ServerboundActionPayload.Action.RELOAD_CONFIG, "", "", 0))
                .bounds(cx, y, btnW, btnH).build());
        cx += btnW + gap;
        widgets.add(Button.builder(Component.literal("Debug umschalten"),
                b -> PanelClient.sendAction(ServerboundActionPayload.Action.TOGGLE_DEBUG, "", "", 0))
                .bounds(cx, y, btnW, btnH).build());
        cx += btnW + gap;
        widgets.add(Button.builder(Component.literal("Module neu laden"),
                b -> PanelClient.sendAction(ServerboundActionPayload.Action.RELOAD_MODULES, "", "", 0))
                .bounds(cx, y, btnW, btnH).build());
    }

    @Override
    public void requestRefresh() {
        PanelClient.sendRequest(ServerboundRequestPayload.Kind.OVERVIEW);
    }

    @Override
    protected void renderContent(GuiGraphics g, Font font, int x, int y, int w, int h,
                                 int mouseX, int mouseY, float partialTick) {
        ClientboundOverviewPayload o = PanelState.get().overview();
        if (o == null) {
            g.drawString(font, "§7Keine Daten geladen. Bitte 'Aktualisieren' drücken.", x, y, screen.mutedColor(), false);
            return;
        }
        int col1 = x;
        int col2 = x + w / 2 + 4;
        int row = y;
        row = drawRow(g, font, col1, row, "Servername", o.serverName());
        drawRow(g, font, col2, y, "Welt", o.worldName());
        row = drawRow(g, font, col1, row, "Minecraft", o.mcVersion());
        drawRow(g, font, col2, y + 12, "NeoForge", o.neoForgeVersion());
        row = drawRow(g, font, col1, row, "Core-Version", o.coreVersion());
        drawRow(g, font, col2, y + 24, "Spieler",
                o.playersOnline() + " / " + o.playersMax());
        row = drawRow(g, font, col1, row, "TPS", String.format("%.2f", o.tps()));
        drawRow(g, font, col2, y + 36, "Uptime", formatUptime(o.uptimeSeconds()));
        row = drawRow(g, font, col1, row, "Dimensionen", o.dimensions());
        drawRow(g, font, col2, y + 48, "Speicher",
                o.memoryUsedMb() + " MB / " + o.memoryMaxMb() + " MB");
        row = drawRow(g, font, col1, row, "Module", o.loadedModules());
        drawRow(g, font, col2, y + 60, "Permissions", o.permissionStatus());
        row = drawRow(g, font, col1, row, "Audit", o.auditStatus());
        drawRow(g, font, col2, y + 72, "Debug", o.debugActive() ? "AKTIV" : "AUS");
    }

    private int drawRow(GuiGraphics g, Font font, int x, int y, String label, String value) {
        g.drawString(font, "§7" + label + ":", x, y, screen.mutedColor(), false);
        g.drawString(font, value == null ? "?" : value, x + 100, y, screen.textColor(), false);
        return y + 12;
    }

    private String formatUptime(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%dh %02dm %02ds", h, m, s);
    }
}
