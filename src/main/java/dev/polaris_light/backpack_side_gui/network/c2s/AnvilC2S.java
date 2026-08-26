package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.List;
import java.util.Optional;
import dev.polaris_light.backpack_side_gui.network.payload.*;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeWrapper;

public final class AnvilC2S {
    private AnvilC2S() {
    }

    private static AnvilUpgradeWrapper wrapper(ServerPlayer player) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty())
            return null;
        List<AnvilUpgradeWrapper> wrappers = BackpackWrapper.fromStack(access.get().stack()).getUpgradeHandler()
                .getWrappersThatImplement(AnvilUpgradeWrapper.class);
        return wrappers.isEmpty() ? null : wrappers.getFirst();
    }

    public static void handleRename(ServerPlayer player, AnvilRenamePayload payload) {
        AnvilUpgradeWrapper wrapper = wrapper(player);
        if (wrapper == null)
            return;
        wrapper.setItemName(payload.name() == null ? ""
                : payload.name().strip().substring(0, Math.min(50, payload.name().strip().length())));
        send(player, wrapper);
    }

    public static void handleClick(ServerPlayer player, AnvilClickPayload payload) {
        AnvilUpgradeWrapper wrapper = wrapper(player);
        if (wrapper == null || payload.slot() < 0 || payload.slot() > 2)
            return;
        IItemHandler inv = wrapper.getInventory();
        ItemStack carried = player.containerMenu.getCarried();
        if (!ItemStack.matches(carried, payload.carried())) {
            if (!player.isCreative())
                return;
            carried = payload.carried().copy();
        }
        if (payload.slot() == 2) {
            Calculation calc = calculate(player, inv, wrapper.getItemName());
            if (!calc.result.isEmpty() && (player.isCreative() || player.experienceLevel >= calc.cost)
                    && carried.isEmpty()) {
                player.containerMenu.setCarried(calc.result);
                inv.extractItem(0, 1, false);
                if (calc.rightConsumed > 0)
                    inv.extractItem(1, Math.min(calc.rightConsumed, inv.getStackInSlot(1).getCount()), false);
                if (!player.isCreative())
                    player.giveExperienceLevels(-calc.cost);
                wrapper.setItemName("");
            }
        } else
            player.containerMenu.setCarried(HandlerSlotClicker.click(inv, payload.slot(), payload.button(), carried));
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()));
        send(player, wrapper);
    }

    public static void send(ServerPlayer player, BackpackAccess access) {
        AnvilUpgradeWrapper wrapper = wrapper(player);
        if (wrapper != null)
            send(player, wrapper);
    }

    private static void send(ServerPlayer player, AnvilUpgradeWrapper wrapper) {
        IItemHandler inv = wrapper.getInventory();
        Calculation calc = calculate(player, inv, wrapper.getItemName());
        PacketDistributor.sendToPlayer(player, new AnvilSyncPayload(inv.getStackInSlot(0).copy(),
                inv.getStackInSlot(1).copy(), calc.result, calc.cost, wrapper.getItemName()));
    }

    private static Calculation calculate(ServerPlayer player, IItemHandler inv, String name) {
        ItemStack left = inv.getStackInSlot(0);
        ItemStack right = inv.getStackInSlot(1);
        if (left.isEmpty())
            return Calculation.EMPTY;
        AnvilMenu menu = new AnvilMenu(0, player.getInventory(),
                ContainerLevelAccess.create(player.level(), player.blockPosition()));
        menu.getSlot(0).set(left.copy());
        menu.getSlot(1).set(right.copy());
        menu.setItemName(name == null ? "" : name);
        ItemStack vanillaResult = menu.getSlot(2).getItem().copy();
        if (!vanillaResult.isEmpty()) {
            // AnvilMenu can leave repairItemCountCost at zero for enchantment
            // combinations even though the second input is consumed on take.
            int consumed = right.isEmpty() ? 0 : Math.max(1, menu.repairItemCountCost);
            return new Calculation(vanillaResult, menu.getCost(), consumed);
        }

        // Fallback for the upgrade's persisted name when vanilla has no change to apply.
        ItemStack result = left.copy();
        int cost = 0;
        if (name != null && !name.isBlank() && !name.equals(left.getHoverName().getString())) {
            result.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            cost++;
        }
        if (!right.isEmpty() && left.isDamageableItem() && left.getItem() == right.getItem()
                && right.isDamageableItem()) {
            int max = left.getMaxDamage(),
                    repaired = Math.min(max, (max - left.getDamageValue()) + (max - right.getDamageValue()) + max / 20);
            result.setDamageValue(max - repaired);
            cost += 2;
        }
        return cost == 0 ? Calculation.EMPTY : new Calculation(result.copyWithCount(1), cost, right.isEmpty() ? 0 : 1);
    }

    private record Calculation(ItemStack result, int cost, int rightConsumed) {
        static final Calculation EMPTY = new Calculation(ItemStack.EMPTY, 0, 0);
    }
}
