package de.contentlos.cmdcore.client.gui;

import com.google.common.collect.ImmutableList;
import de.contentlos.cmdcore.client.gui.tabs.ActionsTab;
import de.contentlos.cmdcore.client.gui.tabs.ConfigTab;
import de.contentlos.cmdcore.client.gui.tabs.DebugTab;
import de.contentlos.cmdcore.client.gui.tabs.HistoryTab;
import de.contentlos.cmdcore.client.gui.tabs.LogsTab;
import de.contentlos.cmdcore.client.gui.tabs.ModulesTab;
import de.contentlos.cmdcore.client.gui.tabs.OverviewTab;
import de.contentlos.cmdcore.client.gui.tabs.PanelTab;
import de.contentlos.cmdcore.client.gui.tabs.PermissionsTab;
import de.contentlos.cmdcore.client.gui.tabs.PlayersTab;
import de.contentlos.cmdcore.client.gui.tabs.ServerTab;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Hauptbildschirm des Admin-Panels.
 *
 * <p>Layout: Linke Sidebar mit Tab-Buttons, Hauptbereich mit dem aktiven Tab.
 * Daten werden vom {@link PanelState}-Singleton bezogen und beim Tabwechsel
 * vom Server nachgefragt.</p>
 */
@OnlyIn(Dist.CLIENT)
public class AdminPanelScreen extends Screen {

    public static final int SIDEBAR_WIDTH = 160;
    public static final int HEADER_HEIGHT = 32;
    public static final int FOOTER_HEIGHT = 28;

    private static final int BG_COLOR = 0xF01A1F2A;
    private static final int SIDEBAR_COLOR = 0xFF131822;
    private static final int CARD_COLOR = 0xFF202736;
    private static final int CARD_BORDER = 0xFF2D3548;
    private static final int TEXT_COLOR = 0xFFE6EDF3;
    private static final int MUTED_COLOR = 0xFF8B95A8;

    private final Map<String, PanelTab> tabs = new LinkedHashMap<>();
    private PanelTab activeTab;

    public AdminPanelScreen() {
        super(Component.literal("NightWatch Admin Panel"));
        tabs.put("OVERVIEW", new OverviewTab());
        tabs.put("PLAYERS", new PlayersTab());
        tabs.put("PERMISSIONS", new PermissionsTab());
        tabs.put("ACTIONS", new ActionsTab());
        tabs.put("SERVER", new ServerTab());
        tabs.put("MODULES", new ModulesTab());
        tabs.put("LOGS", new LogsTab());
        tabs.put("HISTORY", new HistoryTab());
        tabs.put("SETTINGS", new ConfigTab());
        tabs.put("DEBUG", new DebugTab());
    }

    @Override
    protected void init() {
        super.init();
        // Sidebar
        int y = HEADER_HEIGHT + 8;
        for (Map.Entry<String, PanelTab> entry : tabs.entrySet()) {
            String id = entry.getKey();
            PanelTab tab = entry.getValue();
            Button button = Button.builder(Component.literal(tab.title()), b -> selectTab(id))
                    .bounds(8, y, SIDEBAR_WIDTH - 16, 22)
                    .build();
            addRenderableWidget(button);
            y += 26;
        }
        // Footer-Button: Schließen
        addRenderableWidget(Button.builder(Component.literal("Schließen"), b -> onClose())
                .bounds(this.width - 88, this.height - FOOTER_HEIGHT + 4, 80, 20).build());
        // Footer-Button: Aktualisieren
        addRenderableWidget(Button.builder(Component.literal("Aktualisieren"), b -> {
            if (activeTab != null) activeTab.requestRefresh();
        }).bounds(this.width - 192, this.height - FOOTER_HEIGHT + 4, 100, 20).build());

        String state = PanelState.get().activeTab();
        if (!tabs.containsKey(state)) {
            state = "OVERVIEW";
        }
        selectTab(state);
    }

    public void selectTab(String id) {
        PanelTab next = tabs.get(id);
        if (next == null) return;
        // alte Widgets des Tabs entfernen
        if (activeTab != null) {
            for (var w : ImmutableList.copyOf(activeTab.widgets())) {
                this.removeWidget(w);
            }
            activeTab.onHide();
        }
        activeTab = next;
        PanelState.get().setActiveTab(id);
        activeTab.init(this);
        for (var w : activeTab.widgets()) {
            addRenderableWidget(w);
        }
        activeTab.requestRefresh();
    }

    public int contentX() { return SIDEBAR_WIDTH + 12; }
    public int contentY() { return HEADER_HEIGHT + 8; }
    public int contentWidth() { return this.width - contentX() - 12; }
    public int contentHeight() { return this.height - contentY() - FOOTER_HEIGHT - 4; }

    public int textColor() { return TEXT_COLOR; }
    public int mutedColor() { return MUTED_COLOR; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Hintergrund
        graphics.fill(0, 0, this.width, this.height, BG_COLOR);
        // Sidebar
        graphics.fill(0, 0, SIDEBAR_WIDTH, this.height, SIDEBAR_COLOR);
        // Header
        int accent = PanelState.get().accentArgb();
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, 0xFF0F141C);
        graphics.fill(0, HEADER_HEIGHT, this.width, HEADER_HEIGHT + 1, accent);
        graphics.drawString(this.font, "§lNightWatch Core", 12, 8, accent, false);
        graphics.drawString(this.font, "§7Admin-Panel · Stufe: " + PanelState.get().ownLevel(),
                12, 18, MUTED_COLOR, false);
        // Footer
        graphics.fill(0, this.height - FOOTER_HEIGHT, this.width, this.height, 0xFF0F141C);
        graphics.fill(0, this.height - FOOTER_HEIGHT, this.width, this.height - FOOTER_HEIGHT + 1, accent);

        // Aktiver Tab
        if (activeTab != null) {
            // Card Hintergrund
            int x = contentX();
            int y = contentY();
            int w = contentWidth();
            int h = contentHeight();
            graphics.fill(x - 4, y - 4, x + w + 4, y + h + 4, CARD_BORDER);
            graphics.fill(x - 3, y - 3, x + w + 3, y + h + 3, CARD_COLOR);
            graphics.drawString(this.font, "§l" + activeTab.title(), x, y, accent, false);
            graphics.drawString(this.font, activeTab.subtitle(), x, y + 12, MUTED_COLOR, false);
            activeTab.render(graphics, this.font, x, y + 26, w, h - 26, mouseX, mouseY, partialTick);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        // Letzte Aktion-Toast
        var last = PanelState.get().lastResult();
        long age = System.currentTimeMillis() - PanelState.get().lastResultAt();
        if (last != null && age < 4000) {
            int color = last.success() ? 0xFF22C55E : 0xFFEF4444;
            String label = (last.success() ? "OK · " : "Fehler · ") + last.message();
            int tx = this.width - 12 - this.font.width(label);
            int ty = this.height - FOOTER_HEIGHT - 18;
            graphics.fill(tx - 6, ty - 4, tx + this.font.width(label) + 6, ty + 10, 0xFF131822);
            graphics.fill(tx - 6, ty - 4, tx - 4, ty + 10, color);
            graphics.drawString(this.font, label, tx, ty, color, false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (activeTab != null) {
            activeTab.tick();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (activeTab != null) activeTab.onHide();
        super.onClose();
    }

    /**
     * Bequemer Helper: fragt die Daten für einen bestimmten Tab nach.
     */
    public void requestData(ServerboundRequestPayload.Kind kind) {
        PanelClient.sendRequest(kind);
    }
}
