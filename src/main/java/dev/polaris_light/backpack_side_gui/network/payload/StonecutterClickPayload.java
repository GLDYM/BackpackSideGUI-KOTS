package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record StonecutterClickPayload(int slot, int button, int recipe, boolean shift, ItemStack carried) implements CustomPacketPayload {
    public static final Type<StonecutterClickPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "stonecutter_click"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StonecutterClickPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> {
                b.writeVarInt(p.slot);
                b.writeVarInt(p.button);
                b.writeVarInt(p.recipe);
                b.writeBoolean(p.shift);
                b.writeBoolean(!p.carried.isEmpty());
                if (!p.carried.isEmpty()) ItemStack.STREAM_CODEC.encode(b, p.carried);
            },
            b -> new StonecutterClickPayload(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean() ? ItemStack.STREAM_CODEC.decode(b) : ItemStack.EMPTY));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
