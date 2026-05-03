package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundLogListPayload(String logKind, List<Entry> entries) implements CustomPacketPayload {

    public record Entry(long timestamp, String adminName, String adminLevel, String targetName,
                        String action, String source, boolean success, String details, String module) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Entry decode(ByteBuf buf) {
                return new Entry(
                        ByteBufCodecs.VAR_LONG.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf)
                );
            }

            @Override
            public void encode(ByteBuf buf, Entry e) {
                ByteBufCodecs.VAR_LONG.encode(buf, e.timestamp);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.adminName);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.adminLevel);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.targetName);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.action);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.source);
                ByteBufCodecs.BOOL.encode(buf, e.success);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.details);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.module);
            }
        };
    }

    public static final CustomPacketPayload.Type<ClientboundLogListPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("s2c_log_list"));

    public static final StreamCodec<ByteBuf, ClientboundLogListPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ClientboundLogListPayload decode(ByteBuf buf) {
            String kind = ByteBufCodecs.STRING_UTF8.decode(buf);
            int n = ByteBufCodecs.VAR_INT.decode(buf);
            List<Entry> entries = new java.util.ArrayList<>(n);
            for (int i = 0; i < n; i++) entries.add(Entry.STREAM_CODEC.decode(buf));
            return new ClientboundLogListPayload(kind, entries);
        }

        @Override
        public void encode(ByteBuf buf, ClientboundLogListPayload p) {
            ByteBufCodecs.STRING_UTF8.encode(buf, p.logKind);
            ByteBufCodecs.VAR_INT.encode(buf, p.entries.size());
            for (Entry e : p.entries) Entry.STREAM_CODEC.encode(buf, e);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
