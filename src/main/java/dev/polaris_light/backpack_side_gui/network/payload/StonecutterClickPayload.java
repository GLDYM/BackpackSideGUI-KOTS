package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record StonecutterClickPayload(int slot, int button, int recipe, boolean shift, ItemStack carried)
        implements CustomPacketPayload {
    public static final Type<StonecutterClickPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "stonecutter_click"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StonecutterClickPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.slot);
                buffer.writeVarInt(payload.button);
                buffer.writeVarInt(payload.recipe);
                buffer.writeBoolean(payload.shift);
                buffer.writeBoolean(!payload.carried.isEmpty());
                if (!payload.carried.isEmpty())
                    ItemStack.STREAM_CODEC.encode(buffer, payload.carried);
            },
            buffer -> new StonecutterClickPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean() ? ItemStack.STREAM_CODEC.decode(buffer) : ItemStack.EMPTY));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
