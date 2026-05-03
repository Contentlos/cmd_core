package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server fordert Client auf, das Admin-Panel zu öffnen.
 * {@code defaultTab} ist optional und enthält den initialen Tab.
 * {@code accentColor} = z.B. "CYAN".
 * {@code playerLevel} ist die Stufe des Empfängers (z.B. "ADMIN") –
 * der Client nutzt das nur für UI, die echte Prüfung passiert serverseitig.
 */
public record ClientboundOpenPanelPayload(String defaultTab, String accentColor, String playerLevel) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundOpenPanelPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("s2c_open_panel"));

    public static final StreamCodec<ByteBuf, ClientboundOpenPanelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClientboundOpenPanelPayload::defaultTab,
            ByteBufCodecs.STRING_UTF8, ClientboundOpenPanelPayload::accentColor,
            ByteBufCodecs.STRING_UTF8, ClientboundOpenPanelPayload::playerLevel,
            ClientboundOpenPanelPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
