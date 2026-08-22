package dev.polaris_light.backpack_side_gui.server;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
final class StackLimitItemHandler implements IItemHandlerModifiable {
    final IItemHandler delegate;
    final int stackLimit;

    StackLimitItemHandler(IItemHandler delegate, int stackLimit) {
        this.delegate = delegate;
        this.stackLimit = Math.max(64, stackLimit);
    }

    int stackLimit() {
        return this.stackLimit;
    }

    public int getSlots() {
        return this.delegate.getSlots();
    }

    public ItemStack getStackInSlot(int slot) {
        return this.delegate.getStackInSlot(slot);
    }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        IItemHandlerModifiable iItemHandlerModifiable = this.delegate instanceof IItemHandlerModifiable m ? m : null;
        if (iItemHandlerModifiable instanceof IItemHandlerModifiable) {
            IItemHandlerModifiable modifiable = iItemHandlerModifiable;
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            ItemStack remaining = stack.copy();
            ItemStack current = getStackInSlot(slot).copy();
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, remaining)) {
                return remaining;
            }
            int room = getSlotLimit(slot) - (current.isEmpty() ? 0 : current.getCount());
            int move = Math.min(room, remaining.getCount());
            if (move <= 0) {
                return remaining;
            }
            if (!simulate) {
                if (current.isEmpty()) {
                    modifiable.setStackInSlot(slot, remaining.copyWithCount(move));
                } else {
                    current.grow(move);
                    modifiable.setStackInSlot(slot, current);
                }
            }
            remaining.shrink(move);
            return remaining;
        }
        return this.delegate.insertItem(slot, stack, simulate);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.delegate.extractItem(slot, amount, simulate);
    }

    public int getSlotLimit(int slot) {
        try {
            return Math.max(this.stackLimit, this.delegate.getSlotLimit(slot));
        } catch (Throwable th) {
            return this.stackLimit;
        }
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return this.delegate.isItemValid(slot, stack);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        IItemHandlerModifiable iItemHandlerModifiable = this.delegate instanceof IItemHandlerModifiable m ? m : null;
        if (iItemHandlerModifiable instanceof IItemHandlerModifiable) {
            IItemHandlerModifiable modifiable = iItemHandlerModifiable;
            modifiable.setStackInSlot(slot, stack);
        }
    }
}