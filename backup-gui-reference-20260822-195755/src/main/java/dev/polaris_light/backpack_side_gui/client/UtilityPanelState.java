package dev.polaris_light.backpack_side_gui.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Client-side state shared by utility panel rendering and input handling. */
final class UtilityPanelState {
    boolean hasCraftingUpgrade;
    boolean hasFurnaceUpgrade;
    boolean hasAnvilUpgrade;
    boolean hasSmithingUpgrade;
    int activePanel = -1;
    int syncType = -1;
    final List<ItemStack> items = new ArrayList<>();
    int furnaceLitTime;
    int furnaceLitDuration;
    int furnaceCookProgress;
    int furnaceCookTotal = 200;
    int anvilCost;
    String anvilName = "";
    boolean anvilNameFocused;

    void clearAvailability() {
        hasCraftingUpgrade = false;
        hasFurnaceUpgrade = false;
        hasAnvilUpgrade = false;
        hasSmithingUpgrade = false;
        activePanel = -1;
        items.clear();
    }

    void applySync(int utilityType, List<ItemStack> syncedItems, int litTime, int litDuration,
            int cookProgress, int cookTotal, int cost, String name) {
        syncType = utilityType;
        items.clear();
        items.addAll(syncedItems);
        furnaceLitTime = litTime;
        furnaceLitDuration = litDuration;
        furnaceCookProgress = cookProgress;
        furnaceCookTotal = cookTotal;
        anvilCost = cost;
        anvilName = name == null ? "" : name;
    }
}
