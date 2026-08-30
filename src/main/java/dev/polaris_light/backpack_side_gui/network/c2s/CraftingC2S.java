package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingDragPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.JeiBackpackFillPayload;
import dev.polaris_light.backpack_side_gui.network.payload.JeiCraftingFillPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;

public final class CraftingC2S {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CraftingC2S() {
    }

    private static IItemHandler inv(ServerPlayer player) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty())
            return null;
        List<CraftingUpgradeWrapper> wrappers = BackpackWrapper
                .fromStack(access.get().stack())
                .getUpgradeHandler().getWrappersThatImplement(CraftingUpgradeWrapper.class);
        return wrappers.isEmpty() ? null : wrappers.get(0).getInventory();
    }

    public static void handleClick(ServerPlayer player, CraftingClickPayload payload) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty() || payload.slot() < 0 || payload.slot() > 9)
            return;
        IItemHandler inventory = inv(player);
        if (inventory == null)
            return;
        ItemStack carried = player.containerMenu.getCarried();
        if (!ItemStack.matches(carried, payload.carried())) {
            if (!player.gameMode.isCreative())
                return;
            carried = payload.carried().copy();
            player.containerMenu.setCarried(carried);
        }
        if (payload.button() == 6 && payload.slot() < 9) {
            player.containerMenu.setCarried(
                    HandlerSlotClicker.collect(inventory, payload.slot(), 9, carried));
        } else if (payload.slot() == 9) {
            ItemStack result = result(player, inventory);
            if (!result.isEmpty()) {
                if (payload.shift()) {
                    while (!result.isEmpty() && canInsert(player, result)) {
                        ItemStack crafted = result.copy();
                        if (!consume(player, inventory))
                            break;
                        if (!player.getInventory().add(crafted))
                            player.drop(crafted, false);
                        result = result(player, inventory);
                    }
                } else if (carried.isEmpty()) {
                    if (consume(player, inventory))
                        player.containerMenu.setCarried(result.copy());
                } else if (ItemStack.isSameItemSameComponents(carried, result)
                        && carried.getCount() + result.getCount() <= carried.getMaxStackSize()) {
                    if (consume(player, inventory)) {
                        carried.grow(result.getCount());
                        player.containerMenu.setCarried(carried);
                    }
                }
            }
        } else
            player.containerMenu
                    .setCarried(HandlerSlotClicker.click(inventory, payload.slot(), payload.button(), carried));
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()));
        send(player, access.get());
    }

    public static void handleDrag(ServerPlayer player, CraftingDragPayload payload) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty() || payload.slots().size() < 2)
            return;
        IItemHandler inventory = inv(player);
        if (inventory == null)
            return;
        ItemStack carried = player.containerMenu.getCarried();
        if (!ItemStack.matches(carried, payload.carried()) || carried.isEmpty())
            return;
        carried = HandlerSlotClicker.distribute(inventory, payload.slots(), payload.button(), carried);
        player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(carried.copy()));
        send(player, access.get());
    }

    public static void handleJeiFill(ServerPlayer player, JeiCraftingFillPayload payload) {
        LOGGER.info("JEI crafting fill player={} slots={} max={}", player.getGameProfile().getName(),
                payload.ingredients().size(), payload.maxTransfer());
        IItemHandler inventory = inv(player);
        if (inventory == null || payload.ingredients() == null)
            return;
        int limit = Math.min(9, payload.ingredients().size());
        for (int slot = 0; slot < limit; slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty())
                continue;
            for (ItemStack option : payload.ingredients().get(slot)) {
                if (option == null || option.isEmpty())
                    continue;
                int amount = payload.maxTransfer() ? Math.min(option.getMaxStackSize(), availableCount(player, option)) : 1;
                if (amount <= 0 || !inventory.insertItem(slot, option.copyWithCount(amount), true).isEmpty())
                    continue;
                ItemStack found = findAndExtract(player, option, amount);
                LOGGER.info("JEI crafting slot={} wanted={} found={}", slot, option, found);
                if (!found.isEmpty()) {
                    ItemStack rest = inventory.insertItem(slot, found, false);
                    if (!rest.isEmpty())
                        returnToBackpacks(player, rest);
                    break;
                }
            }
        }
        player.containerMenu.broadcastChanges();
        send(player, BackpackResolver.resolve(player).orElse(null));
    }

    public static void handleBackpackFill(ServerPlayer player, JeiBackpackFillPayload payload) {
        if (payload.ingredients() == null)
            return;
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (List<ItemStack> options : payload.ingredients()) {
            int target = -1;
            for (int i = 0; i < player.containerMenu.slots.size(); i++) {
                var slot = player.containerMenu.getSlot(i);
                if (!used.contains(i) && slot.getItem().isEmpty() && slot.container != player.getInventory()
                        && options.stream()
                                .anyMatch(option -> option != null && !option.isEmpty() && slot.mayPlace(option))) {
                    target = i;
                    break;
                }
            }
            if (target < 0)
                continue;
            for (ItemStack wanted : options) {
                if (wanted == null || wanted.isEmpty())
                    continue;
                int amount = payload.maxTransfer() ? wanted.getMaxStackSize() : 1;
                ItemStack found = findAndExtractFromBackpacks(player, wanted, amount);
                if (found.isEmpty())
                    continue;
                used.add(target);
                ItemStack rest = player.containerMenu.getSlot(target).safeInsert(found);
                if (!rest.isEmpty())
                    returnToBackpacks(player, rest);
                break;
            }
        }
        player.containerMenu.broadcastChanges();
    }

    private static ItemStack findAndExtractFromBackpacks(ServerPlayer player, ItemStack wanted, int maxAmount) {
        ItemStack result = ItemStack.EMPTY;
        int remaining = Math.max(0, maxAmount);
        for (BackpackAccess access : BackpackResolver.getAllBackpacks(player))
            for (int i = 0; i < access.handler().getSlots() && remaining > 0; i++) {
                ItemStack stack = access.handler().getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(stack, wanted))
                    {
                        ItemStack taken = access.handler().extractItem(i, Math.min(stack.getCount(), remaining), false);
                        if (!taken.isEmpty()) {
                            result = result.isEmpty() ? taken.copy() : result.copyWithCount(result.getCount() + taken.getCount());
                            remaining -= taken.getCount();
                        }
                    }
        }
        return result;
    }

    private static void returnToBackpacks(ServerPlayer player, ItemStack stack) {
        ItemStack r = stack;
        for (BackpackAccess access : BackpackResolver.getAllBackpacks(player))
            for (int i = 0; i < access.handler().getSlots() && !r.isEmpty(); i++)
                r = access.handler().insertItem(i, r, false);
        if (!r.isEmpty())
            if (!player.getInventory().add(r))
                player.drop(r, false);
    }

    private static ItemStack findAndExtract(ServerPlayer player, ItemStack wanted, int maxAmount) {
        ItemStack result = ItemStack.EMPTY;
        int remaining = Math.max(0, maxAmount);
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (remaining > 0 && ItemStack.isSameItemSameComponents(stack, wanted)) {
                int amount = Math.min(stack.getCount(), remaining);
                ItemStack out = stack.copyWithCount(amount);
                stack.shrink(amount);
                result = result.isEmpty() ? out : result.copyWithCount(result.getCount() + out.getCount());
                remaining -= amount;
            }
        }
        for (BackpackAccess access : BackpackResolver.getAllBackpacks(player)) {
            IItemHandler handler = access.handler();
            for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                    ItemStack out = handler.extractItem(i, Math.min(stack.getCount(), remaining), false);
                    if (!out.isEmpty()) {
                        result = result.isEmpty() ? out.copy() : result.copyWithCount(result.getCount() + out.getCount());
                        remaining -= out.getCount();
                    }
                }
            }
        }
        return result;
    }

    private static int availableCount(ServerPlayer player, ItemStack wanted) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items)
            if (ItemStack.isSameItemSameComponents(stack, wanted))
                count += stack.getCount();
        for (BackpackAccess access : BackpackResolver.getAllBackpacks(player))
            for (int i = 0; i < access.handler().getSlots(); i++) {
                ItemStack stack = access.handler().getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(stack, wanted))
                    count += stack.getCount();
            }
        return count;
    }

    private static ItemStack result(ServerPlayer player, IItemHandler itemHandler) {
        ItemStack[] x = new ItemStack[9];
        for (int n = 0; n < 9; n++)
            x[n] = itemHandler.getStackInSlot(n).copy();
        CraftingInput input = CraftingInput.of(3, 3, Arrays.asList(x));
        return player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .map(recipeHolder -> recipeHolder.value().assemble(input, player.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static boolean consume(ServerPlayer player, IItemHandler itemHandler) {
        ItemStack[] x = new ItemStack[9];
        for (int n = 0; n < 9; n++)
            x[n] = itemHandler.getStackInSlot(n).copy();
        CraftingInput input = CraftingInput.of(3, 3, Arrays.asList(x));
        List<ItemStack> remaining = player.level().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .map(RecipeHolder::value)
                .map(recipe -> recipe.getRemainingItems(input))
                .orElse(NonNullList.withSize(9, ItemStack.EMPTY));
        // Validate every extraction before mutating any slot. This keeps a
        // partially failing third-party handler from consuming only part of a
        // recipe while no result is delivered.
        for (int n = 0; n < 9; n++) {
            if (!x[n].isEmpty() && itemHandler.extractItem(n, 1, true).getCount() != 1)
                return false;
        }
        for (int n = 0; n < 9; n++) {
            if (!itemHandler.getStackInSlot(n).isEmpty()) {
                ItemStack extracted = itemHandler.extractItem(n, 1, false);
                if (extracted.isEmpty())
                    return false;
            }
            ItemStack rest = remaining.get(n);
            if (!rest.isEmpty()) {
                ItemStack rejected = itemHandler.insertItem(n, rest.copy(), false);
                if (!rejected.isEmpty())
                    returnToBackpacks(player, rejected);
            }
        }
        return true;
    }

    private static boolean canInsert(ServerPlayer player, ItemStack stack) {
        int c = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty())
                c += stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(s, stack))
                c += Math.max(0, s.getMaxStackSize() - s.getCount());
            if (c >= stack.getCount())
                return true;
        }
        return false;
    }

    public static void send(ServerPlayer player, BackpackAccess access) {
        IItemHandler inventory = inv(player);
        if (inventory == null)
            return;
        ItemStack[] x = new ItemStack[10];
        for (int n = 0; n < 9; n++)
            x[n] = inventory.getStackInSlot(n).copy();
        x[9] = result(player, inventory);
        PacketDistributor.sendToPlayer(player, new CraftingSyncPayload(x));
    }
}

