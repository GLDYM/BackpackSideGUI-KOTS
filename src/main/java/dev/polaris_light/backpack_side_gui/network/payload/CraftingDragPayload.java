package dev.polaris_light.backpack_side_gui.network.payload;

import java.util.List;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record CraftingDragPayload(List<Integer> slots, int button, ItemStack carried) implements CustomPacketPayload {
    public static final Type<CraftingDragPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "crafting_drag"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingDragPayload> STREAM_CODEC = StreamCodec
            .of((buffer, payload) -> {
                buffer.writeVarInt(payload.slots.size());
                for (int i : payload.slots)
                    buffer.writeVarInt(i);
                buffer.writeBoolean(payload.button == 1);
                ItemStack.STREAM_CODEC.encode(buffer, payload.carried);
            }, buffer -> {
                int n = Math.min(9, buffer.readVarInt());
                var s = new java.util.ArrayList<Integer>();
                for (int i = 0; i < n; i++)
                    s.add(buffer.readVarInt());
                return new CraftingDragPayload(s, buffer.readBoolean() ? 1 : 0, ItemStack.STREAM_CODEC.decode(buffer));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
