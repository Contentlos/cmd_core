package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.client.gui.PanelClient;
import de.contentlos.cmdcore.client.gui.PanelState;
import de.contentlos.cmdcore.common.network.payload.ClientboundPlayerListPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundActionPayload;
import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayersTab extends PanelTab {

    private EditBox filterField;
    private EditBox targetField;

    @Override
    public String title() { return "Spieler"; }

    @Override
    public String subtitle() { return "Live-Daten aller Online-Spieler. Ziel oben eingeben."; }

    @Override
    protected void onInit() {
        int x = screen.contentX();
        int y = screen.contentY();
        var font = net.minecraft.client.Minecraft.getInstance().font;
        targetField = new EditBox(font, x, y + 30, 160, 20, Component.literal("Zielspieler"));
        targetField.setHint(Component.literal("Zielspieler-Name"));
        widgets.add(targetField);

        filterField = new EditBox(font, x + 168, y + 30, 160, 20, Component.literal("Filter"));
        filterField.setHint(Component.literal("Filter (Name)"));
        widgets.add(filterField);

        int by = screen.contentY() + screen.contentHeight() - 26;
        int bx = x;
        bx = addBtn(bx, by, "Heilen", () -> sendAction(ServerboundActionPayload.Action.HEAL, ""));
        bx = addBtn(bx, by, "Füttern", () -> sendAction(ServerboundActionPayload.Action.FEED, ""));
        bx = addBtn(bx, by, "Flug", () -> sendAction(ServerboundActionPayload.Action.FLY_TOGGLE, ""));
        bx = addBtn(bx, by, "Tp zu", () -> sendAction(ServerboundActionPayload.Action.TELEPORT_TO_TARGET, ""));
        bx = addBtn(bx, by, "Tp her", () -> sendAction(ServerboundActionPayload.Action.TELEPORT_HERE, ""));
        bx = addBtn(bx, by, "Survival", () -> sendAction(ServerboundActionPayload.Action.SET_GAMEMODE, "survival"));
        bx = addBtn(bx, by, "Creative", () -> sendAction(ServerboundActionPayload.Action.SET_GAMEMODE, "creative"));
        bx = addBtn(bx, by, "Spectator", () -> sendAction(ServerboundActionPayload.Action.SET_GAMEMODE, "spectator"));
        bx = addBtn(bx, by, "Kick", () -> sendAction(ServerboundActionPayload.Action.KICK, "Vom Admin entfernt"));
        bx = addBtn(bx, by, "Info", () -> sendAction(ServerboundActionPayload.Action.SHOW_INFO, ""));
    }

    private int addBtn(int x, int y, String label, Runnable action) {
        int w = Math.max(50, net.minecraft.client.Minecraft.getInstance().font.width(label) + 10);
        widgets.add(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, 20).build());
        return x + w + 4;
    }

    private void sendAction(ServerboundActionPayload.Action action, String arg) {
        String target = targetField == null ? "" : targetField.getValue().trim();
        if (target.isEmpty()) {
            // Kein Auto-Targeting auf den ersten Online-Spieler — gerade bei
            // destruktiven Aktionen (Kick, Gamemode-Change) wäre das gefährlich.
            PanelState.get().setLastResult(
                    new de.contentlos.cmdcore.common.network.payload.ClientboundActionResultPayload(
                            false,
                            "Kein Zielspieler. Bitte oben einen Namen eintragen.",
                            ""));
            return;
        }
        PanelClient.sendAction(action, target, arg, 0);
        PanelClient.sendRequest(ServerboundRequestPayload.Kind.PLAYERS);
    }

    @Override
    public void requestRefresh() {
        PanelClient.sendRequest(ServerboundRequestPayload.Kind.PLAYERS);
    }

    @Override
    protected void renderContent(GuiGraphics g, Font font, int x, int y, int w, int h,
                                 int mouseX, int mouseY, float partialTick) {
        ClientboundPlayerListPayload list = PanelState.get().players();
        if (list == null) {
            g.drawString(font, "§7Keine Daten geladen.", x, y, screen.mutedColor(), false);
            return;
        }
        int headerY = y + 28;
        g.fill(x, headerY, x + w, headerY + 12, 0xFF1A2030);
        g.drawString(font, "§lName", x + 4, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lLevel", x + 110, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lGM", x + 170, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lDim", x + 220, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lHP", x + 320, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lFood", x + 360, headerY + 2, screen.textColor(), false);
        g.drawString(font, "§lPing", x + 410, headerY + 2, screen.textColor(), false);

        String f = filterField == null ? "" : filterField.getValue().trim().toLowerCase();
        int row = headerY + 14;
        int idx = 0;
        for (var p : list.players()) {
            if (!f.isEmpty() && !p.name().toLowerCase().contains(f)) continue;
            if (row + 12 > y + h - 28) break;
            int rowBg = (idx % 2 == 0) ? 0xFF202736 : 0xFF1B212D;
            g.fill(x, row, x + w, row + 12, rowBg);
            g.drawString(font, p.name(), x + 4, row + 2, screen.textColor(), false);
            g.drawString(font, p.level(), x + 110, row + 2, levelColor(p.level()), false);
            g.drawString(font, p.gamemode(), x + 170, row + 2, screen.textColor(), false);
            g.drawString(font, shortDim(p.dimension()), x + 220, row + 2, screen.textColor(), false);
            g.drawString(font, String.format("%.1f", p.health()), x + 320, row + 2, screen.textColor(), false);
            g.drawString(font, String.valueOf(p.food()), x + 360, row + 2, screen.textColor(), false);
            g.drawString(font, p.ping() + "ms", x + 410, row + 2, screen.textColor(), false);
            row += 12;
            idx++;
        }
        if (list.players().isEmpty()) {
            g.drawString(font, "§7Keine Spieler online.", x, row + 4, screen.mutedColor(), false);
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

    private String shortDim(String dim) {
        if (dim == null) return "?";
        int slash = dim.indexOf(':');
        return slash >= 0 ? dim.substring(slash + 1) : dim;
    }
}
