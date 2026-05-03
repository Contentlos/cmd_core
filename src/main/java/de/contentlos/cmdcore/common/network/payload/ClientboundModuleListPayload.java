package de.contentlos.cmdcore.common.network.payload;

import de.contentlos.cmdcore.common.network.NWNetwork;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundModuleListPayload(List<Entry> modules) implements CustomPacketPayload {

    public record Entry(String id, String name, String description, boolean enabled,
                        boolean canBeDisabled, String requiredLevel, String version, String error) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Entry decode(ByteBuf buf) {
                return new Entry(
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf)
                );
            }

            @Override
            public void encode(ByteBuf buf, Entry e) {
                ByteBufCodecs.STRING_UTF8.encode(buf, e.id);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.name);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.description);
                ByteBufCodecs.BOOL.encode(buf, e.enabled);
                ByteBufCodecs.BOOL.encode(buf, e.canBeDisabled);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.requiredLevel);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.version);
                ByteBufCodecs.STRING_UTF8.encode(buf, e.error);
            }
        };
    }

    public static final CustomPacketPayload.Type<ClientboundModuleListPayload> TYPE =
            new CustomPacketPayload.Type<>(NWNetwork.id("s2c_module_list"));

    public static final StreamCodec<ByteBuf, ClientboundModuleListPayload> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(ClientboundModuleListPayload::new, ClientboundModuleListPayload::modules);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
