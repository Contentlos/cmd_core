package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.PanelClient;
import de.contentlos.cmdcore.client.gui.PanelState;
import de.contentlos.cmdcore.common.network.payload.ClientboundModuleListPayload;
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
public class ModulesTab extends PanelTab {

    private EditBox idField;

    @Override
    public String title() { return "Module"; }

    @Override
    public String subtitle() { return "Module aktivieren, deaktivieren oder neu laden."; }

    @Override
    protected void onInit() {
        var font = Minecraft.getInstance().font;
        int x = screen.contentX();
        int y = screen.contentY() + 26;
        idField = new EditBox(font, x, y + 4, 200, 20, Component.literal("Modul-ID"));
        idField.setHint(Component.literal("Modul-ID (z.B. logging)"));
        widgets.add(idField);

        int by = screen.contentY() + screen.contentHeight() - 26;
        int bx = x;
        widgets.add(Button.builder(Component.literal("Aktivieren"), b ->
                        PanelClient.sendToggleModule(idField.getValue().trim(), "ENABLE"))
                .bounds(bx, by, 90, 20).build());
        bx += 94;
        widgets.add(Button.builder(Component.literal("Deaktivieren"), b ->
                        PanelClient.sendToggleModule(idField.getValue().trim(), "DISABLE"))
                .bounds(bx, by, 100, 20).build());
        bx += 104;
        widgets.add(Button.builder(Component.literal("Reload"), b ->
                        PanelClient.sendToggleModule(idField.getValue().trim(), "RELOAD"))
                .bounds(bx, by, 70, 20).build());
        bx += 74;
        widgets.add(Button.builder(Component.literal("Alle Reload"), b ->
                        PanelClient.sendAction(ServerboundActionPayload.Action.RELOAD_MODULES, "", "", 0))
                .bounds(bx, by, 100, 20).build());
        bx += 104;
        widgets.add(Button.builder(Component.literal("Aktualisieren"), b ->
                        PanelClient.sendRequest(ServerboundRequestPayload.Kind.MODULES))
                .bounds(bx, by, 100, 20).build());
    }

    @Override
    public void requestRefresh() {
        PanelClient.sendRequest(ServerboundRequestPayload.Kind.MODULES);
    }

    @Override
    protected void renderContent(GuiGraphics g, Font font, int x, int y, int w, int h,
                                 int mouseX, int mouseY, float partialTick) {
        ClientboundModuleListPayload list = PanelState.get().modules();
        int headerY = y + 28;
        if (list == null) {
            g.drawString(font, "§7Keine Daten geladen.", x, y, screen.mutedColor(), false);
            return;
        }
        g.fill(x, headerY, x + w, headerY + 12, 0xFF1A2030);
        g.drawString(font, "§lID", x + 4, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lName", x + 120, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lStatus", x + 280, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lStufe", x + 350, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lAbschaltbar", x + 420, headerY + 2, screen.textColor(), false);

        int row = headerY + 14;
        int idx = 0;
        for (var m : list.modules()) {
            if (row + 12 > y + h - 28) break;
            int rowBg = (idx % 2 == 0) ? 0xFF202736 : 0xFF1B212D;
            g.fill(x, row, x + w, row + 12, rowBg);
            g.drawString(font, m.id(), x + 4, row + 2, screen.textColor(), false);
            g.drawString(font, m.name(), x + 120, row + 2, screen.textColor(), false);
            int statusColor = m.enabled() ? 0xFF22C55E : 0xFFEF4444;
            g.drawString(font, m.enabled() ? "AKTIV" : "AUS", x + 280, row + 2, statusColor, false);
            g.drawString(font, m.requiredLevel(), x + 350, row + 2, screen.mutedColor(), false);
            g.drawString(font, m.canBeDisabled() ? "ja" : "nein", x + 420, row + 2,
                    m.canBeDisabled() ? screen.textColor() : 0xFFEF4444, false);
            row += 12;
            idx++;
        }
    }
}
