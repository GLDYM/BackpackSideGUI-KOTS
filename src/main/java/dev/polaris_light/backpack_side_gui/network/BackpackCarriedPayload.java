package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record BackpackCarriedPayload(ItemStack carried) implements CustomPacketPayload {
    public static final Type<BackpackCarriedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "backpack_carried"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackCarriedPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { boolean present = p.carried() != null && !p.carried().isEmpty(); b.writeBoolean(present); if (present) ItemStack.STREAM_CODEC.encode(b, p.carried()); },
            b -> new BackpackCarriedPayload(b.readBoolean() ? ItemStack.STREAM_CODEC.decode(b) : ItemStack.EMPTY));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
