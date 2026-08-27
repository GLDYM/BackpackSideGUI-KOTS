package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record BackpackSlotPayload(int slot, int clickType, ItemStack carried) implements CustomPacketPayload {
    public static final Type<BackpackSlotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "backpack_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackSlotPayload> STREAM_CODEC = StreamCodec
            .of((buffer, payload) -> {
                buffer.writeVarInt(payload.slot());
                buffer.writeVarInt(payload.clickType());
                boolean present = !payload.carried().isEmpty();
                buffer.writeBoolean(present);
                if (present)
                    ItemStack.STREAM_CODEC.encode(buffer, payload.carried());
            }, buffer -> new BackpackSlotPayload(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readBoolean() ? ItemStack.STREAM_CODEC.decode(buffer) : ItemStack.EMPTY));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
