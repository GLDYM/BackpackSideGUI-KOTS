package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.List;
import java.util.Optional;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.StonecutterClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.StonecutterSyncPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

public final class StonecutterC2S {
    private StonecutterC2S() {
    }

    private static StonecutterUpgradeItem.Wrapper wrapper(ServerPlayer player, BackpackAccess access) {
        var wrappers = BackpackWrapper.fromStack(access.stack()).getUpgradeHandler()
                .getWrappersThatImplement(StonecutterUpgradeItem.Wrapper.class);
        return wrappers.isEmpty() ? null : wrappers.get(0);
    }

    public static void handleClick(ServerPlayer player, StonecutterClickPayload payload) {
        var access = BackpackResolver.resolve(player);
        if (access.isEmpty())
            return;
        var upgradeWrapper = wrapper(player, access.get());
        if (upgradeWrapper == null)
            return;
        IItemHandlerModifiable inventory = upgradeWrapper.getInputInventory();
        ItemStack carried = player.containerMenu.getCarried();
        if (!ItemStack.matches(carried, payload.carried())) {
            if (!player.gameMode.isCreative())
                return;
            carried = payload.carried().copy();
            player.containerMenu.setCarried(carried);
        }
        if (payload.slot() == 1) {
            var rs = recipes(player, inventory);
            if (payload.recipe() >= 0 && payload.recipe() < rs.size())
                upgradeWrapper.setRecipeId(rs.get(payload.recipe()).id());
        } else if (payload.slot() == 0) {
            player.containerMenu.setCarried(payload.button() == 6 ? collect(inventory, carried)
                    : HandlerSlotClicker.click(inventory, 0, payload.button(), carried));
        } else if (payload.slot() == 2) {
            var out = result(player, inventory, payload.recipe());
            if (!out.isEmpty()) {
                if (payload.shift()) {
                    while (!out.isEmpty() && canInsert(player, out)) {
                        if (!player.getInventory().add(out.copy()))
                            break;
                        inventory.extractItem(0, 1, false);
                        out = result(player, inventory, payload.recipe());
                    }
                } else if (carried.isEmpty()) {
                    player.containerMenu.setCarried(out.copy());
                    inventory.extractItem(0, 1, false);
                } else if (ItemStack.isSameItemSameComponents(carried, out)
                        && carried.getCount() + out.getCount() <= carried.getMaxStackSize()) {
                    carried.grow(out.getCount());
                    player.containerMenu.setCarried(carried);
                    inventory.extractItem(0, 1, false);
                }
            }
        }
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()));
        send(player, access.get());
    }
    private static ItemStack collect(IItemHandlerModifiable inventory, ItemStack carried) {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty() && carried.isEmpty()) return carried;
        if (carried.isEmpty()) return inventory.extractItem(0, stack.getCount(), false);
        if (!ItemStack.isSameItemSameComponents(stack, carried)) return carried;
        ItemStack taken = inventory.extractItem(0, Math.min(carried.getMaxStackSize() - carried.getCount(), stack.getCount()), false);
        return carried.copyWithCount(carried.getCount() + taken.getCount());
    }

    private static boolean canInsert(ServerPlayer player, ItemStack stack) {
        int room = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty())
                room += stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(s, stack))
                room += Math.max(0, s.getMaxStackSize() - s.getCount());
            if (room >= stack.getCount())
                return true;
        }
        return false;
    }

    private static List<RecipeHolder<StonecutterRecipe>> recipes(ServerPlayer player, IItemHandlerModifiable inventory) {
        var in = inventory.getStackInSlot(0);
        return in.isEmpty() ? List.of()
                : RecipeHelper.getRecipesOfType(RecipeType.STONECUTTING, new SingleRecipeInput(in));
    }

    private static ItemStack result(ServerPlayer player, IItemHandlerModifiable inventory, int recipeIndex) {
        var recipes = recipes(player, inventory);
        if (recipeIndex < 0 || recipeIndex >= recipes.size())
            return ItemStack.EMPTY;
        return recipes.get(recipeIndex).value().assemble(new SingleRecipeInput(inventory.getStackInSlot(0)), player.registryAccess());
    }

    public static void send(ServerPlayer player, BackpackAccess access) {
        var upgradeWrapper = wrapper(player, access);
        if (upgradeWrapper == null)
            return;
        var i = upgradeWrapper.getInputInventory();
        var rs = recipes(player, i);
        ItemStack[] out = rs.stream().map(r -> r.value().getResultItem(player.registryAccess())).toArray(ItemStack[]::new);
        int selected = upgradeWrapper.getRecipeId().flatMap(id -> {
            for (int n = 0; n < rs.size(); n++)
                if (rs.get(n).id().equals(id))
                    return Optional.of(n);
            return Optional.empty();
        }).orElse(0);
        PacketDistributor.sendToPlayer(player,
                new StonecutterSyncPayload(i.getStackInSlot(0), result(player, i, selected), out, selected));
    }
}

