package de.contentlos.cmdcore.client.gui.tabs;

import de.contentlos.cmdcore.common.network.payload.ServerboundRequestPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HistoryTab extends LogsTab {

    @Override
    public String title() { return "Verlauf"; }

    @Override
    public String subtitle() { return "Admin-Verlauf (Quelle: COMMAND/PANEL/SYSTEM)."; }

    @Override
    protected ServerboundRequestPayload.Kind requestKind() {
        return ServerboundRequestPayload.Kind.HISTORY;
    }
}
