package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingDragPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.JeiCraftingFillPayload;
import dev.polaris_light.backpack_side_gui.network.payload.JeiBackpackFillPayload;
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
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

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
        LOGGER.info("JEI crafting fill player={} slots={} max={}", player.getGameProfile().getName(), payload.ingredients().size(), payload.maxTransfer());
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
                ItemStack found = findAndExtract(player, option, payload.maxTransfer());
                LOGGER.info("JEI crafting slot={} wanted={} found={}", slot, option, found);
                if (!found.isEmpty()) {
                    inventory.insertItem(slot, found, false);
                    break;
                }
            }
        }
        player.containerMenu.broadcastChanges();
        send(player, BackpackResolver.resolve(player).orElse(null));
    }

    public static void handleBackpackFill(ServerPlayer player, JeiBackpackFillPayload payload) {
        if (payload.ingredients() == null) return;
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (List<ItemStack> options : payload.ingredients()) {
            int target=-1;
            for(int i=0;i<player.containerMenu.slots.size();i++) {
                var slot=player.containerMenu.getSlot(i);
                if(!used.contains(i)&&slot.getItem().isEmpty()&&slot.container!=player.getInventory()
                        &&options.stream().anyMatch(o->o!=null&&!o.isEmpty()&&slot.mayPlace(o))) { target=i; break; }
            }
            if(target<0) continue;
            for(ItemStack wanted:options) {
                if(wanted==null||wanted.isEmpty()) continue;
                ItemStack found=findAndExtractFromBackpacks(player,wanted,payload.maxTransfer());
                if(found.isEmpty()) continue;
                used.add(target); ItemStack rest=player.containerMenu.getSlot(target).safeInsert(found);
                if(!rest.isEmpty()) returnToBackpacks(player,rest); break;
            }
        }
        player.containerMenu.broadcastChanges();
    }
    private static ItemStack findAndExtractFromBackpacks(ServerPlayer player, ItemStack wanted, boolean maxTransfer) {
        for(BackpackAccess access:BackpackResolver.getAllBackpacks(player)) for(int i=0;i<access.handler().getSlots();i++) {
            ItemStack stack=access.handler().getStackInSlot(i);
            if(ItemStack.isSameItemSameComponents(stack,wanted)) return access.handler().extractItem(i,maxTransfer?Math.min(stack.getCount(),wanted.getMaxStackSize()):1,false);
        }
        return ItemStack.EMPTY;
    }
    private static void returnToBackpacks(ServerPlayer player, ItemStack stack) {
        ItemStack r=stack;
        for(BackpackAccess access:BackpackResolver.getAllBackpacks(player)) for(int i=0;i<access.handler().getSlots()&&!r.isEmpty();i++) r=access.handler().insertItem(i,r,false);
        if(!r.isEmpty()) player.getInventory().placeItemBackInInventory(r);
    }

    private static ItemStack findAndExtract(ServerPlayer player, ItemStack wanted, boolean max) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                int amount = max ? Math.min(stack.getCount(), wanted.getMaxStackSize()) : 1;
                ItemStack out = stack.copyWithCount(amount);
                stack.shrink(amount);
                return out;
            }
        }
        for (BackpackAccess access : BackpackResolver.getAllBackpacks(player)) {
            IItemHandler handler = access.handler();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(stack, wanted))
                    return handler.extractItem(i, max ? Math.min(handler.getStackInSlot(i).getCount(), wanted.getMaxStackSize()) : 1, false);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack result(ServerPlayer player, IItemHandler itemHandler) {
        ItemStack[] x = new ItemStack[9];
        for (int n = 0; n < 9; n++)
            x[n] = itemHandler.getStackInSlot(n).copy();
        CraftingInput input = CraftingInput.of(3, 3, Arrays.asList(x));
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
