package dev.polaris_light.backpack_side_gui.server.record;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public record BackpackAccess(int playerInventorySlot, ItemStack stack, IItemHandler handler, int stackLimit,
        IItemHandler curiosHandler, int curiosSlot) {
    public BackpackAccess(int playerInventorySlot, ItemStack stack, IItemHandler handler, int stackLimit) {
        this(playerInventorySlot, stack, handler, stackLimit, null, -1);
    }
}
