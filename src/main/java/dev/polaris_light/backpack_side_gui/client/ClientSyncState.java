package dev.polaris_light.backpack_side_gui.client;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Client-side cache for server synchronized backpack availability. */
public final class ClientSyncState {
    private List<ItemStack> backpackItems = List.of();
    private boolean availabilitySynced;

    public List<ItemStack> backpackItems() {
        return backpackItems;
    }

    public boolean availabilitySynced() {
        return availabilitySynced;
    }

    public void updateAvailability(List<ItemStack> items) {
        backpackItems = items == null ? List.of() : items.stream().map(ItemStack::copy).toList();
        availabilitySynced = true;
    }

    public void invalidate() {
        backpackItems = List.of();
        availabilitySynced = false;
    }
}
