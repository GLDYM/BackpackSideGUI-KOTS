package dev.polaris_light.backpack_side_gui.server;

import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.world.item.ItemStack;

final class PullResult {
    final BackpackAccess backpack;
    final ItemStack stack;

    PullResult(BackpackAccess backpack, ItemStack stack) {
        this.backpack = backpack;
        this.stack = stack;
    }

    public BackpackAccess backpack() {
        return this.backpack;
    }

    public ItemStack stack() {
        return this.stack;
    }
}