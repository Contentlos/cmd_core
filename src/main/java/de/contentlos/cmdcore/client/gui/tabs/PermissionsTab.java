package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.PanelClient;
import de.contentlos.cmdcore.client.gui.PanelState;
import de.contentlos.cmdcore.common.network.payload.ClientboundPermissionListPayload;
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
public class PermissionsTab extends PanelTab {

    private EditBox nameField;
    private EditBox uuidField;
    private EditBox levelField;

    @Override
    public String title() { return "Berechtigungen"; }

    @Override
    public String subtitle() { return "Stufen verwalten – nur OWNER darf ändern."; }

    @Override
    protected void onInit() {
        var font = Minecraft.getInstance().font;
        int x = screen.contentX();
        int y = screen.contentY() + 26;
        nameField = new EditBox(font, x, y + 4, 130, 20, Component.literal("Name"));
        nameField.setHint(Component.literal("Spielername"));
        widgets.add(nameField);
        uuidField = new EditBox(font, x + 138, y + 4, 240, 20, Component.literal("UUID"));
        uuidField.setHint(Component.literal("UUID"));
        widgets.add(uuidField);
        levelField = new EditBox(font, x + 386, y + 4, 90, 20, Component.literal("Level"));
        levelField.setHint(Component.literal("z.B. ADMIN"));
        widgets.add(levelField);

        int by = screen.contentY() + screen.contentHeight() - 26;
        int bx = x;
        widgets.add(Button.builder(Component.literal("Setzen"), b -> {
            PanelClient.sendUpdatePermission("SET",
                    uuidField.getValue().trim(), nameField.getValue().trim(), levelField.getValue().trim());
            PanelClient.sendRequest(ServerboundRequestPayload.Kind.PERMISSIONS);
        }).bounds(bx, by, 90, 20).build());
        bx += 94;
        widgets.add(Button.builder(Component.literal("Entfernen"), b -> {
            PanelClient.sendUpdatePermission("REMOVE",
                    uuidField.getValue().trim(), nameField.getValue().trim(), "");
            PanelClient.sendRequest(ServerboundRequestPayload.Kind.PERMISSIONS);
        }).bounds(bx, by, 90, 20).build());
        bx += 94;
        widgets.add(Button.builder(Component.literal("Reload"), b -> {
            PanelClient.sendAction(ServerboundActionPayload.Action.RELOAD_PERMS, "", "", 0);
            PanelClient.sendRequest(ServerboundRequestPayload.Kind.PERMISSIONS);
        }).bounds(bx, by, 70, 20).build());
        bx += 74;
        widgets.add(Button.builder(Component.literal("Speichern"), b -> {
            PanelClient.sendRequest(ServerboundRequestPayload.Kind.SAVE_ALL);
        }).bounds(bx, by, 90, 20).build());
        bx += 94;
        widgets.add(Button.builder(Component.literal("OP-Cache"), b -> {
            PanelClient.sendAction(ServerboundActionPayload.Action.REFRESH_OP_CACHE, "", "", 0);
            PanelClient.sendRequest(ServerboundRequestPayload.Kind.PERMISSIONS);
        }).bounds(bx, by, 80, 20).build());
    }

    @Override
    public void requestRefresh() {
        PanelClient.sendRequest(ServerboundRequestPayload.Kind.PERMISSIONS);
    }

    @Override
    protected void renderContent(GuiGraphics g, Font font, int x, int y, int w, int h,
                                 int mouseX, int mouseY, float partialTick) {
        ClientboundPermissionListPayload list = PanelState.get().permissions();
        int headerY = y + 28;
        if (list == null) {
            g.drawString(font, "§7Keine Daten geladen.", x, y, screen.mutedColor(), false);
            return;
        }
        g.drawString(font, "§7OP-Fallback: " + (list.opFallbackEnabled() ? "AN" : "AUS")
                + "    §7Standard: " + list.defaultLevel(), x, y, screen.mutedColor(), false);

        g.fill(x, headerY, x + w, headerY + 12, 0xFF1A2030);
        g.drawString(font, "§lName", x + 4, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lUUID", x + 110, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lLevel", x + 360, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lQuelle", x + 420, headerY + 2, screen.textColor(), false);

        int row = headerY + 14;
        int idx = 0;
        for (var e : list.entries()) {
            if (row + 12 > y + h - 28) break;
            int rowBg = (idx % 2 == 0) ? 0xFF202736 : 0xFF1B212D;
            g.fill(x, row, x + w, row + 12, rowBg);
            g.drawString(font, e.name(), x + 4, row + 2, screen.textColor(), false);
            g.drawString(font, e.uuid(), x + 110, row + 2, screen.mutedColor(), false);
            g.drawString(font, e.level(), x + 360, row + 2, levelColor(e.level()), false);
            g.drawString(font, e.source(), x + 420, row + 2, screen.mutedColor(), false);
            row += 12;
            idx++;
        }
        if (list.entries().isEmpty()) {
            g.drawString(font, "§7Keine Einträge.", x, row + 4, screen.mutedColor(), false);
        }
    }

    private int levelColor(String level) {
        return switch (level == null ? "" : level) {
            case "OWNER" -> 0xFFEF4444;
            case "ADMIN" -> 0xFFF59E0B;
            case "MODERATOR" -> 0xFF22D3EE;
            case "HELPER" -> 0xFF22C55E;
            case "CONSOLE" -> 0xFFA855F7;
            default -> 0xFFA0A8B5;
        };
    }
}
