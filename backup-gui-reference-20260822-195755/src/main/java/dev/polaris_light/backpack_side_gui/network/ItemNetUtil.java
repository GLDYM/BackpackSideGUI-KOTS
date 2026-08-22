package dev.polaris_light.backpack_side_gui.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class ItemNetUtil {
    private static final int MAX_NETWORK_STACK_COUNT = 1000000;

    private ItemNetUtil() {
    }

    public static void writeStackWithRealCount(RegistryFriendlyByteBuf buf, ItemStack stack) {
        ItemStack safe = stack == null ? ItemStack.EMPTY : stack.copy();
        int realCount = safe.isEmpty() ? 0 : Math.max(0, Math.min(MAX_NETWORK_STACK_COUNT, safe.getCount()));
        if (!safe.isEmpty()) {
            safe.setCount(Math.max(1, Math.min(realCount, safe.getMaxStackSize())));
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, safe);
        buf.writeVarInt(realCount);
    }

    public static ItemStack readStackWithRealCount(RegistryFriendlyByteBuf buf) {
        ItemStack stack = (ItemStack) ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int realCount = Math.max(0, Math.min(MAX_NETWORK_STACK_COUNT, buf.readVarInt()));
        if (!stack.isEmpty() && realCount > 0) {
            stack.setCount(realCount);
        }
        return stack;
    }
}
