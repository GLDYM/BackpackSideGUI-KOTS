package dev.polaris_light.backpack_side_gui.network;

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
            .of((b, p) -> {
                b.writeVarInt(p.slots.size());
                for (int i : p.slots)
                    b.writeVarInt(i);
                b.writeBoolean(p.button == 1);
                ItemStack.STREAM_CODEC.encode(b, p.carried);
            }, b -> {
                int n = Math.min(9, b.readVarInt());
                var s = new java.util.ArrayList<Integer>();
                for (int i = 0; i < n; i++)
                    s.add(b.readVarInt());
                return new CraftingDragPayload(s, b.readBoolean() ? 1 : 0, ItemStack.STREAM_CODEC.decode(b));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
