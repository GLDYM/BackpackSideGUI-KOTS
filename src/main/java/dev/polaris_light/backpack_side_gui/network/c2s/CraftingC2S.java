package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.Arrays;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingDragPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingSyncPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;

public final class CraftingC2S {
    private CraftingC2S() {
    }

    private static IItemHandler inv(ServerPlayer player) {
        var access = BackpackResolver.resolve(player);
        if (access.isEmpty())
            return null;
        var wrappers = BackpackWrapper
                .fromStack(access.get().stack())
                .getUpgradeHandler().getWrappersThatImplement(CraftingUpgradeWrapper.class);
        return wrappers.isEmpty() ? null : wrappers.get(0).getInventory();
    }

    public static void handleClick(ServerPlayer player, CraftingClickPayload payload) {
        var access = BackpackResolver.resolve(player);
        if (access.isEmpty() || payload.slot() < 0 || payload.slot() > 9)
            return;
        var inventory = inv(player);
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
            ItemStack test = carried.isEmpty() ? inventory.getStackInSlot(payload.slot()) : carried;
            if (!test.isEmpty())
                for (int slot = 0; slot < 9; slot++) {
                    ItemStack stack = inventory.getStackInSlot(slot);
                    if (!ItemStack.isSameItemSameComponents(test, stack))
                        continue;
                    int room = test.getMaxStackSize() - carried.getCount();
                    if (room <= 0)
                        break;
                    int m = Math.min(room, stack.getCount());
                    inventory.extractItem(slot, m, false);
                    carried = carried.isEmpty() ? stack.copyWithCount(m) : carried.copyWithCount(carried.getCount() + m);
                }
            player.containerMenu.setCarried(carried);
        } else if (payload.slot() == 9) {
            ItemStack result = result(player, inventory);
            if (!result.isEmpty()) {
                if (payload.shift()) {
                    while (!result.isEmpty() && canInsert(player, result)) {
                        if (!player.getInventory().add(result.copy()))
                            break;
                        consume(inventory);
                        result = result(player, inventory);
                    }
                } else if (carried.isEmpty()) {
                    player.containerMenu.setCarried(result.copy());
                    consume(inventory);
                } else if (ItemStack.isSameItemSameComponents(carried, result)
                        && carried.getCount() + result.getCount() <= carried.getMaxStackSize()) {
                    carried.grow(result.getCount());
                    player.containerMenu.setCarried(carried);
                    consume(inventory);
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
        var access = BackpackResolver.resolve(player);
        if (access.isEmpty() || payload.slots().size() < 2)
            return;
        var inventory = inv(player);
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

    private static ItemStack result(ServerPlayer player, IItemHandler itemHandler) {
        ItemStack[] x = new ItemStack[9];
        for (int n = 0; n < 9; n++)
            x[n] = itemHandler.getStackInSlot(n).copy();
        var input = CraftingInput.of(3, 3, Arrays.asList(x));
        return player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .map(h -> h.value().assemble(input, player.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static void consume(IItemHandler itemHandler) {
        for (int n = 0; n < 9; n++)
            if (!itemHandler.getStackInSlot(n).isEmpty())
                itemHandler.extractItem(n, 1, false);
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
        var inventory = inv(player);
        if (inventory == null)
            return;
        ItemStack[] x = new ItemStack[10];
        for (int n = 0; n < 9; n++)
            x[n] = inventory.getStackInSlot(n).copy();
        x[9] = result(player, inventory);
        PacketDistributor.sendToPlayer(player, new CraftingSyncPayload(x));
    }
}
