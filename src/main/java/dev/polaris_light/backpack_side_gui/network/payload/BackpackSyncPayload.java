package dev.polaris_light.backpack_side_gui.network.payload;

import java.util.ArrayList;
import java.util.List;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record BackpackSyncPayload(String title, List<ItemStack> items, List<Integer> slotLimits,
        List<Boolean> infiniteSlots) implements CustomPacketPayload {
    public BackpackSyncPayload(String title, List<ItemStack> items) {
        this(title, items, java.util.List.of(), java.util.List.of());
    }

    public BackpackSyncPayload(String title, List<ItemStack> items, List<Integer> slotLimits) {
        this(title, items, slotLimits, java.util.List.of());
    }

    public static final Type<BackpackSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "backpack_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        public BackpackSyncPayload decode(RegistryFriendlyByteBuf buf) {
            String title = buf.readUtf(128);
            int count = Math.min(162, buf.readVarInt());
            List<ItemStack> items = new ArrayList<>(count);
            List<Integer> limits = new ArrayList<>(count);
            List<Boolean> infinite = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                if (buf.readBoolean()) {
                    ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
                    stack.setCount(Math.max(1, buf.readVarInt()));
                    items.add(stack);
                    limits.add(buf.readVarInt());
                    infinite.add(buf.readBoolean());
                } else {
                    items.add(ItemStack.EMPTY);
                    limits.add(64);
                    infinite.add(false);
                }
            }
            return new BackpackSyncPayload(title, items, limits, infinite);
        }

        public void encode(RegistryFriendlyByteBuf buf, BackpackSyncPayload payload) {
            buf.writeUtf(payload.title(), 128);
            buf.writeVarInt(Math.min(162, payload.items().size()));
            for (int i = 0; i < Math.min(162, payload.items().size()); i++) {
                ItemStack stack = payload.items().get(i);
                boolean present = stack != null && !stack.isEmpty();
                buf.writeBoolean(present);
                if (present) {
                    ItemStack encoded = stack.copy();
                    encoded.setCount(1);
                    ItemStack.STREAM_CODEC.encode(buf, encoded);
                    buf.writeVarInt(Math.max(1, stack.getCount()));
                    buf.writeVarInt(i < payload.slotLimits().size() ? payload.slotLimits().get(i) : 64);
                    buf.writeBoolean(i < payload.infiniteSlots().size()
                            && Boolean.TRUE.equals(payload.infiniteSlots().get(i)));
                }
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
