package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.AdminPanelScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.Font;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Schnittstelle eines Tabs im Admin-Panel.
 *
 * <p>Konkrete Tabs verwalten ihre eigenen Widgets in {@link #widgets()}.
 * Der {@link AdminPanelScreen} fügt diese beim Wechsel hinzu/entfernt sie.</p>
 */
@OnlyIn(Dist.CLIENT)
public abstract class PanelTab {

    protected final List<AbstractWidget> widgets = new ArrayList<>();
    protected AdminPanelScreen screen;

    public abstract String title();

    public String subtitle() { return "Echte Daten vom Server."; }

    public List<AbstractWidget> widgets() { return widgets; }

    public void init(AdminPanelScreen screen) {
        this.screen = screen;
        widgets.clear();
        onInit();
    }

    /** Wird beim Aktivieren des Tabs aufgerufen. Kinder fügen Widgets hinzu. */
    protected abstract void onInit();

    /** Wird beim Verlassen des Tabs aufgerufen. */
    public void onHide() {}

    public void tick() {}

    public void render(GuiGraphics g, Font font, int x, int y, int w, int h,
                       int mouseX, int mouseY, float partialTick) {
        // Standardimplementation rendert nur die Widgets via Screen.render.
        renderContent(g, font, x, y, w, h, mouseX, mouseY, partialTick);
    }

    protected void renderContent(GuiGraphics g, Font font, int x, int y, int w, int h,
                                 int mouseX, int mouseY, float partialTick) {
    }

    public void requestRefresh() {}
}
