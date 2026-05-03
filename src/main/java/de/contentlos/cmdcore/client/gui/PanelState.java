package de.contentlos.cmdcore.client.gui;

import de.contentlos.cmdcore.common.network.payload.ClientboundActionResultPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundConfigPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundDebugPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundLogListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundModuleListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundOverviewPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPermissionListPayload;
import de.contentlos.cmdcore.common.network.payload.ClientboundPlayerListPayload;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Singleton mit dem letzten bekannten Stand der vom Server geschickten Daten.
 *
 * <p>Wird vom Admin-Panel-Screen ausgelesen. Die GUI hält selbst keinerlei
 * Zustand der Tabs zwischen den Sessions – beim Schließen/Öffnen werden
 * Daten neu angefordert.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PanelState {

    private static final PanelState INSTANCE = new PanelState();

    public static PanelState get() {
        return INSTANCE;
    }

    private String accentColor = "CYAN";
    private String ownLevel = "USER";
    private String activeTab = "OVERVIEW";

    @Nullable private ClientboundOverviewPayload overview;
    @Nullable private ClientboundPlayerListPayload players;
    @Nullable private ClientboundPermissionListPayload permissions;
    @Nullable private ClientboundLogListPayload logs;
    @Nullable private ClientboundModuleListPayload modules;
    @Nullable private ClientboundConfigPayload config;
    @Nullable private ClientboundDebugPayload debug;
    @Nullable private ClientboundActionResultPayload lastResult;
    private long lastResultAt;

    public String accentColor() { return accentColor; }
    public void setAccentColor(String s) { this.accentColor = s == null ? "CYAN" : s; }

    public String ownLevel() { return ownLevel; }
    public void setOwnLevel(String s) { this.ownLevel = s == null ? "USER" : s; }

    public String activeTab() { return activeTab; }
    public void setActiveTab(String tab) { this.activeTab = tab == null ? "OVERVIEW" : tab; }

    public @Nullable ClientboundOverviewPayload overview() { return overview; }
    public void setOverview(ClientboundOverviewPayload p) { this.overview = p; }

    public @Nullable ClientboundPlayerListPayload players() { return players; }
    public void setPlayers(ClientboundPlayerListPayload p) { this.players = p; }

    public @Nullable ClientboundPermissionListPayload permissions() { return permissions; }
    public void setPermissions(ClientboundPermissionListPayload p) { this.permissions = p; }

    public @Nullable ClientboundLogListPayload logs() { return logs; }
    public void setLogs(ClientboundLogListPayload p) { this.logs = p; }

    public @Nullable ClientboundModuleListPayload modules() { return modules; }
    public void setModules(ClientboundModuleListPayload p) { this.modules = p; }

    public @Nullable ClientboundConfigPayload config() { return config; }
    public void setConfig(ClientboundConfigPayload p) { this.config = p; }

    public @Nullable ClientboundDebugPayload debug() { return debug; }
    public void setDebug(ClientboundDebugPayload p) { this.debug = p; }

    public @Nullable ClientboundActionResultPayload lastResult() { return lastResult; }
    public long lastResultAt() { return lastResultAt; }
    public void setLastResult(ClientboundActionResultPayload p) {
        this.lastResult = p;
        this.lastResultAt = System.currentTimeMillis();
    }

    public int accentArgb() {
        return switch (accentColor.toUpperCase()) {
            case "BLUE" -> 0xFF3B82F6;
            case "VIOLET" -> 0xFFA855F7;
            case "RED" -> 0xFFEF4444;
            case "GREEN" -> 0xFF22C55E;
            case "AMBER" -> 0xFFF59E0B;
            default -> 0xFF22D3EE;
        };
    }
}
