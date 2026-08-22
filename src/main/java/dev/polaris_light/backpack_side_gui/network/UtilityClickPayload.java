package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class UtilityClickPayload implements CustomPacketPayload {
    private final int utilityType;
    private final int slot;
    private final int button;
    private final ItemStack clientCarried;
    public static final CustomPacketPayload.Type<UtilityClickPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "utility_click"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UtilityClickPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, UtilityClickPayload>() {
        public UtilityClickPayload decode(RegistryFriendlyByteBuf buf) {
            return new UtilityClickPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), ItemNetUtil.readStackWithRealCount(buf));
        }

        public void encode(RegistryFriendlyByteBuf buf, UtilityClickPayload payload) {
            buf.writeVarInt(payload.utilityType);
            buf.writeVarInt(payload.slot);
            buf.writeVarInt(payload.button);
            ItemNetUtil.writeStackWithRealCount(buf, payload.clientCarried);
        }
    };

    public UtilityClickPayload(int utilityType, int slot, int button, ItemStack clientCarried) {
        this.utilityType = utilityType;
        this.slot = slot;
        this.button = button;
        this.clientCarried = clientCarried;
    }

    public int utilityType() {
        return this.utilityType;
    }

    public int slot() {
        return this.slot;
    }

    public int button() {
        return this.button;
    }

    public ItemStack clientCarried() {
        return this.clientCarried;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
