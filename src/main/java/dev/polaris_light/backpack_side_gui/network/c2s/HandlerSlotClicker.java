package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.Collection;
import java.util.LinkedHashSet;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Shared primary/secondary click logic for real item handlers. */
public final class HandlerSlotClicker {
    private HandlerSlotClicker() {
    }

    public static ItemStack click(IItemHandler inventory, int slot, int button, ItemStack carried) {
        if (slot < 0 || slot >= inventory.getSlots())
            return carried;
        ItemStack cursor = carried == null ? ItemStack.EMPTY : carried.copy();
        ItemStack current = inventory.getStackInSlot(slot);
        if (cursor.isEmpty()) {
            if (current.isEmpty())
                return ItemStack.EMPTY;
            return inventory.extractItem(slot, button == 1 ? (current.getCount() + 1) / 2 : current.getCount(), false);
        }
        if (!inventory.isItemValid(slot, cursor))
            return cursor;
        int requested = button == 1 ? 1 : cursor.getCount();
        ItemStack rest = inventory.insertItem(slot, cursor.copyWithCount(requested), false);
        cursor.shrink(requested - rest.getCount());
        return cursor.isEmpty() ? ItemStack.EMPTY : cursor;
    }

    /**
     * Distributes the cursor stack across distinct, valid slots. Left drag
     * shares it equally; right drag tries to place one in every selected slot.
     */
    public static ItemStack distribute(IItemHandler inventory, Collection<Integer> selectedSlots, int button,
            ItemStack carried) {
        ItemStack cursor = carried == null ? ItemStack.EMPTY : carried.copy();
        if (cursor.isEmpty() || selectedSlots == null)
            return cursor;
        var slots = new LinkedHashSet<Integer>();
        for (Integer slot : selectedSlots)
            if (slot != null && slot >= 0 && slot < inventory.getSlots())
                slots.add(slot);
        // Exclude slots which cannot accept even one item before calculating
        // the left-drag share, exactly as vanilla quick-craft does.
        slots.removeIf(slot -> !inventory.isItemValid(slot, cursor)
                || inventory.insertItem(slot, cursor.copyWithCount(1), true).getCount() == 1);
        if (slots.isEmpty())
            return cursor;
        int perSlot = button == 1 ? 1 : cursor.getCount() / slots.size();
        if (perSlot <= 0)
            return cursor;
        for (int slot : slots) {
            int requested = Math.min(perSlot, cursor.getCount());
            ItemStack rest = inventory.insertItem(slot, cursor.copyWithCount(requested), false);
            cursor.shrink(requested - rest.getCount());
            if (cursor.isEmpty())
                break;
        }
        return cursor.isEmpty() ? ItemStack.EMPTY : cursor;
    }
}
