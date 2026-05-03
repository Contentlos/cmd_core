package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.PanelClient;
import de.contentlos.cmdcore.client.gui.PanelState;
import de.contentlos.cmdcore.common.network.payload.ClientboundOverviewPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundActionPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerTab extends PanelTab {

    private EditBox broadcastField;

    @Override
    public String title() { return "Server"; }

    @Override
    public String subtitle() { return "Wetter, Tag/Nacht, Welt speichern, Broadcast."; }

    @Override
    protected void onInit() {
        var font = Minecraft.getInstance().font;
        int x = screen.contentX();
        int y = screen.contentY() + 26;
        broadcastField = new EditBox(font, x, y + 4, 360, 20, Component.literal("Broadcast"));
        broadcastField.setHint(Component.literal("Broadcast-Nachricht"));
        widgets.add(broadcastField);
        widgets.add(Button.builder(Component.literal("Broadcast senden"),
                b -> PanelClient.sendAction(ServerboundActionPayload.Action.BROADCAST, "",
                        broadcastField.getValue(), 0))
                .bounds(x + 368, y + 4, 130, 20).build());

        int by = screen.contentY() + screen.contentHeight() - 26;
        int bx = x;
        bx = quickBtn(bx, by, "Tag", ServerboundActionPayload.Action.SET_DAY);
        bx = quickBtn(bx, by, "Nacht", ServerboundActionPayload.Action.SET_NIGHT);
        bx = quickBtn(bx, by, "Klar", ServerboundActionPayload.Action.SET_WEATHER, "clear");
        bx = quickBtn(bx, by, "Regen", ServerboundActionPayload.Action.SET_WEATHER, "rain");
        bx = quickBtn(bx, by, "Gewitter", ServerboundActionPayload.Action.SET_WEATHER, "thunder");
        bx = quickBtn(bx, by, "Welt speichern", ServerboundActionPayload.Action.SAVE_WORLD);
        bx = quickBtn(bx, by, "Module reload", ServerboundActionPayload.Action.RELOAD_MODULES);
        bx = quickBtn(bx, by, "Config reload", ServerboundActionPayload.Action.RELOAD_CONFIG);
    }

    private int quickBtn(int x, int y, String label, ServerboundActionPayload.Action action) {
        return quickBtn(x, y, label, action, "");
    }

    private int quickBtn(int x, int y, String label, ServerboundActionPayload.Action action, String arg) {
        int w = Math.max(70, Minecraft.getInstance().font.width(label) + 12);
        widgets.add(Button.builder(Component.literal(label),
                b -> PanelClient.sendAction(action, "", arg, 0))
                .bounds(x, y, w, 20).build());
        return x + w + 4;
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
            g.drawString(font, "§7Daten werden geladen ...", x, y, screen.mutedColor(), false);
            return;
        }
        int row = y + 28;
        row = drawRow(g, font, x, row, "Server", o.serverName());
        row = drawRow(g, font, x, row, "Welt", o.worldName());
        row = drawRow(g, font, x, row, "Spieler", o.playersOnline() + " / " + o.playersMax());
        row = drawRow(g, font, x, row, "Uptime", formatUptime(o.uptimeSeconds()));
        row = drawRow(g, font, x, row, "TPS", String.format("%.2f", o.tps()));
        row = drawRow(g, font, x, row, "Speicher", o.memoryUsedMb() + " MB / " + o.memoryMaxMb() + " MB");
        row = drawRow(g, font, x, row, "Dimensionen", o.dimensions());
    }

    private int drawRow(GuiGraphics g, Font font, int x, int y, String label, String value) {
        g.drawString(font, "§7" + label + ":", x, y, screen.mutedColor(), false);
        g.drawString(font, value == null ? "?" : value, x + 110, y, screen.textColor(), false);
        return y + 12;
    }

    private String formatUptime(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%dh %02dm %02ds", h, m, s);
    }
}
