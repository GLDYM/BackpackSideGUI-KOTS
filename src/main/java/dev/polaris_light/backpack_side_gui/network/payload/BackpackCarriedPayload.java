package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record BackpackCarriedPayload(ItemStack carried) implements CustomPacketPayload {
    public static final Type<BackpackCarriedPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "backpack_carried"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackCarriedPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                boolean present = payload.carried() != null && !payload.carried().isEmpty();
                buffer.writeBoolean(present);
                if (present)
                    ItemStack.STREAM_CODEC.encode(buffer, payload.carried());
            },
            buffer -> new BackpackCarriedPayload(
                    buffer.readBoolean() ? ItemStack.STREAM_CODEC.decode(buffer) : ItemStack.EMPTY));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
