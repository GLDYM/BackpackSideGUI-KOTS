package dev.polaris_light.backpack_side_gui.server;

import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import dev.polaris_light.backpack_side_gui.server.record.UpgradeFlags;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.smithing.SmithingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.ICookingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeItem;

final class FlagResolver {
    private FlagResolver() {
    }

    static UpgradeFlags resolve(BackpackAccess backpack) {
        Flags result = new Flags();
        UpgradeHandler upgrades = BackpackWrapper.fromStack(backpack.stack()).getUpgradeHandler();

        for (int i = 0; i < upgrades.getSlots(); i++) {
            result.add(upgrades.getStackInSlot(i));
        }

        // for (int i = 0; i < backpack.handler().getSlots(); i++) {
        //     result.add(backpack.handler().getStackInSlot(i));
        // }

        return new UpgradeFlags(result.crafting, result.furnace, result.anvil, result.smithing);
    }

    private static final class Flags {
        boolean crafting, furnace, anvil, smithing;

        void add(ItemStack stack) {
            if (stack == null || stack.isEmpty())
                return;
            var item = stack.getItem();
            crafting |= item instanceof CraftingUpgradeItem;
            furnace |= item instanceof ICookingUpgradeItem;
            anvil |= item instanceof AnvilUpgradeItem;
            smithing |= item instanceof SmithingUpgradeItem;
        }
    }
}
