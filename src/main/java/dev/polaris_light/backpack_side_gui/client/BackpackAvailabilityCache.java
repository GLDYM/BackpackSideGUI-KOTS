package dev.polaris_light.backpack_side_gui.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Pure matching logic for recipe ingredient groups against available stacks. */
public final class BackpackAvailabilityCache {
    private BackpackAvailabilityCache() {}

    public static boolean canFill(List<List<ItemStack>> groups, List<ItemStack> available) {
        if (groups == null || groups.isEmpty() || available == null) return false;
        List<ItemStack> items = new ArrayList<>();
        available.forEach(s -> { if (s != null && !s.isEmpty()) items.add(s.copy()); });
        for (List<ItemStack> options : groups) {
            if (options == null || options.isEmpty()) continue;
            int found = -1;
            for (int i = 0; i < items.size() && found < 0; i++) {
                for (ItemStack option : options) {
                    if (option != null && !option.isEmpty() && ItemStack.isSameItemSameComponents(items.get(i), option)
                            && items.get(i).getCount() >= Math.max(1, option.getCount())) { found = i; break; }
                }
            }
            if (found < 0) return false;
            items.get(found).shrink(1);
        }
        return true;
    }
}
