package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackDragPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackSlotPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SortPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.BackpackVirtualSlot;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import dev.polaris_light.backpack_side_gui.server.action.ServerActionValidator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BackpackC2S {
    private BackpackC2S() {
    }

    public static void handleSlot(ServerPlayer player, BackpackSlotPayload payload) {
        Optional<BackpackAccess> access = ServerActionValidator.backpack(player, payload.slot());
        if (access.isEmpty())
            return;
        ItemStack carried = player.containerMenu.getCarried();
        if (!ServerActionValidator.carriedMatches(player, payload.carried()))
            return;
        if (player.gameMode.isCreative() && !ItemStack.matches(carried, payload.carried()))
            carried = payload.carried().copy();
        BackpackVirtualSlot slot = new BackpackVirtualSlot(access.get().stack(), payload.slot(), player);
        if (payload.clickType() == 6) {
            player.containerMenu.setCarried(
                    HandlerSlotClicker.collect(access.get().handler(), payload.slot(), carried));
        } else if (payload.clickType() == 4 || payload.clickType() == 5) {
            if (!carried.isEmpty())
                return;
            ItemStack picked = slot.getItem().copy();
            int limit = Math.max(1, picked.getMaxStackSize());
            int amount = payload.clickType() == 5 ? Math.min(limit, (picked.getCount() + 1) / 2)
                    : Math.min(limit, picked.getCount());
            ItemStack moved = slot.remove(amount);
            if (!player.getInventory().add(moved))
                slot.set(moved);
        } else if (payload.clickType() <= 1) {
            if (!carried.isEmpty())
                return;
            ItemStack in = slot.getItem();
            int max = Math.min(Math.max(1, in.getMaxStackSize()), in.getCount());
            int amount = payload.clickType() == 1 ? Math.min(max, (in.getCount() + 1) / 2) : max;
            player.containerMenu
                    .setCarried(slot.isInfinite() ? slot.remove(amount) : slot.safeTake(amount, amount, player));
        } else {
            if (carried.isEmpty() || !slot.mayPlace(carried))
                return;
            int amount = payload.clickType() == 2 ? 1
                    : (payload.clickType() >= 10 ? Math.min(carried.getCount(), payload.clickType() - 10)
                            : carried.getCount());
            ItemStack rest = slot.safeInsert(carried.copyWithCount(amount));
            ItemStack remaining = carried.copy();
            remaining.shrink(amount - rest.getCount());
            player.containerMenu.setCarried(remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        }
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()),
                new CustomPacketPayload[0]);
        PacketDistributor.sendToPlayer(player, snapshot(access.get(), player), new CustomPacketPayload[0]);
    }

    public static void handleDrag(ServerPlayer player, BackpackDragPayload payload) {
        Optional<BackpackAccess> resolved = BackpackResolver.resolve(player);
        if (resolved.isEmpty() || payload.slots().size() < 2)
            return;
        ItemStack carried = player.containerMenu.getCarried();
        if (!player.gameMode.isCreative() && !ItemStack.matches(carried, payload.carried()))
            return;
        if (carried.isEmpty())
            return;
        LinkedHashSet<Integer> unique = new LinkedHashSet<>(payload.slots());
        unique.removeIf(slotIndex -> slotIndex < 0 || slotIndex >= resolved.get().handler().getSlots());
        unique.removeIf(slotIndex -> {
            BackpackVirtualSlot candidate = new BackpackVirtualSlot(resolved.get().stack(), slotIndex, player);
            ItemStack existing = candidate.getItem();
            return (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, carried))
                    || !candidate.mayPlace(carried)
                    || !resolved.get().handler().insertItem(slotIndex, carried.copyWithCount(1), true).isEmpty();
        });
        if (unique.isEmpty())
            return;
        int each = payload.button() == 1 ? 1 : carried.getCount() / unique.size();
        if (each <= 0)
            return;
        for (int index : unique) {
            BackpackVirtualSlot slot = new BackpackVirtualSlot(resolved.get().stack(), index, player);
            if (!slot.mayPlace(carried))
                continue;
            int amount = Math.min(each, carried.getCount());
            ItemStack rest = slot.safeInsert(carried.copyWithCount(amount));
            carried.shrink(amount - rest.getCount());
            if (carried.isEmpty())
                break;
        }
        player.containerMenu.setCarried(carried);
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(carried.copy()), new CustomPacketPayload[0]);
        PacketDistributor.sendToPlayer(player, snapshot(resolved.get(), player), new CustomPacketPayload[0]);
    }

    public static void handleSort(ServerPlayer player, SortPayload payload) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty() || !(access.get().handler() instanceof IItemHandlerModifiable inv))
            return;
        ArrayList<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i).copy();
            if (!stack.isEmpty())
                stacks.add(stack);
            inv.setStackInSlot(i, ItemStack.EMPTY);
        }
        ArrayList<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack left = stack.copy();
            for (ItemStack e : merged)
                if (ItemStack.isSameItemSameComponents(e, left)) {
                    int n = Math.min(left.getCount(),
                            Math.max(0, Math.max(access.get().stackLimit(), e.getMaxStackSize()) - e.getCount()));
                    e.grow(n);
                    left.shrink(n);
                    if (left.isEmpty())
                        break;
                }
            while (!left.isEmpty()) {
                ItemStack part = left.copy();
                part.setCount(Math.min(left.getCount(), Math.max(access.get().stackLimit(), left.getMaxStackSize())));
                merged.add(part);
                left.shrink(part.getCount());
            }
        }
        Comparator<ItemStack> name = Comparator
                .comparing(stackEntry -> stackEntry.getHoverName().getString().toLowerCase(Locale.ROOT));
        if (payload.sortMode() == 0)
            merged.sort(Comparator.comparingInt(ItemStack::getCount).reversed().thenComparing(name));
        else if (payload.sortMode() == 1)
            merged.sort(Comparator.comparing(
                    (ItemStack stackEntry) -> BuiltInRegistries.ITEM.getKey(stackEntry.getItem()).getNamespace())
                    .thenComparing(name));
        else if (payload.sortMode() == 2)
            merged.sort(name);
        else
            merged.sort(Comparator
                    .comparing((ItemStack stackEntry) -> BuiltInRegistries.ITEM.getKey(stackEntry.getItem()).toString())
                    .thenComparing(name));
        int slots = inv.getSlots();
        for (int i = 0; i < merged.size(); i++) {
            ItemStack stack = merged.get(i);
            if (i < slots)
                inv.setStackInSlot(i, stack);
            else if (!player.getInventory().add(stack.copy()))
                player.drop(stack, false);
        }
        PacketDistributor.sendToPlayer(player, snapshot(access.get(), player), new CustomPacketPayload[0]);
    }

    public static BackpackSyncPayload snapshot(BackpackAccess access, ServerPlayer player) {
        ArrayList<ItemStack> s = new ArrayList<>();
        ArrayList<Integer> limits = new ArrayList<>();
        for (int i = 0; i < access.handler().getSlots(); i++) {
            ItemStack item = access.handler().getStackInSlot(i).copy();
            s.add(item);
            BackpackVirtualSlot virtualSlot = new BackpackVirtualSlot(access.stack(), i, player);
            limits.add(virtualSlot.getMaxStackSize(item));
        }
        return new BackpackSyncPayload(access.stack().getHoverName().getString(), s, limits);
    }
}
