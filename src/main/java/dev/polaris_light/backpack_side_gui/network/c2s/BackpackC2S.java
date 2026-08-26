package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackDragPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackSlotPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SortPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.BackpackVirtualSlot;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BackpackC2S {
    private BackpackC2S() {
    }

    public static void handleSlot(ServerPlayer player, BackpackSlotPayload p) {
        var access = BackpackResolver.resolve(player);
        if (access.isEmpty() || p.slot() < 0 || p.slot() >= access.get().handler().getSlots())
            return;
        ItemStack carried = player.containerMenu.getCarried();
        if (!player.gameMode.isCreative() && !ItemStack.matches(carried, p.carried()))
            return;
        if (player.gameMode.isCreative() && !ItemStack.matches(carried, p.carried()))
            carried = p.carried().copy();
        var slot = new BackpackVirtualSlot(access.get().stack(), p.slot(), player);
        if (p.clickType() == 6) {
            ItemStack target = carried.isEmpty() ? slot.getItem() : carried;
            if (target.isEmpty())
                return;
            for (int i = 0; i < access.get().handler().getSlots(); i++) {
                ItemStack source = access.get().handler().getStackInSlot(i);
                if (!ItemStack.isSameItemSameComponents(target, source))
                    continue;
                int room = target.getMaxStackSize() - carried.getCount();
                if (room <= 0)
                    break;
                int move = Math.min(room, source.getCount());
                access.get().handler().extractItem(i, move, false);
                carried = carried.isEmpty() ? source.copyWithCount(move)
                        : carried.copyWithCount(carried.getCount() + move);
            }
            player.containerMenu.setCarried(carried);
        } else if (p.clickType() == 4 || p.clickType() == 5) {
            if (!carried.isEmpty())
                return;
            ItemStack picked = slot.getItem().copy();
            int limit = Math.max(1, picked.getMaxStackSize());
            int amount = p.clickType() == 5 ? Math.min(limit, (picked.getCount() + 1) / 2)
                    : Math.min(limit, picked.getCount());
            ItemStack moved = slot.remove(amount);
            if (!player.getInventory().add(moved))
                slot.set(moved);
        } else if (p.clickType() <= 1) {
            if (!carried.isEmpty())
                return;
            ItemStack in = slot.getItem();
            int max = Math.min(Math.max(1, in.getMaxStackSize()), in.getCount());
            int amount = p.clickType() == 1 ? Math.min(max, (in.getCount() + 1) / 2) : max;
            player.containerMenu
                    .setCarried(slot.isInfinite() ? slot.remove(amount) : slot.safeTake(amount, amount, player));
        } else {
            if (carried.isEmpty() || !slot.mayPlace(carried))
                return;
            int amount = p.clickType() == 2 ? 1
                    : (p.clickType() >= 10 ? Math.min(carried.getCount(), p.clickType() - 10) : carried.getCount());
            ItemStack rest = slot.safeInsert(carried.copyWithCount(amount));
            ItemStack remaining = carried.copy();
            remaining.shrink(amount - rest.getCount());
            player.containerMenu.setCarried(remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        }
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()),
                new CustomPacketPayload[0]);
        PacketDistributor.sendToPlayer(player, snapshot(access.get()), new CustomPacketPayload[0]);
    }

    public static void handleDrag(ServerPlayer player, BackpackDragPayload p) {
        var resolved = BackpackResolver.resolve(player);
        if (resolved.isEmpty() || p.slots().size() < 2)
            return;
        ItemStack carried = player.containerMenu.getCarried();
        if (!player.gameMode.isCreative() && !ItemStack.matches(carried, p.carried()))
            return;
        if (carried.isEmpty())
            return;
        var unique = new LinkedHashSet<Integer>(p.slots());
        unique.removeIf(i -> i < 0 || i >= resolved.get().handler().getSlots());
        unique.removeIf(i -> {
            var candidate = new BackpackVirtualSlot(resolved.get().stack(), i, player);
            ItemStack existing = candidate.getItem();
            return (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, carried))
                    || !candidate.mayPlace(carried)
                    || !resolved.get().handler().insertItem(i, carried.copyWithCount(1), true).isEmpty();
        });
        if (unique.isEmpty())
            return;
        int each = p.button() == 1 ? 1 : carried.getCount() / unique.size();
        if (each <= 0)
            return;
        for (int index : unique) {
            var slot = new BackpackVirtualSlot(resolved.get().stack(), index, player);
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
        PacketDistributor.sendToPlayer(player, snapshot(resolved.get()), new CustomPacketPayload[0]);
    }

    public static void handleSort(ServerPlayer player, SortPayload p) {
        var access = BackpackResolver.resolve(player);
        if (access.isEmpty() || !(access.get().handler() instanceof IItemHandlerModifiable inv))
            return;
        var stacks = new ArrayList<ItemStack>();
        for (int i = 0; i < inv.getSlots(); i++) {
            var s = inv.getStackInSlot(i).copy();
            if (!s.isEmpty())
                stacks.add(s);
            inv.setStackInSlot(i, ItemStack.EMPTY);
        }
        var merged = new ArrayList<ItemStack>();
        for (var s : stacks) {
            var left = s.copy();
            for (var e : merged)
                if (ItemStack.isSameItemSameComponents(e, left)) {
                    int n = Math.min(left.getCount(),
                            Math.max(0, Math.max(access.get().stackLimit(), e.getMaxStackSize()) - e.getCount()));
                    e.grow(n);
                    left.shrink(n);
                    if (left.isEmpty())
                        break;
                }
            while (!left.isEmpty()) {
                var part = left.copy();
                part.setCount(Math.min(left.getCount(), Math.max(access.get().stackLimit(), left.getMaxStackSize())));
                merged.add(part);
                left.shrink(part.getCount());
            }
        }
        Comparator<ItemStack> name = Comparator.comparing(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT));
        if (p.sortMode() == 0)
            merged.sort(Comparator.comparingInt(ItemStack::getCount).reversed().thenComparing(name));
        else if (p.sortMode() == 1)
            merged.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace())
                    .thenComparing(name));
        else if (p.sortMode() == 2)
            merged.sort(name);
        else
            merged.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                    .thenComparing(name));
        for (int i = 0; i < merged.size() && i < inv.getSlots(); i++)
            inv.setStackInSlot(i, merged.get(i));
        PacketDistributor.sendToPlayer(player, snapshot(access.get()), new CustomPacketPayload[0]);
    }

    public static BackpackSyncPayload snapshot(BackpackAccess a) {
        var s = new ArrayList<ItemStack>();
        for (int i = 0; i < a.handler().getSlots(); i++)
            s.add(a.handler().getStackInSlot(i).copy());
        return new BackpackSyncPayload(a.stack().getHoverName().getString(), s);
    }
}
