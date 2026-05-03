package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.PanelClient;
import de.contentlos.cmdcore.client.gui.PanelState;
import de.contentlos.cmdcore.common.network.payload.ClientboundConfigPayload;
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
public class ConfigTab extends PanelTab {

    private EditBox keyField;
    private EditBox valueField;

    @Override
    public String title() { return "Einstellungen"; }

    @Override
    public String subtitle() { return "Konfiguration anzeigen und (mit Berechtigung) ändern."; }

    @Override
    protected void onInit() {
        var font = Minecraft.getInstance().font;
        int x = screen.contentX();
        int y = screen.contentY() + 26;
        keyField = new EditBox(font, x, y + 4, 200, 20, Component.literal("Schlüssel"));
        keyField.setHint(Component.literal("z.B. panel.accentColor"));
        widgets.add(keyField);
        valueField = new EditBox(font, x + 208, y + 4, 220, 20, Component.literal("Wert"));
        valueField.setHint(Component.literal("Wert"));
        widgets.add(valueField);

        widgets.add(Button.builder(Component.literal("Setzen"), b ->
                        PanelClient.sendUpdateConfig(keyField.getValue().trim(), valueField.getValue()))
                .bounds(x + 432, y + 4, 70, 20).build());

        int by = screen.contentY() + screen.contentHeight() - 26;
        int bx = x;
        widgets.add(Button.builder(Component.literal("Aktualisieren"), b ->
                        PanelClient.sendRequest(ServerboundRequestPayload.Kind.CONFIG))
                .bounds(bx, by, 100, 20).build());
        bx += 104;
        widgets.add(Button.builder(Component.literal("Config reload"), b ->
                        PanelClient.sendAction(ServerboundActionPayload.Action.RELOAD_CONFIG, "", "", 0))
                .bounds(bx, by, 110, 20).build());
        bx += 114;
        widgets.add(Button.builder(Component.literal("Speichern"), b ->
                        PanelClient.sendAction(ServerboundActionPayload.Action.SAVE_DATA, "", "", 0))
                .bounds(bx, by, 90, 20).build());
    }

    @Override
    public void requestRefresh() {
        PanelClient.sendRequest(ServerboundRequestPayload.Kind.CONFIG);
    }

    @Override
    protected void renderContent(GuiGraphics g, Font font, int x, int y, int w, int h,
                                 int mouseX, int mouseY, float partialTick) {
        ClientboundConfigPayload c = PanelState.get().config();
        if (c == null) {
            g.drawString(font, "§7Keine Daten geladen.", x, y, screen.mutedColor(), false);
            return;
        }
        int headerY = y + 28;
        g.fill(x, headerY, x + w, headerY + 12, 0xFF1A2030);
        g.drawString(font, "§lSchlüssel", x + 4, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lWert", x + 280, headerY + 2, screen.textColor(), false);

        int row = headerY + 14;
        int idx = 0;
        for (var e : c.entries()) {
            if (row + 12 > y + h - 28) break;
            int rowBg = (idx % 2 == 0) ? 0xFF202736 : 0xFF1B212D;
            g.fill(x, row, x + w, row + 12, rowBg);
            g.drawString(font, e.key(), x + 4, row + 2, screen.textColor(), false);
            g.drawString(font, e.value(), x + 280, row + 2, screen.mutedColor(), false);
            row += 12;
            idx++;
        }
    }
}
