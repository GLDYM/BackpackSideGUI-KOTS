package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class UtilityDoubleCollectPayload implements CustomPacketPayload {
    private final int utilityType;
    private final int slot;
    private final ItemStack clientCarried;
    public static final CustomPacketPayload.Type<UtilityDoubleCollectPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "utility_double_collect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UtilityDoubleCollectPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, UtilityDoubleCollectPayload>() {
        public UtilityDoubleCollectPayload decode(RegistryFriendlyByteBuf buf) {
            return new UtilityDoubleCollectPayload(buf.readVarInt(), buf.readVarInt(), ItemNetUtil.readStackWithRealCount(buf));
        }

        public void encode(RegistryFriendlyByteBuf buf, UtilityDoubleCollectPayload payload) {
            buf.writeVarInt(payload.utilityType);
            buf.writeVarInt(payload.slot);
            ItemNetUtil.writeStackWithRealCount(buf, payload.clientCarried);
        }
    };

    public UtilityDoubleCollectPayload(int utilityType, int slot, ItemStack clientCarried) {
        this.utilityType = utilityType;
        this.slot = slot;
        this.clientCarried = clientCarried;
    }

    public int utilityType() {
        return this.utilityType;
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
