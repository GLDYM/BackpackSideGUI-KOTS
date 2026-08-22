package dev.polaris_light.backpack_side_gui.server;

import net.minecraft.world.item.ItemStack;

/** Package-private utility state helpers shared by server entry points. */
final class UtilityStateStore {
    private UtilityStateStore() {
    }

    static ItemStack[] inputSlots(UtilityState state, int utilityType) {
        return switch (utilityType) {
            case ServerBackpackAccess.UTILITY_CRAFTING -> state.crafting;
            case ServerBackpackAccess.UTILITY_FURNACE -> state.furnace;
            case ServerBackpackAccess.UTILITY_ANVIL -> state.anvil;
            case ServerBackpackAccess.UTILITY_SMITHING -> state.smithing;
            default -> new ItemStack[0];
        };
    }
}
