package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record FurnaceSyncPayload(ItemStack input, ItemStack fuel, ItemStack output,
        long burnFinish, int burnTotal, long cookFinish, int cookTotal, boolean cooking)
        implements CustomPacketPayload {
    public static final Type<FurnaceSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "furnace_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FurnaceSyncPayload> STREAM_CODEC = StreamCodec
            .of((buffer, payload) -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.input);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.fuel);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.output);
                buffer.writeVarLong(payload.burnFinish);
                buffer.writeVarInt(payload.burnTotal);
                buffer.writeVarLong(payload.cookFinish);
                buffer.writeVarInt(payload.cookTotal);
                buffer.writeBoolean(payload.cooking);
            }, buffer -> new FurnaceSyncPayload(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readVarLong(), buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readVarInt(), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
