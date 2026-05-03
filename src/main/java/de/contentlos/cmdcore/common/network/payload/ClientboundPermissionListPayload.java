package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundPermissionListPayload(List<Entry> entries, boolean opFallbackEnabled, String defaultLevel)
        implements CustomPacketPayload {

    public record Entry(String name, String uuid, String level, String source, long updatedAt) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::name,
                ByteBufCodecs.STRING_UTF8, Entry::uuid,
                ByteBufCodecs.STRING_UTF8, Entry::level,
                ByteBufCodecs.STRING_UTF8, Entry::source,
                ByteBufCodecs.VAR_LONG, Entry::updatedAt,
                Entry::new
        );
    }

    public static final CustomPacketPayload.Type<ClientboundPermissionListPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("s2c_permission_list"));

    public static final StreamCodec<ByteBuf, ClientboundPermissionListPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ClientboundPermissionListPayload decode(ByteBuf buf) {
            int n = ByteBufCodecs.VAR_INT.decode(buf);
            List<Entry> entries = new java.util.ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                entries.add(Entry.STREAM_CODEC.decode(buf));
            }
            return new ClientboundPermissionListPayload(
                    entries,
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf)
            );
        }

        @Override
        public void encode(ByteBuf buf, ClientboundPermissionListPayload p) {
            ByteBufCodecs.VAR_INT.encode(buf, p.entries.size());
            for (Entry e : p.entries) {
                Entry.STREAM_CODEC.encode(buf, e);
            }
            ByteBufCodecs.BOOL.encode(buf, p.opFallbackEnabled);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.defaultLevel);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
