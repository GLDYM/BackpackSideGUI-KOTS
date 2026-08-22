package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class SortPayload implements CustomPacketPayload {
    private final int sortMode;
    public static final CustomPacketPayload.Type<SortPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "sort"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SortPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, SortPayload>() {
        public SortPayload decode(RegistryFriendlyByteBuf buf) {
            return new SortPayload(buf.readVarInt());
        }

        public void encode(RegistryFriendlyByteBuf buf, SortPayload payload) {
            buf.writeVarInt(payload.sortMode);
        }
    };

    public SortPayload(int sortMode) {
        this.sortMode = sortMode;
    }

    public int sortMode() {
        return this.sortMode;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
