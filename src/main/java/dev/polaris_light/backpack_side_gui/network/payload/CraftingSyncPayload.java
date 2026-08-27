package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record CraftingSyncPayload(ItemStack[] items) implements CustomPacketPayload {
    public static final Type<CraftingSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "crafting_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingSyncPayload> STREAM_CODEC = StreamCodec
            .of((buffer, payload) -> {
                for (int i = 0; i < 10; i++) {
                    ItemStack s = payload.items[i];
                    buffer.writeBoolean(!s.isEmpty());
                    if (!s.isEmpty())
                        ItemStack.STREAM_CODEC.encode(buffer, s);
                }
            }, buffer -> {
                ItemStack[] a = new ItemStack[10];
                for (int i = 0; i < 10; i++)
                    a[i] = buffer.readBoolean() ? ItemStack.STREAM_CODEC.decode(buffer) : ItemStack.EMPTY;
                return new CraftingSyncPayload(a);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
