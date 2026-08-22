package dev.polaris_light.backpack_side_gui.server;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

final class AnvilCalc {
    final ItemStack result;
    final int cost;
    final int materialCost;
    static final AnvilCalc EMPTY = new AnvilCalc(ItemStack.EMPTY, 0, 0);

    AnvilCalc(ItemStack result, int cost, int materialCost) {
        this.result = result;
        this.cost = cost;
        this.materialCost = materialCost;
    }

    public ItemStack result() {
        return this.result;
    }

    public int cost() {
        return this.cost;
    }

    public int materialCost() {
        return this.materialCost;
    }
}