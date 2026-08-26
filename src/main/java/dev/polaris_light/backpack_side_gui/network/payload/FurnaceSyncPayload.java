package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record FurnaceSyncPayload(ItemStack input, ItemStack fuel, ItemStack output,
        long burnFinish, int burnTotal, long cookFinish, int cookTotal, boolean cooking) implements CustomPacketPayload {
    public static final Type<FurnaceSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "furnace_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FurnaceSyncPayload> STREAM_CODEC = StreamCodec
            .of((b, p) -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(b, p.input);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(b, p.fuel);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(b, p.output);
                b.writeVarLong(p.burnFinish);
                b.writeVarInt(p.burnTotal);
                b.writeVarLong(p.cookFinish);
                b.writeVarInt(p.cookTotal);
                b.writeBoolean(p.cooking);
            }, b -> new FurnaceSyncPayload(ItemStack.OPTIONAL_STREAM_CODEC.decode(b), ItemStack.OPTIONAL_STREAM_CODEC.decode(b),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(b), b.readVarLong(), b.readVarInt(), b.readVarLong(),
                    b.readVarInt(), b.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
