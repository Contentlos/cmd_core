package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.PanelClient;
import de.contentlos.cmdcore.client.gui.PanelState;
import de.contentlos.cmdcore.common.network.payload.ClientboundLogListPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LogsTab extends PanelTab {

    protected static final SimpleDateFormat FMT = new SimpleDateFormat("HH:mm:ss");

    protected EditBox filterField;
    protected int currentLimit = 50;

    @Override
    public String title() { return "Logs"; }

    @Override
    public String subtitle() { return "Audit-Log mit Filter und Pagination."; }

    @Override
    protected void onInit() {
        var font = Minecraft.getInstance().font;
        int x = screen.contentX();
        int y = screen.contentY() + 26;
        filterField = new EditBox(font, x, y + 4, 200, 20, Component.literal("Filter"));
        filterField.setHint(Component.literal("Filter (Name/Aktion)"));
        widgets.add(filterField);
        widgets.add(Button.builder(Component.literal("Suchen"), b -> requestRefresh())
                .bounds(x + 208, y + 4, 70, 20).build());

        int by = screen.contentY() + screen.contentHeight() - 26;
        int bx = x;
        widgets.add(Button.builder(Component.literal("50"), b -> { currentLimit = 50; requestRefresh(); })
                .bounds(bx, by, 40, 20).build());
        bx += 44;
        widgets.add(Button.builder(Component.literal("100"), b -> { currentLimit = 100; requestRefresh(); })
                .bounds(bx, by, 40, 20).build());
        bx += 44;
        widgets.add(Button.builder(Component.literal("250"), b -> { currentLimit = 250; requestRefresh(); })
                .bounds(bx, by, 40, 20).build());
    }

    @Override
    public void requestRefresh() {
        String filter = filterField == null ? "" : filterField.getValue().trim();
        PanelClient.sendRequest(requestKind(), filter, currentLimit);
    }

    protected ServerboundRequestPayload.Kind requestKind() {
        return ServerboundRequestPayload.Kind.LOGS;
    }

    @Override
    protected void renderContent(GuiGraphics g, Font font, int x, int y, int w, int h,
                                 int mouseX, int mouseY, float partialTick) {
        ClientboundLogListPayload list = PanelState.get().logs();
        if (list == null) {
            g.drawString(font, "§7Keine Daten geladen.", x, y, screen.mutedColor(), false);
            return;
        }
        int headerY = y + 28;
        g.fill(x, headerY, x + w, headerY + 12, 0xFF1A2030);
        g.drawString(font, "§lZeit", x + 4, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lAdmin", x + 70, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lAktion", x + 180, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lZiel", x + 320, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lQuelle", x + 410, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lOK", x + 470, headerY + 2, screen.textColor(), false);

        int row = headerY + 14;
        int idx = 0;
        for (var e : list.entries()) {
            if (row + 12 > y + h - 28) break;
            int rowBg = (idx % 2 == 0) ? 0xFF202736 : 0xFF1B212D;
            g.fill(x, row, x + w, row + 12, rowBg);
            g.drawString(font, FMT.format(new Date(e.timestamp())), x + 4, row + 2, screen.mutedColor(), false);
            g.drawString(font, e.adminName(), x + 70, row + 2, screen.textColor(), false);
            g.drawString(font, e.action(), x + 180, row + 2, screen.textColor(), false);
            g.drawString(font, e.targetName().isEmpty() ? "-" : e.targetName(), x + 320, row + 2, screen.mutedColor(), false);
            g.drawString(font, e.source(), x + 410, row + 2, screen.mutedColor(), false);
            g.drawString(font, e.success() ? "§a✓" : "§c✗", x + 470, row + 2, screen.textColor(), false);
            row += 12;
            idx++;
        }
        if (list.entries().isEmpty()) {
            g.drawString(font, "§7Keine Einträge.", x, row + 4, screen.mutedColor(), false);
        }
    }
}
