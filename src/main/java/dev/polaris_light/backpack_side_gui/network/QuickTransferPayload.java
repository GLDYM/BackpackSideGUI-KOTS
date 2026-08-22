package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class QuickTransferPayload implements CustomPacketPayload {
    private final int side;
    private final int slot;
    public static final int FROM_BACKPACK = 0;
    public static final int FROM_MENU = 1;
    public static final CustomPacketPayload.Type<QuickTransferPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "quick_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuickTransferPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, QuickTransferPayload>() {
        public QuickTransferPayload decode(RegistryFriendlyByteBuf buf) {
            return new QuickTransferPayload(buf.readVarInt(), buf.readVarInt());
        }

        public void encode(RegistryFriendlyByteBuf buf, QuickTransferPayload payload) {
            buf.writeVarInt(payload.side);
            buf.writeVarInt(payload.slot);
        }
    };

    public QuickTransferPayload(int side, int slot) {
        this.side = side;
        this.slot = slot;
    }

    public int side() {
        return this.side;
    }

    public int slot() {
        return this.slot;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
