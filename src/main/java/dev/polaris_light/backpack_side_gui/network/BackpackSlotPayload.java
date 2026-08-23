package dev.polaris_light.backpack_side_gui.network;

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
            .of((b, p) -> {
                b.writeVarInt(p.slot());
                b.writeVarInt(p.clickType());
                boolean present = !p.carried().isEmpty();
                b.writeBoolean(present);
                if (present)
                    ItemStack.STREAM_CODEC.encode(b, p.carried());
            }, b -> new BackpackSlotPayload(b.readVarInt(), b.readVarInt(),
                    b.readBoolean() ? ItemStack.STREAM_CODEC.decode(b) : ItemStack.EMPTY));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
