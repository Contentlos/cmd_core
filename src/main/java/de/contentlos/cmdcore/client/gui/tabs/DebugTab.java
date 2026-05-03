package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.PanelClient;
import de.contentlos.cmdcore.client.gui.PanelState;
import de.contentlos.cmdcore.common.network.payload.ClientboundDebugPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundActionPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugTab extends PanelTab {

    @Override
    public String title() { return "Debug"; }

    @Override
    public String subtitle() { return "Detaillierte Diagnostik (nur ADMIN/OWNER)."; }

    @Override
    protected void onInit() {
        int x = screen.contentX();
        int by = screen.contentY() + screen.contentHeight() - 26;
        int bx = x;
        widgets.add(Button.builder(Component.literal("Debug umschalten"), b ->
                        PanelClient.sendAction(ServerboundActionPayload.Action.TOGGLE_DEBUG, "", "", 0))
                .bounds(bx, by, 130, 20).build());
        bx += 134;
        widgets.add(Button.builder(Component.literal("Test-Nachricht"), b ->
                        PanelClient.sendAction(ServerboundActionPayload.Action.DEBUG_TEST_MESSAGE, "", "", 0))
                .bounds(bx, by, 120, 20).build());
        bx += 124;
        widgets.add(Button.builder(Component.literal("OP-Cache"), b ->
                        PanelClient.sendAction(ServerboundActionPayload.Action.REFRESH_OP_CACHE, "", "", 0))
                .bounds(bx, by, 90, 20).build());
        bx += 94;
        widgets.add(Button.builder(Component.literal("Daten speichern"), b ->
                        PanelClient.sendAction(ServerboundActionPayload.Action.SAVE_DATA, "", "", 0))
                .bounds(bx, by, 110, 20).build());
        bx += 114;
        widgets.add(Button.builder(Component.literal("Aktualisieren"), b ->
                        PanelClient.sendRequest(ServerboundRequestPayload.Kind.DEBUG))
                .bounds(bx, by, 100, 20).build());
    }

    @Override
    public void requestRefresh() {
        PanelClient.sendRequest(ServerboundRequestPayload.Kind.DEBUG);
    }

    @Override
    protected void renderContent(GuiGraphics g, Font font, int x, int y, int w, int h,
                                 int mouseX, int mouseY, float partialTick) {
        ClientboundDebugPayload d = PanelState.get().debug();
        if (d == null) {
            g.drawString(font, "§7Keine Daten geladen.", x, y, screen.mutedColor(), false);
            return;
        }
        int row = y + 28;
        for (var e : d.rows()) {
            if (row + 12 > y + h - 28) break;
            g.drawString(font, "§7" + e.key() + ":", x, row, screen.mutedColor(), false);
            g.drawString(font, e.value(), x + 220, row, screen.textColor(), false);
            row += 12;
        }
    }
}
