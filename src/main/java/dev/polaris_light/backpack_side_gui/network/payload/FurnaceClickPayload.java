package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record FurnaceClickPayload(int slot, int button, ItemStack carried) implements CustomPacketPayload {
    public static final Type<FurnaceClickPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "furnace_click"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FurnaceClickPayload> STREAM_CODEC = StreamCodec
            .of((buffer, payload) -> {
                buffer.writeVarInt(payload.slot);
                buffer.writeVarInt(payload.button);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.carried);
            }, buffer -> new FurnaceClickPayload(buffer.readVarInt(), buffer.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
