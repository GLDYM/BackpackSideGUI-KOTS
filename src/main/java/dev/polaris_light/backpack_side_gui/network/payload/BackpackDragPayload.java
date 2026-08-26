package dev.polaris_light.backpack_side_gui.network.payload;

import java.util.List;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record BackpackDragPayload(List<Integer> slots, int button, ItemStack carried) implements CustomPacketPayload {
    public static final Type<BackpackDragPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "backpack_drag"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackDragPayload> STREAM_CODEC = StreamCodec
            .of((b, p) -> {
                int size = Math.min(54, p.slots().size());
                b.writeVarInt(size);
                for (int i = 0; i < size; i++)
                    b.writeVarInt(p.slots().get(i));
                b.writeBoolean(p.button() == 1);
                ItemStack.STREAM_CODEC.encode(b, p.carried());
            }, b -> {
                int size = b.readVarInt();
                var slots = new java.util.ArrayList<Integer>(Math.min(size, 54));
                for (int i = 0; i < size; i++) {
                    int slot = b.readVarInt();
                    if (i < 54)
                        slots.add(slot);
                }
                return new BackpackDragPayload(slots, b.readBoolean() ? 1 : 0, ItemStack.STREAM_CODEC.decode(b));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
