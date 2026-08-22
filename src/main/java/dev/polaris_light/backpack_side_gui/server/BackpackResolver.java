package dev.polaris_light.backpack_side_gui.server;

import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import top.theillusivec4.curios.api.CuriosApi;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Owns the per-player active backpack cache and its validity checks. */
public final class BackpackResolver {
    private static final Map<UUID, BackpackAccess> cached = new ConcurrentHashMap<>();

    // Cache
    public static Optional<BackpackAccess> cached(ServerPlayer player) {
        BackpackAccess access = cached.get(player.getUUID());
        return access != null && isValid(player, access) ? Optional.of(access) : Optional.empty();
    }

    public static boolean isValid(ServerPlayer player, BackpackAccess access) {
        if (access.curiosHandler() != null) {
            return access.curiosSlot() >= 0 && access.curiosSlot() < access.curiosHandler().getSlots()
                    && access.curiosHandler().getStackInSlot(access.curiosSlot()) == access.stack();
        }

        int slot = access.playerInventorySlot();
        return slot >= 0 && slot < player.getInventory().items.size()
                && player.getInventory().items.get(slot) == access.stack();
    }

    public static void remember(ServerPlayer player, BackpackAccess access) {
        cached.put(player.getUUID(), access);
    }

    public static void forget(ServerPlayer player) {
        cached.remove(player.getUUID());
    }

    // Find the first backpack in the player's inventory or curios slots
    public static Optional<BackpackAccess> resolve(ServerPlayer player) {
        Optional<BackpackAccess> hit = cached(player);
        if (hit.isPresent())
            return hit;

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            Optional<BackpackAccess> found = createBackpackAccess(i, player.getInventory().items.get(i));
            if (found.isPresent()) {
                remember(player, found.get());
                return found;
            }
        }

        var curios = CuriosApi.getCuriosInventory(player);
        if (curios.isPresent()) {
            int index = 0;
            for (var entry : curios.get().getCurios().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++, index++) {
                    Optional<BackpackAccess> found = createBackpackAccess(-1000 - index, stacks.getStackInSlot(slot),
                            stacks, slot);
                    if (found.isPresent()) {
                        remember(player, found.get());
                        return found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    // Find all backpacks in the player's inventory and curios slots
    public static List<BackpackAccess> getAllBackpacks(ServerPlayer player) {
        List<BackpackAccess> result = new ArrayList<>();
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            Optional<BackpackAccess> backpack = createBackpackAccess(i, player.getInventory().items.get(i));
            backpack.ifPresent(result::add);
        }
        var curios = CuriosApi.getCuriosInventory(player);
        if (curios.isPresent()) {
            int index = 0;
            for (var entry : curios.get().getCurios().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++, index++) {
                    createBackpackAccess(-1000 - index, stacks.getStackInSlot(slot), stacks, slot)
                            .ifPresent(backpack -> {
                                if (result.stream().noneMatch(existing -> existing.stack() == backpack.stack()))
                                    result.add(backpack);
                            });
                }
            }
        }
        return result;
    }

    private static Optional<BackpackAccess> createBackpackAccess(int slot, ItemStack stack) {
        return createBackpackAccess(slot, stack, null, -1);
    }

    private static Optional<BackpackAccess> createBackpackAccess(int slot, ItemStack stack, IItemHandler curiosHandler,
            int curiosSlot) {
        if (stack == null || stack.isEmpty() || !isSophisticatedBackpack(stack))
            return Optional.empty();

        IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
        if (handler == null) {
            return Optional.empty();
        }

        IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
        int stackLimit = StackUpgradeItem.getInventorySlotLimit(wrapper);
        IItemHandler effective = stackLimit > 64 ? new StackLimitItemHandler(handler, stackLimit) : handler;
        return Optional.of(new BackpackAccess(slot, stack, effective, stackLimit, curiosHandler, curiosSlot));
    }

    private static boolean isSophisticatedBackpack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof BackpackItem;
    }
}
