package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class DoubleCollectPayload implements CustomPacketPayload {
    private final int slot;
    private final ItemStack clientCarried;
    public static final CustomPacketPayload.Type<DoubleCollectPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "double_collect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DoubleCollectPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, DoubleCollectPayload>() {
        public DoubleCollectPayload decode(RegistryFriendlyByteBuf buf) {
            return new DoubleCollectPayload(buf.readVarInt(), ItemNetUtil.readStackWithRealCount(buf));
        }

        public void encode(RegistryFriendlyByteBuf buf, DoubleCollectPayload payload) {
            buf.writeVarInt(payload.slot);
            ItemNetUtil.writeStackWithRealCount(buf, payload.clientCarried);
        }
    };

    public DoubleCollectPayload(int slot, ItemStack clientCarried) {
        this.slot = slot;
        this.clientCarried = clientCarried;
    }

    public int slot() {
        return this.slot;
    }

    public ItemStack clientCarried() {
        return this.clientCarried;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
