package dev.polaris_light.backpack_side_gui.network.payload;

import java.util.ArrayList;
import java.util.List;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative contents used by JEI to calculate transfer availability. */
public record BackpackAvailabilityPayload(List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<BackpackAvailabilityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "backpack_availability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackAvailabilityPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                int count = Math.min(4096, payload.items().size());
                buf.writeVarInt(count);
                for (int i = 0; i < count; i++) {
                    ItemStack stack = payload.items().get(i);
                    ItemStack.STREAM_CODEC.encode(buf, stack.copyWithCount(Math.max(1, stack.getCount())));
                }
            }, buf -> {
                int count = Math.min(4096, buf.readVarInt());
                List<ItemStack> items = new ArrayList<>(count);
                for (int i = 0; i < count; i++)
                    items.add(ItemStack.STREAM_CODEC.decode(buf));
                return new BackpackAvailabilityPayload(items);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
