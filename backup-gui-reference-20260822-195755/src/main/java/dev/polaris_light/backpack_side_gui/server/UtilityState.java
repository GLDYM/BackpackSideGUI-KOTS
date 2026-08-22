package dev.polaris_light.backpack_side_gui.server;

import net.minecraft.world.item.ItemStack;

final class UtilityState {
    final ItemStack[] crafting = empty(9);
    final ItemStack[] furnace = empty(3);
    final ItemStack[] anvil = empty(2);
    final ItemStack[] smithing = empty(3);
    int litTime = 0;
    int litDuration = 0;
    int cookProgress = 0;
    int cookTotal = 200;
    int anvilCost = 0;
    int anvilMaterialCost = 0;
    String anvilName = "";

    UtilityState() {
    }

    private static ItemStack[] empty(int n) {
        ItemStack[] a = new ItemStack[n];
        for (int i = 0; i < n; i++) {
            a[i] = ItemStack.EMPTY;
        }
        return a;
    }
}