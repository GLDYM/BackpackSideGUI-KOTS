package dev.polaris_light.backpack_side_gui.server;

import dev.polaris_light.backpack_side_gui.network.ModNetwork;
import dev.polaris_light.backpack_side_gui.network.PanelSyncPayload;
import dev.polaris_light.backpack_side_gui.network.UtilitySyncPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import top.theillusivec4.curios.api.CuriosApi;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;

public final class ServerBackpackAccess {
    private static final int MAX_SYNC_SLOTS = 216;
    public static final int UTILITY_CRAFTING = 0;
    public static final int UTILITY_FURNACE = 1;
    public static final int UTILITY_ANVIL = 2;
    public static final int UTILITY_SMITHING = 3;
    private static final BackpackResolver BACKPACK_RESOLVER = new BackpackResolver();
    private static final Map<String, UtilityState> UTILITY_STATES = new HashMap();

    private ServerBackpackAccess() {
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BACKPACK_RESOLVER.forget(player);
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        try {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                tickActiveBackpackUtilities(player);
            }
        } catch (Throwable th) {
        }
    }

    private static void tickActiveBackpackUtilities(ServerPlayer player) {
        Optional<BackpackAccess> access = getActiveBackpack(player);
        if (access.isEmpty()) {
            return;
        }
        BackpackAccess bp = access.get();
        UpgradeFlags flags = FlagResolver.resolve(bp);
        handleLostUtilityUpgrades(player, bp, flags);
        if (!flags.furnace()) {
            return;
        }
        UtilityState state = getUtilityState(bp.stack());
        if (tickFurnace(player, state)) {
            saveUtilityState(bp.stack(), state);
            touchBackpack(player, bp);
        }
    }

    public static void syncTo(ServerPlayer player) {
        Optional<BackpackAccess> access = getActiveBackpack(player);
        if (access.isEmpty()) {
            ModNetwork.sendPanelSync(player, PanelSyncPayload.empty());
            return;
        }
        BackpackAccess bp = access.get();
        UpgradeFlags flags = FlagResolver.resolve(bp);
        handleLostUtilityUpgrades(player, bp, flags);
        List<ItemStack> list = new ArrayList<>();
        int count = Math.min(bp.handler().getSlots(), MAX_SYNC_SLOTS);
        for (int i = 0; i < count; i++) {
            list.add(bp.handler().getStackInSlot(i).copy());
        }
        ModNetwork.sendPanelSync(player, new PanelSyncPayload(true, count, bp.stack().getHoverName().getString(), list, flags.crafting(), flags.furnace(), flags.anvil(), flags.smithing()));
    }

    /** Returns the selected backpack, keeping invalid client requests synchronized. */
    private static Optional<BackpackAccess> getActiveBackpackOrSync(ServerPlayer player) {
        Optional<BackpackAccess> access = getActiveBackpack(player);
        if (access.isEmpty()) {
            syncTo(player);
        }
        return access;
    }

    public static void handlePanelClick(ServerPlayer player, int slot, int button, ItemStack clientCarried) {
        applyCreativeCarried(player, clientCarried);
        Optional<BackpackAccess> access = getActiveBackpackOrSync(player);
        if (access.isEmpty() || slot < 0) {
            syncTo(player);
            return;
        }
        BackpackAccess bp = access.get();
        if (bp.handler() instanceof IItemHandlerModifiable modifiable) {
            if (slot < bp.handler().getSlots()) {
                ItemStack carried = player.containerMenu.getCarried().copy();
                ItemStack inSlot = bp.handler().getStackInSlot(slot).copy();
                if (button == 1) {
                    rightClick(player, modifiable, slot, carried, inSlot);
                } else {
                    leftClick(player, modifiable, slot, carried, inSlot);
                }
                touchBackpack(player, bp);
                sendFullMenu(player);
                syncTo(player);
                return;
            }
        }
        syncTo(player);
    }

    public static void handleDoubleCollect(ServerPlayer player, int slot, ItemStack clientCarried) {
        applyCreativeCarried(player, clientCarried);
        Optional<BackpackAccess> access = getActiveBackpackOrSync(player);
        if (access.isEmpty() || slot < 0) {
            syncTo(player);
            return;
        }
        BackpackAccess bp = access.get();
        if (bp.handler() instanceof IItemHandlerModifiable modifiable) {
            if (slot < bp.handler().getSlots()) {
                ItemStack carried = player.containerMenu.getCarried().copy();
                ItemStack sample = carried.isEmpty() ? bp.handler().getStackInSlot(slot).copy() : carried.copy();
                if (sample.isEmpty()) {
                    syncTo(player);
                    return;
                }
                if (carried.isEmpty()) {
                    carried = ItemStack.EMPTY;
                }
                int max = Math.max(sample.getMaxStackSize(), bp.stackLimit());
                for (int i = 0; i < bp.handler().getSlots() && (carried.isEmpty() || carried.getCount() < max); i++) {
                    ItemStack stack = bp.handler().getStackInSlot(i).copy();
                    if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, sample)) {
                        if (carried.isEmpty()) {
                            int take = Math.min(max, stack.getCount());
                            carried = stack.copyWithCount(take);
                            stack.shrink(take);
                        } else {
                            int move = Math.min(max - carried.getCount(), stack.getCount());
                            carried.grow(move);
                            stack.shrink(move);
                        }
                        modifiable.setStackInSlot(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
                    }
                }
                player.containerMenu.setCarried(carried);
                touchBackpack(player, bp);
                sendFullMenu(player);
                syncTo(player);
                return;
            }
        }
        syncTo(player);
    }

    public static void handleQuickTransfer(ServerPlayer player, int side, int slot) {
        Optional<BackpackAccess> access = getActiveBackpackOrSync(player);
        if (access.isEmpty()) {
            return;
        }
        BackpackAccess bp = access.get();
        if (!(bp.handler() instanceof IItemHandlerModifiable modifiable)) {
            syncTo(player);
            return;
        }
        if (side == 0) {
            if (slot >= 0 && slot < bp.handler().getSlots()) {
                ItemStack left = insertIntoPlayerInventory(player, bp.handler().getStackInSlot(slot).copy());
                modifiable.setStackInSlot(slot, left);
                touchBackpack(player, bp);
            }
        } else if (slot >= 0 && slot < player.containerMenu.slots.size()) {
            Slot menuSlot = (Slot) player.containerMenu.slots.get(slot);
            ItemStack stack = menuSlot.getItem().copy();
            if (!stack.isEmpty() && menuSlot.mayPickup(player)) {
                ItemStack left2 = insertIntoBackpack(bp.handler(), modifiable, stack);
                int moved = stack.getCount() - left2.getCount();
                if (moved > 0) {
                    ItemStack removed = menuSlot.remove(moved);
                    if (!removed.isEmpty()) {
                        menuSlot.onTake(player, removed);
                    }
                    menuSlot.setChanged();
                    touchBackpack(player, bp);
                }
            }
        }
        sendFullMenu(player);
        syncTo(player);
    }

    public static void handleDragDistribute(ServerPlayer player, List<Integer> rawSlots, int button, ItemStack clientCarried) {
        int move;
        applyCreativeCarried(player, clientCarried);
        Optional<BackpackAccess> access = getActiveBackpackOrSync(player);
        if (access.isEmpty() || rawSlots == null || rawSlots.isEmpty()) {
            syncTo(player);
            return;
        }
        BackpackAccess bp = access.get();
        if (!(bp.handler() instanceof IItemHandlerModifiable modifiable)) {
            syncTo(player);
            return;
        }
        ItemStack carried = player.containerMenu.getCarried().copy();
        if (carried.isEmpty()) {
            syncTo(player);
            return;
        }
        List<Integer> slots = rawSlots.stream().filter(i -> {
            return i != null && i.intValue() >= 0 && i.intValue() < bp.handler().getSlots();
        }).distinct().filter(i2 -> {
            return canDragPlace(bp.handler(), i2.intValue(), carried);
        }).toList();
        if (slots.isEmpty()) {
            syncTo(player);
            return;
        }
        boolean changed = false;
        if (button == 1) {
            Iterator<Integer> it = slots.iterator();
            while (it.hasNext()) {
                int slot = it.next().intValue();
                if (carried.isEmpty()) {
                    break;
                } else if (placeOne(modifiable, bp.handler(), slot, carried)) {
                    changed = true;
                }
            }
        } else {
            int perSlot = Math.max(1, carried.getCount() / slots.size());
            Iterator<Integer> it2 = slots.iterator();
            while (it2.hasNext()) {
                int slot2 = it2.next().intValue();
                if (carried.isEmpty()) {
                    break;
                }
                ItemStack inSlot = bp.handler().getStackInSlot(slot2).copy();
                int max = getSlotLimit(bp.handler(), slot2, inSlot, carried);
                int room = inSlot.isEmpty() ? max : max - inSlot.getCount();
                if (room > 0 && (move = Math.min(Math.min(perSlot, room), carried.getCount())) > 0) {
                    if (inSlot.isEmpty()) {
                        modifiable.setStackInSlot(slot2, carried.copyWithCount(move));
                    } else {
                        inSlot.grow(move);
                        modifiable.setStackInSlot(slot2, inSlot);
                    }
                    carried.shrink(move);
                    changed = true;
                }
            }
        }
        player.containerMenu.setCarried(carried);
        if (changed) {
            touchBackpack(player, bp);
            sendFullMenu(player);
        }
        syncTo(player);
    }

    private static int getSlotLimit(IItemHandler handler, int slot, ItemStack inSlot, ItemStack carried) {
        int handlerLimit = 64;
        try {
            handlerLimit = Math.max(1, handler.getSlotLimit(slot));
        } catch (Throwable th) {
        }
        if (handler instanceof StackLimitItemHandler) {
            StackLimitItemHandler stackLimitHandler = (StackLimitItemHandler) handler;
            handlerLimit = Math.max(handlerLimit, stackLimitHandler.stackLimit());
        }
        int max = Math.max((carried == null || carried.isEmpty()) ? 64 : carried.getMaxStackSize(), handlerLimit);
        if (inSlot != null && !inSlot.isEmpty()) {
            max = Math.max(max, Math.max(inSlot.getMaxStackSize(), inSlot.getCount()));
        }
        return Math.max(1, max);
    }

    private static boolean canDragPlace(IItemHandler handler, int slot, ItemStack carried) {
        if (slot < 0 || slot >= handler.getSlots() || carried == null || carried.isEmpty()) {
            return false;
        }
        ItemStack inSlot = handler.getStackInSlot(slot).copy();
        if (!handler.isItemValid(slot, carried)) {
            return false;
        }
        if (inSlot.isEmpty() || ItemStack.isSameItemSameComponents(inSlot, carried)) {
            return inSlot.isEmpty() || inSlot.getCount() < getSlotLimit(handler, slot, inSlot, carried);
        }
        return false;
    }

    private static boolean placeOne(IItemHandlerModifiable modifiable, IItemHandler handler, int slot, ItemStack carried) {
        if (!canDragPlace(handler, slot, carried)) {
            return false;
        }
        ItemStack inSlot = handler.getStackInSlot(slot).copy();
        if (inSlot.isEmpty()) {
            modifiable.setStackInSlot(slot, carried.copyWithCount(1));
        } else {
            inSlot.grow(1);
            modifiable.setStackInSlot(slot, inSlot);
        }
        carried.shrink(1);
        return true;
    }

    public static void handleSort(ServerPlayer player, int sortMode) {
        Optional<BackpackAccess> access = getActiveBackpackOrSync(player);
        if (access.isEmpty()) {
            return;
        }
        BackpackAccess bp = access.get();
        if (!(bp.handler() instanceof IItemHandlerModifiable modifiable)) {
            syncTo(player);
            return;
        }
        List<ItemStack> rawStacks = new ArrayList<>();
        for (int i = 0; i < bp.handler().getSlots(); i++) {
            ItemStack stack = bp.handler().getStackInSlot(i).copy();
            if (!stack.isEmpty()) {
                rawStacks.add(stack);
            }
            modifiable.setStackInSlot(i, ItemStack.EMPTY);
        }
        List<ItemStack> compacted = compactAndSortStacks(rawStacks, Math.max(64, bp.stackLimit()), sortMode);
        for (int i2 = 0; i2 < compacted.size() && i2 < bp.handler().getSlots(); i2++) {
            modifiable.setStackInSlot(i2, compacted.get(i2));
        }
        touchBackpack(player, bp);
        syncTo(player);
    }

    private static List<ItemStack> compactAndSortStacks(List<ItemStack> rawStacks, int globalStackLimit, int sortMode) {
        List<ItemStack> compacted = new ArrayList<>();
        for (ItemStack original : rawStacks) {
            ItemStack remaining = original.copy();
            while (!remaining.isEmpty()) {
                boolean merged = false;
                for (ItemStack existing : compacted) {
                    if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                        int limit = Math.max(Math.max(existing.getMaxStackSize(), remaining.getMaxStackSize()), globalStackLimit);
                        int room = Math.max(0, limit - existing.getCount());
                        int move = Math.min(room, remaining.getCount());
                        if (move > 0) {
                            existing.grow(move);
                            remaining.shrink(move);
                            merged = true;
                        }
                        if (remaining.isEmpty()) {
                            break;
                        }
                    }
                }
                if (!remaining.isEmpty()) {
                    int limit2 = Math.max(remaining.getMaxStackSize(), globalStackLimit);
                    ItemStack split = remaining.copyWithCount(Math.min(limit2, remaining.getCount()));
                    compacted.add(split);
                    remaining.shrink(split.getCount());
                }
                if (merged || remaining.isEmpty() || !compacted.isEmpty()) {
                }
            }
        }
        compacted.sort(getComparator(sortMode));
        return compacted;
    }

    public static void handleUtilityRequest(ServerPlayer player, int utilityType) {
        Optional<BackpackAccess> access = getActiveBackpack(player);
        if (access.isEmpty()) {
            return;
        }
        BackpackAccess bp = access.get();
        UpgradeFlags flags = FlagResolver.resolve(bp);
        handleLostUtilityUpgrades(player, bp, flags);
        if (flags.allows(utilityType)) {
            UtilityState state = getUtilityState(bp.stack());
            sendUtilitySync(player, utilityType, state);
        }
    }

    public static void handleUtilityRename(ServerPlayer player, String name) {
        Optional<BackpackAccess> access = getActiveBackpack(player);
        if (access.isEmpty()) {
            return;
        }
        BackpackAccess bp = access.get();
        UtilityState state = getUtilityState(bp.stack());
        state.anvilName = sanitizeName(name);
        saveUtilityState(bp.stack(), state);
        touchBackpack(player, bp);
        sendUtilitySync(player, 2, state);
    }

    public static void handleUtilityClick(ServerPlayer player, int utilityType, int slot, int button, ItemStack clientCarried) {
        applyCreativeCarried(player, clientCarried);
        Optional<BackpackAccess> access = getActiveBackpack(player);
        if (access.isEmpty()) {
            syncTo(player);
            return;
        }
        BackpackAccess bp = access.get();
        UpgradeFlags flags = FlagResolver.resolve(bp);
        handleLostUtilityUpgrades(player, bp, flags);
        if (!flags.allows(utilityType)) {
            syncTo(player);
            return;
        }
        UtilityState state = getUtilityState(bp.stack());
        if (utilityType == 0) {
            if (slot == 9) {
                takeCraftingResult(player, state);
            } else if (slot >= 0 && slot < 9) {
                clickVirtualStack(player, state.crafting, slot, button);
            }
        } else if (utilityType == 1) {
            if (slot == 2) {
                takeResultSlot(player, state.furnace, 2);
            } else if (slot == 0 || slot == 1) {
                clickVirtualStack(player, state.furnace, slot, button);
            }
        } else if (utilityType == 2) {
            if (slot == 2) {
                takeAnvilResult(player, state);
            } else if (slot == 0 || slot == 1) {
                clickVirtualStack(player, state.anvil, slot, button);
            }
        } else if (utilityType == 3) {
            if (slot == 3) {
                takeSmithingResult(player, state);
            } else if (slot >= 0 && slot < 3) {
                clickVirtualStack(player, state.smithing, slot, button);
            }
        }
        saveUtilityState(bp.stack(), state);
        touchBackpack(player, bp);
        sendFullMenu(player);
        syncTo(player);
        sendUtilitySync(player, utilityType, state);
    }

    private static void applyCreativeCarried(ServerPlayer player, ItemStack clientCarried) {
        if (player != null && player.isCreative() && player.containerMenu != null && player.containerMenu.getCarried().isEmpty() && clientCarried != null && !clientCarried.isEmpty()) {
            player.containerMenu.setCarried(clientCarried.copy());
        }
    }

    private static void leftClick(ServerPlayer player, IItemHandlerModifiable handler, int slot, ItemStack carried, ItemStack inSlot) {
        int move;
        if (carried.isEmpty()) {
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            player.containerMenu.setCarried(inSlot);
            return;
        }
        int limit = getSlotLimit(handler, slot, inSlot, carried);
        if (inSlot.isEmpty()) {
            if (handler.isItemValid(slot, carried)) {
                int move2 = Math.min(limit, carried.getCount());
                handler.setStackInSlot(slot, carried.copyWithCount(move2));
                carried.shrink(move2);
                player.containerMenu.setCarried(carried);
                return;
            }
            return;
        }
        if (handler.isItemValid(slot, carried)) {
            if (ItemStack.isSameItemSameComponents(inSlot, carried) && (move = Math.min(limit - inSlot.getCount(), carried.getCount())) > 0) {
                inSlot.grow(move);
                carried.shrink(move);
                handler.setStackInSlot(slot, inSlot);
                player.containerMenu.setCarried(carried);
                return;
            }
            handler.setStackInSlot(slot, carried);
            player.containerMenu.setCarried(inSlot);
        }
    }

    private static void rightClick(ServerPlayer player, IItemHandlerModifiable handler, int slot, ItemStack carried, ItemStack inSlot) {
        if (carried.isEmpty()) {
            if (inSlot.isEmpty()) {
                return;
            }
            int take = (inSlot.getCount() + 1) / 2;
            ItemStack picked = inSlot.copyWithCount(take);
            inSlot.shrink(take);
            handler.setStackInSlot(slot, inSlot.isEmpty() ? ItemStack.EMPTY : inSlot);
            player.containerMenu.setCarried(picked);
            return;
        }
        int limit = getSlotLimit(handler, slot, inSlot, carried);
        if (inSlot.isEmpty()) {
            if (handler.isItemValid(slot, carried)) {
                handler.setStackInSlot(slot, carried.copyWithCount(1));
                carried.shrink(1);
                player.containerMenu.setCarried(carried);
                return;
            }
            return;
        }
        if (handler.isItemValid(slot, carried) && ItemStack.isSameItemSameComponents(inSlot, carried) && inSlot.getCount() < limit) {
            inSlot.grow(1);
            carried.shrink(1);
            handler.setStackInSlot(slot, inSlot);
            player.containerMenu.setCarried(carried);
        }
    }

    private static void clickVirtualStack(ServerPlayer player, ItemStack[] slots, int slot, int button) {
        ItemStack carried = player.containerMenu.getCarried().copy();
        ItemStack inSlot = safe(slots[slot]).copy();
        if (button == 1) {
            if (carried.isEmpty()) {
                if (inSlot.isEmpty()) {
                    return;
                }
                int take = (inSlot.getCount() + 1) / 2;
                ItemStack picked = inSlot.copyWithCount(take);
                inSlot.shrink(take);
                slots[slot] = inSlot.isEmpty() ? ItemStack.EMPTY : inSlot;
                player.containerMenu.setCarried(picked);
                return;
            }
            if (inSlot.isEmpty()) {
                slots[slot] = carried.copyWithCount(1);
                carried.shrink(1);
                player.containerMenu.setCarried(carried);
                return;
            } else {
                if (ItemStack.isSameItemSameComponents(inSlot, carried) && inSlot.getCount() < Math.min(inSlot.getMaxStackSize(), carried.getMaxStackSize())) {
                    inSlot.grow(1);
                    carried.shrink(1);
                    slots[slot] = inSlot;
                    player.containerMenu.setCarried(carried);
                    return;
                }
                return;
            }
        }
        if (carried.isEmpty()) {
            slots[slot] = ItemStack.EMPTY;
            player.containerMenu.setCarried(inSlot);
            return;
        }
        if (inSlot.isEmpty()) {
            slots[slot] = carried;
            player.containerMenu.setCarried(ItemStack.EMPTY);
            return;
        }
        if (ItemStack.isSameItemSameComponents(inSlot, carried)) {
            int max = Math.min(inSlot.getMaxStackSize(), carried.getMaxStackSize());
            int move = Math.min(max - inSlot.getCount(), carried.getCount());
            if (move > 0) {
                inSlot.grow(move);
                carried.shrink(move);
                slots[slot] = inSlot;
                player.containerMenu.setCarried(carried);
                return;
            }
        }
        slots[slot] = carried;
        player.containerMenu.setCarried(inSlot);
    }

    private static void takeResultSlot(ServerPlayer player, ItemStack[] slots, int slot) {
        ItemStack result = safe(slots[slot]).copy();
        if (result.isEmpty()) {
            return;
        }
        ItemStack carried = player.containerMenu.getCarried().copy();
        if (canMergeCarried(carried, result)) {
            mergeIntoCarried(player, carried, result);
            slots[slot] = ItemStack.EMPTY;
        }
    }

    private static void takeCraftingResult(ServerPlayer player, UtilityState state) {
        ItemStack result = getCraftingResult(player, state);
        if (result.isEmpty()) {
            return;
        }
        ItemStack carried = player.containerMenu.getCarried().copy();
        if (canMergeCarried(carried, result)) {
            mergeIntoCarried(player, carried, result);
            consumeOneEach(state.crafting, 0, 9);
        }
    }

    private static ItemStack getCraftingResult(ServerPlayer player, UtilityState state) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < 9; i++) stacks.add(safe(state.crafting[i]).copy());
        CraftingInput input = CraftingInput.of(3, 3, stacks);
        return player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .map(holder -> holder.value().assemble(input, player.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static boolean tickFurnace(ServerPlayer player, UtilityState state) {
        int burn;
        boolean changed = false;
        ItemStack input = safe(state.furnace[0]);
        ItemStack fuel = safe(state.furnace[1]);
        ItemStack out = safe(state.furnace[2]);
        if (state.litTime > 0) {
            state.litTime--;
            changed = true;
        }
        RecipeHolder<?> recipe = input.isEmpty() ? null : getSmeltingRecipeHolder(player, input);
        ItemStack result = recipe == null ? ItemStack.EMPTY : assembleSingleRecipe(recipe, input, player);
        int recipeCookTime = recipe == null ? 200 : getRecipeCookingTime(recipe);
        if (state.cookTotal != recipeCookTime) {
            state.cookTotal = recipeCookTime;
            if (state.cookProgress > state.cookTotal) {
                state.cookProgress = state.cookTotal;
            }
            changed = true;
        }
        int resultLimit = result.isEmpty() ? 64 : result.getMaxStackSize();
        boolean canSmelt = !result.isEmpty() && (out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= resultLimit));
        if (state.litTime <= 0 && canSmelt && !fuel.isEmpty() && (burn = getBurnTime(fuel)) > 0) {
            state.litTime = burn;
            state.litDuration = burn;
            ItemStack remainder = getCraftingRemainder(fuel);
            fuel.shrink(1);
            state.furnace[1] = fuel.isEmpty() ? remainder : fuel;
            changed = true;
        }
        if (state.litTime > 0 && canSmelt) {
            state.cookProgress++;
            changed = true;
            if (state.cookProgress >= Math.max(1, state.cookTotal)) {
                state.cookProgress = 0;
                input.shrink(1);
                state.furnace[0] = input.isEmpty() ? ItemStack.EMPTY : input;
                if (out.isEmpty()) {
                    state.furnace[2] = result.copy();
                } else {
                    out.grow(result.getCount());
                    state.furnace[2] = out;
                }
            }
        } else if (state.cookProgress > 0) {
            state.cookProgress = Math.max(0, state.cookProgress - 2);
            changed = true;
        }
        return changed;
    }

    private static int getRecipeCookingTime(RecipeHolder<?> holder) {
        return holder.value() instanceof AbstractCookingRecipe cooking ? cooking.getCookingTime() : 200;
    }

    private static ItemStack getCraftingRemainder(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    private static int getBurnTime(ItemStack fuel) {
        return fuel.getBurnTime(RecipeType.SMELTING);
    }

    private static void takeAnvilResult(ServerPlayer player, UtilityState state) {
        ItemStack result = getAnvilResult(player, state);
        if (result.isEmpty()) {
            return;
        }
        int cost = Math.max(0, state.anvilCost);
        if (player.isCreative() || (cost < 40 && player.experienceLevel >= cost)) {
            ItemStack carried = player.containerMenu.getCarried().copy();
            if (canMergeCarried(carried, result)) {
                if (!player.isCreative() && cost > 0) {
                    player.giveExperienceLevels(-cost);
                }
                mergeIntoCarried(player, carried, result);
                consumeAnvilInputs(state);
                state.anvilCost = 0;
                state.anvilName = "";
                try {
                    player.level().playSound((Player) null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 0.9f + (player.getRandom().nextFloat() * 0.2f));
                } catch (Throwable th) {
                }
            }
        }
    }

    private static void consumeAnvilInputs(UtilityState state) {
        state.anvil[0] = ItemStack.EMPTY;
        ItemStack right = safe(state.anvil[1]);
        if (!right.isEmpty()) {
            int consume = Math.max(1, state.anvilMaterialCost);
            right.shrink(consume);
            state.anvil[1] = right.isEmpty() ? ItemStack.EMPTY : right;
            return;
        }
        state.anvil[1] = ItemStack.EMPTY;
    }

    private static ItemStack getAnvilResult(ServerPlayer player, UtilityState state) {
        AnvilCalc vanilla = getVanillaAnvilResult(player, state);
        if (!vanilla.result().isEmpty()) {
            state.anvilCost = Math.max(1, vanilla.cost());
            state.anvilMaterialCost = Math.max(1, vanilla.materialCost());
            return (player.isCreative() || state.anvilCost < 40) ? vanilla.result() : ItemStack.EMPTY;
        }
        ItemStack left = safe(state.anvil[0]);
        ItemStack right = safe(state.anvil[1]);
        if (left.isEmpty()) {
            state.anvilCost = 0;
            state.anvilMaterialCost = 0;
            return ItemStack.EMPTY;
        }
        ItemStack result = left.copy();
        int cost = 0;
        int materialCost = 0;
        boolean changed = false;
        if (!state.anvilName.isBlank() && !state.anvilName.equals(left.getHoverName().getString())) {
            result.set(DataComponents.CUSTOM_NAME, Component.literal(state.anvilName));
            cost = 0 + 1;
            changed = true;
        }
        if (!right.isEmpty() && left.isDamageableItem() && left.getItem() == right.getItem() && right.isDamageableItem()) {
            int max = result.getMaxDamage();
            int remain = (((max - left.getDamageValue()) + max) - right.getDamageValue()) + ((max * 5) / 100);
            result.setDamageValue(Math.max(0, max - Math.min(max, remain)));
            cost += 2;
            materialCost = 1;
            changed = true;
        }
        if (!changed) {
            state.anvilCost = 0;
            state.anvilMaterialCost = 0;
            return ItemStack.EMPTY;
        }
        result.setCount(1);
        state.anvilCost = Math.max(1, cost);
        state.anvilMaterialCost = materialCost;
        return (player.isCreative() || state.anvilCost < 40) ? result : ItemStack.EMPTY;
    }

    private static AnvilCalc getVanillaAnvilResult(ServerPlayer player, UtilityState state) {
        return AnvilCalc.EMPTY;
    }

    private static void takeSmithingResult(ServerPlayer player, UtilityState state) {
        ItemStack result = getSmithingResult(player, state);
        if (result.isEmpty()) {
            return;
        }
        ItemStack carried = player.containerMenu.getCarried().copy();
        if (canMergeCarried(carried, result)) {
            mergeIntoCarried(player, carried, result);
            consumeOneEach(state.smithing, 0, 3);
            try {
                player.level().playSound((Player) null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            } catch (Throwable th) {
            }
        }
    }

    private static ItemStack getSmithingResult(ServerPlayer player, UtilityState state) {
        SmithingRecipeInput input = new SmithingRecipeInput(safe(state.smithing[0]).copy(), safe(state.smithing[1]).copy(), safe(state.smithing[2]).copy());
        return player.level().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, player.level())
                .map(holder -> holder.value().assemble(input, player.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static RecipeHolder<?> getSmeltingRecipeHolder(ServerPlayer player, ItemStack inputStack) {
        return player.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(inputStack.copy()), player.level()).orElse(null);
    }

    private static ItemStack assembleSingleRecipe(RecipeHolder<?> holder, ItemStack inputStack, ServerPlayer player) {
        return ItemStack.EMPTY;
    }

    private static boolean canMergeCarried(ItemStack carried, ItemStack result) {
        return carried.isEmpty() || (ItemStack.isSameItemSameComponents(carried, result) && carried.getCount() + result.getCount() <= Math.min(carried.getMaxStackSize(), result.getMaxStackSize()));
    }

    private static void mergeIntoCarried(ServerPlayer player, ItemStack carried, ItemStack result) {
        if (carried.isEmpty()) {
            player.containerMenu.setCarried(result.copy());
        } else {
            carried.grow(result.getCount());
            player.containerMenu.setCarried(carried);
        }
    }

    private static void consumeOneEach(ItemStack[] slots, int start, int end) {
        for (int i = start; i < end && i < slots.length; i++) {
            ItemStack s = safe(slots[i]);
            if (!s.isEmpty()) {
                s.shrink(1);
                slots[i] = s.isEmpty() ? ItemStack.EMPTY : s;
            }
        }
    }

    private static void sendUtilitySync(ServerPlayer player, int type, UtilityState state) {
        List<ItemStack> stacks = new ArrayList<>();
        if (type == 0) {
            for (int i = 0; i < 9; i++) {
                stacks.add(safe(state.crafting[i]).copy());
            }
            stacks.add(getCraftingResult(player, state));
        } else if (type == 1) {
            for (int i2 = 0; i2 < 3; i2++) {
                stacks.add(safe(state.furnace[i2]).copy());
            }
        } else if (type == 2) {
            stacks.add(safe(state.anvil[0]).copy());
            stacks.add(safe(state.anvil[1]).copy());
            stacks.add(getAnvilResult(player, state));
        } else if (type == 3) {
            for (int i3 = 0; i3 < 3; i3++) {
                stacks.add(safe(state.smithing[i3]).copy());
            }
            stacks.add(getSmithingResult(player, state));
        }
        ModNetwork.sendUtilitySync(player, new UtilitySyncPayload(type, stacks, state.litTime, state.litDuration, state.cookProgress, state.cookTotal, state.anvilCost, state.anvilName));
    }

    private static Comparator<ItemStack> getComparator(int sortMode) {
        Comparator<ItemStack> byName = Comparator.comparing(s -> {
            return s.getHoverName().getString().toLowerCase(Locale.ROOT);
        });
        switch (sortMode) {
            case 0:
                return Comparator.comparingInt((ItemStack v0) -> {
                    return v0.getCount();
                }).reversed().thenComparing(byName);
            case 1:
                return Comparator.comparing((ItemStack s2) -> {
                    return BuiltInRegistries.ITEM.getKey(s2.getItem()).getNamespace();
                }).thenComparing(byName);
            case UTILITY_ANVIL:
                return byName;
            case UTILITY_SMITHING:
                return Comparator.comparing((ItemStack s3) -> {
                    return BuiltInRegistries.ITEM.getKey(s3.getItem()).toString();
                }).thenComparing(byName);
            default:
                return byName;
        }
    }

    // Gets the active backpack for the given player, check cache first
    private static Optional<BackpackAccess> getActiveBackpack(ServerPlayer player) {
        return BACKPACK_RESOLVER.resolve(player);
    }

    private static Optional<BackpackAccess> findCuriosBackpack(ServerPlayer player, int preferredIndex) {
        var curios = CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty()) return Optional.empty();
        int index = 0;
        for (var entry : curios.get().getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                ItemStack stack = stacks.getStackInSlot(slot);
                if (stack != null && !stack.isEmpty() && isSophisticatedBackpack(stack)
                        && (preferredIndex < 0 || preferredIndex == index)) {
                Optional<BackpackAccess> backpack = createBackpackAccess(-1000 - index, stack);
                if (backpack.isPresent()) return backpack;
            }
            index++;
            }
        }
        return Optional.empty();
    }

    private static Optional<BackpackAccess> getBackpack(ServerPlayer player, int inventorySlot) {
        Inventory inv = player.getInventory();
        if (inventorySlot < 0 || inventorySlot >= inv.items.size()) {
            return Optional.empty();
        }
        ItemStack stack = (ItemStack) inv.items.get(inventorySlot);
        return createBackpackAccess(inventorySlot, stack);
    }

    static Optional<BackpackAccess> createBackpackAccess(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isSophisticatedBackpack(stack)) return Optional.empty();
        IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
        return handler == null ? Optional.empty() : Optional.of(createBackpackAccess(slot, stack, handler));
    }

    private static BackpackAccess createBackpackAccess(int slot, ItemStack stack, IItemHandler handler) {
        int stackLimit = Math.max(getBackpackStackUpgradeLimit(stack), getBackpackStackUpgradeLimitFromContents(handler));
        IItemHandler effective = stackLimit > 64 ? new StackLimitItemHandler(handler, stackLimit) : handler;
        return new BackpackAccess(slot, stack, effective, stackLimit);
    }

    private static int getBackpackStackUpgradeLimitFromContents(IItemHandler handler) {
        if (handler == null) {
            return 64;
        }
        int best = 64;
        for (int i = 0; i < handler.getSlots(); i++) {
            try {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack != null && !stack.isEmpty()) {
                    String all = describeStack(stack).toLowerCase(Locale.ROOT);
                    if (all.contains("stack")) {
                        if (all.contains("tier_4") || all.contains("tier4") || all.contains("stack_upgrade_4")) {
                            best = Math.max(best, 1024);
                        } else if (all.contains("tier_3") || all.contains("tier3") || all.contains("stack_upgrade_3")) {
                            best = Math.max(best, 512);
                        } else if (all.contains("tier_2") || all.contains("tier2") || all.contains("stack_upgrade_2")) {
                            best = Math.max(best, 256);
                        } else {
                            best = (all.contains("tier_1") || all.contains("tier1") || all.contains("stack_upgrade_1")) ? Math.max(best, 128) : Math.max(best, 256);
                        }
                    }
                }
            } catch (Throwable th) {
            }
        }
        return best;
    }

    private static int getBackpackStackUpgradeLimit(ItemStack stack) {
        IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
        StringBuilder text = new StringBuilder(getStackDataText(stack));
        UpgradeHandler upgrades = wrapper.getUpgradeHandler();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            text.append(' ').append(describeStack(upgrades.getStackInSlot(i)));
        }
        String all = text.toString().toLowerCase(Locale.ROOT);
        if (!all.contains("stack")) {
            return 64;
        }
        if (all.contains("tier_4") || all.contains("tier4") || all.contains("stack_upgrade_4")) {
            return 1024;
        }
        if (all.contains("tier_3") || all.contains("tier3") || all.contains("stack_upgrade_3")) {
            return 512;
        }
        if (all.contains("tier_2") || all.contains("tier2") || all.contains("stack_upgrade_2")) {
            return 256;
        }
        if (all.contains("tier_1") || all.contains("tier1") || all.contains("stack_upgrade_1")) {
            return 128;
        }
        return all.contains("stack_upgrade") ? 256 : 64;
    }

    private static boolean isSophisticatedBackpack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("sophisticatedbackpacks", "backpacks")))) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && ("sophisticatedbackpacks".equals(id.getNamespace()) || id.getPath().contains("backpack"));
    }

    private static UpgradeFlags getUpgradeFlags(BackpackAccess bp) {
        boolean crafting = false;
        boolean furnace = false;
        boolean anvil = false;
        boolean smithing = false;
        List<ItemStack> upgradeStacks = getUpgradeStacks(bp.stack());
        Iterator<ItemStack> it = upgradeStacks.iterator();
        while (it.hasNext()) {
            String all = describeStack(it.next());
            crafting |= looksLikeCraftingUpgrade(all);
            furnace |= looksLikeFurnaceUpgrade(all);
            anvil |= looksLikeAnvilUpgrade(all);
            smithing |= looksLikeSmithingUpgrade(all);
        }
        String componentText = getStackDataText(bp.stack());
        boolean crafting2 = crafting | looksLikeCraftingUpgrade(componentText);
        boolean furnace2 = furnace | looksLikeFurnaceUpgrade(componentText);
        boolean anvil2 = anvil | looksLikeAnvilUpgrade(componentText);
        boolean smithing2 = smithing | looksLikeSmithingUpgrade(componentText);
        if (upgradeStacks.isEmpty() && (!crafting2 || !furnace2 || !anvil2 || !smithing2)) {
            for (int i = 0; i < bp.handler().getSlots(); i++) {
                ItemStack stack = bp.handler().getStackInSlot(i);
                if (stack != null && !stack.isEmpty()) {
                    String all2 = describeStack(stack);
                    if (looksLikeUtilityUpgrade(all2)) {
                        crafting2 |= looksLikeCraftingUpgrade(all2);
                        furnace2 |= looksLikeFurnaceUpgrade(all2);
                        anvil2 |= looksLikeAnvilUpgrade(all2);
                        smithing2 |= looksLikeSmithingUpgrade(all2);
                    }
                }
            }
        }
        return new UpgradeFlags(crafting2, furnace2, anvil2, smithing2);
    }

    private static List<ItemStack> getUpgradeStacks(ItemStack stack) {
        List<ItemStack> result = new ArrayList<>();
        UpgradeHandler upgrades = BackpackWrapper.fromStack(stack).getUpgradeHandler();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack upgrade = upgrades.getStackInSlot(i);
            if (!upgrade.isEmpty()) {
                result.add(upgrade.copy());
            }
        }
        return result;
    }

    private static boolean looksLikeUtilityUpgrade(String text) {
        String all = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return looksLikeCraftingUpgrade(all) || looksLikeFurnaceUpgrade(all) || looksLikeAnvilUpgrade(all) || looksLikeSmithingUpgrade(all);
    }

    private static boolean looksLikeCraftingUpgrade(String text) {
        String all = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return all.contains("crafting_upgrade") || all.contains("crafting") || all.contains("craft") || all.contains("workbench");
    }

    private static boolean looksLikeFurnaceUpgrade(String text) {
        String all = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return all.contains("smelting_upgrade") || all.contains("furnace_upgrade") || all.contains("smelt") || all.contains("furnace") || all.contains("smoking") || all.contains("blasting");
    }

    private static boolean looksLikeAnvilUpgrade(String text) {
        String all = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return all.contains("anvil_upgrade") || all.contains("anvil");
    }

    private static boolean looksLikeSmithingUpgrade(String text) {
        String all = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return all.contains("smithing_upgrade") || all.contains("smithing") || all.contains("smith");
    }

    static String describeStack(ItemStack stack) {
        return (stack == null || stack.isEmpty()) ? "" : String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())) + " " + stack.getHoverName().getString() + " " + getStackDataText(stack);
    }

    static String getStackDataText(ItemStack stack) {
        StringBuilder builder = new StringBuilder();
        try {
            builder.append(getCustomTag(stack));
        } catch (Throwable th) {
        }
        try {
            builder.append(' ').append(stack.getComponents());
        } catch (Throwable th2) {
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    public static void handleUtilityDoubleCollect(ServerPlayer player, int utilityType, int slot, ItemStack clientCarried) {
        applyCreativeCarried(player, clientCarried);
        Optional<BackpackAccess> access = getActiveBackpack(player);
        if (access.isEmpty()) {
            syncTo(player);
            return;
        }
        BackpackAccess bp = access.get();
        UpgradeFlags flags = FlagResolver.resolve(bp);
        if (!flags.allows(utilityType)) {
            syncTo(player);
            return;
        }
        UtilityState state = getUtilityState(bp.stack());
        ItemStack[] slots = getUtilityInputSlots(state, utilityType);
        if (slots.length == 0 || slot < 0 || slot >= slots.length) {
            sendUtilitySync(player, utilityType, state);
            return;
        }
        ItemStack carried = player.containerMenu.getCarried().copy();
        ItemStack sample = carried.isEmpty() ? safe(slots[slot]).copy() : carried.copy();
        if (sample.isEmpty()) {
            sendUtilitySync(player, utilityType, state);
            return;
        }
        int max = sample.getMaxStackSize();
        for (int i = 0; i < slots.length && (carried.isEmpty() || carried.getCount() < max); i++) {
            ItemStack stack = safe(slots[i]).copy();
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, sample)) {
                if (carried.isEmpty()) {
                    int take = Math.min(max, stack.getCount());
                    carried = stack.copyWithCount(take);
                    stack.shrink(take);
                } else {
                    int move = Math.min(max - carried.getCount(), stack.getCount());
                    carried.grow(move);
                    stack.shrink(move);
                }
                slots[i] = stack.isEmpty() ? ItemStack.EMPTY : stack;
            }
        }
        player.containerMenu.setCarried(carried);
        saveUtilityState(bp.stack(), state);
        touchBackpack(player, bp);
        sendFullMenu(player);
        syncTo(player);
        sendUtilitySync(player, utilityType, state);
    }

    public static void handleUtilityDragDistribute(ServerPlayer player, int utilityType, List<Integer> rawSlots, int button, ItemStack clientCarried) {
        applyCreativeCarried(player, clientCarried);
        Optional<BackpackAccess> access = getActiveBackpack(player);
        if (access.isEmpty() || rawSlots == null || rawSlots.isEmpty()) {
            syncTo(player);
            return;
        }
        BackpackAccess bp = access.get();
        UpgradeFlags flags = FlagResolver.resolve(bp);
        if (!flags.allows(utilityType)) {
            syncTo(player);
            return;
        }
        UtilityState state = getUtilityState(bp.stack());
        ItemStack[] slots = getUtilityInputSlots(state, utilityType);
        if (slots.length == 0) {
            sendUtilitySync(player, utilityType, state);
            return;
        }
        ItemStack carried = player.containerMenu.getCarried().copy();
        if (carried.isEmpty()) {
            sendUtilitySync(player, utilityType, state);
            return;
        }
        List<Integer> valid = rawSlots.stream().filter(i -> {
            return i != null && i.intValue() >= 0 && i.intValue() < slots.length;
        }).distinct().filter(i2 -> {
            return isUtilityDragInputSlot(utilityType, i2.intValue()) && canUtilityDragPlace(slots[i2.intValue()], carried);
        }).toList();
        if (valid.isEmpty()) {
            sendUtilitySync(player, utilityType, state);
            return;
        }
        if (button == 1) {
            Iterator<Integer> it = valid.iterator();
            while (it.hasNext()) {
                int i3 = it.next().intValue();
                if (carried.isEmpty()) {
                    break;
                }
                ItemStack in = safe(slots[i3]).copy();
                if (in.isEmpty()) {
                    slots[i3] = carried.copyWithCount(1);
                } else {
                    in.grow(1);
                    slots[i3] = in;
                }
                carried.shrink(1);
            }
        } else {
            int perSlot = Math.max(1, carried.getCount() / valid.size());
            Iterator<Integer> it2 = valid.iterator();
            while (it2.hasNext()) {
                int i4 = it2.next().intValue();
                if (carried.isEmpty()) {
                    break;
                }
                ItemStack in2 = safe(slots[i4]).copy();
                int limit = in2.isEmpty() ? carried.getMaxStackSize() : Math.min(in2.getMaxStackSize(), carried.getMaxStackSize());
                int room = in2.isEmpty() ? limit : limit - in2.getCount();
                int move = Math.min(Math.min(perSlot, room), carried.getCount());
                if (move > 0) {
                    if (in2.isEmpty()) {
                        slots[i4] = carried.copyWithCount(move);
                    } else {
                        in2.grow(move);
                        slots[i4] = in2;
                    }
                    carried.shrink(move);
                }
            }
        }
        player.containerMenu.setCarried(carried);
        saveUtilityState(bp.stack(), state);
        touchBackpack(player, bp);
        sendFullMenu(player);
        syncTo(player);
        sendUtilitySync(player, utilityType, state);
    }

    private static ItemStack[] getUtilityInputSlots(UtilityState state, int utilityType) {
        return UtilityStateStore.inputSlots(state, utilityType);
    }

    private static boolean isUtilityDragInputSlot(int utilityType, int slot) {
        switch (utilityType) {
            case 0:
                return slot >= 0 && slot < 9;
            case 1:
                return slot == 0 || slot == 1;
            case UTILITY_ANVIL:
                return slot == 0 || slot == 1;
            case UTILITY_SMITHING:
                return slot >= 0 && slot < 3;
            default:
                return false;
        }
    }

    private static boolean canUtilityDragPlace(ItemStack inSlot, ItemStack carried) {
        if (carried == null || carried.isEmpty()) {
            return false;
        }
        ItemStack inSlot2 = safe(inSlot);
        if (inSlot2.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(inSlot2, carried) && inSlot2.getCount() < Math.min(inSlot2.getMaxStackSize(), carried.getMaxStackSize());
    }

    public static void handleJeiPrefetch(ServerPlayer player, List<List<ItemStack>> ingredientGroups) {
        List<BackpackAccess> backpacks = BackpackResolver.getAllBackpacks(player);
        if (backpacks.isEmpty() || ingredientGroups == null || ingredientGroups.isEmpty()) {
            syncTo(player);
            return;
        }
        Optional<BackpackAccess> active = getActiveBackpack(player);
        if (active.isPresent()) {
            BackpackAccess bp = active.get();
            UpgradeFlags flags = FlagResolver.resolve(bp);
            if (ingredientGroups.size() > 4 && flags.crafting() && fillCraftingUtilityFromJei(player, bp, ingredientGroups, backpacks)) {
                syncTo(player);
                ModNetwork.sendJeiOpenCrafting(player);
                return;
            }
        }
        List<ItemStack> reserved = new ArrayList<>();
        boolean moved = false;
        for (List<ItemStack> rawGroup : ingredientGroups) {
            List<ItemStack> group = sanitizeIngredientGroup(rawGroup);
            if (!group.isEmpty()) {
                int required = getRequiredCount(group);
                int availableInPlayer = countAvailableInventoryMatch(player, group, reserved);
                int missing = Math.max(0, required - availableInPlayer);
                int i = 0;
                while (true) {
                    if (i < missing) {
                        PullResult pulled = pullOneMatchingFromAnyBackpack(backpacks, group);
                        if (pulled.stack().isEmpty()) {
                            break;
                        }
                        ItemStack left = insertIntoPlayerInventory(player, pulled.stack());
                        if (!left.isEmpty()) {
                            if (pulled.backpack().handler() instanceof IItemHandlerModifiable modifiable) {
                                ItemStack backLeft = insertIntoBackpack(pulled.backpack().handler(), modifiable, left);
                                if (!backLeft.isEmpty()) {
                                    player.drop(backLeft, false);
                                }
                            } else {
                                player.drop(left, false);
                            }
                        } else {
                            reserved.add(pulled.stack().copyWithCount(1));
                            touchBackpack(player, pulled.backpack());
                            moved = true;
                            i++;
                        }
                    }
                }
            }
        }
        if (moved) {
            sendFullMenu(player);
            ModNetwork.sendJeiRetry(player);
        }
        syncTo(player);
    }

    private static boolean fillCraftingUtilityFromJei(ServerPlayer player, BackpackAccess target, List<List<ItemStack>> rawGroups, List<BackpackAccess> backpacks) {
        if (target == null || !(target.handler() instanceof IItemHandlerModifiable)) {
            return false;
        }
        UtilityState state = getUtilityState(target.stack());
        List<List<ItemStack>> groups = new ArrayList<>();
        for (List<ItemStack> raw : rawGroups) {
            List<ItemStack> group = sanitizeIngredientGroup(raw);
            if (!group.isEmpty()) {
                groups.add(group);
            }
        }
        if (groups.isEmpty() || groups.size() > 9) {
            return false;
        }
        ItemStack[] old = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            old[i] = safe(state.crafting[i]).copy();
        }
        for (int i2 = 0; i2 < 9; i2++) {
            state.crafting[i2] = ItemStack.EMPTY;
        }
        for (ItemStack stack : old) {
            if (!stack.isEmpty()) {
                ItemStack left = insertIntoPlayerInventory(player, stack);
                if (!left.isEmpty()) {
                    if (target.handler() instanceof IItemHandlerModifiable modifiable) {
                        left = insertIntoBackpack(target.handler(), modifiable, left);
                    }
                }
                if (!left.isEmpty()) {
                    player.drop(left, false);
                }
            }
        }
        int start = 0;
        for (int i3 = 0; i3 < groups.size() && i3 < 9; i3++) {
            PullResult pulled = pullOneMatchingFromAnyBackpack(backpacks, groups.get(i3));
            ItemStack ingredient = pulled.stack();
            if (ingredient.isEmpty()) {
                ingredient = pullOneMatchingFromPlayerInventory(player, groups.get(i3));
            } else {
                touchBackpack(player, pulled.backpack());
            }
            if (ingredient.isEmpty()) {
                for (int sIdx = 0; sIdx < 9; sIdx++) {
                    ItemStack placed = safe(state.crafting[sIdx]);
                    if (!placed.isEmpty()) {
                        ItemStack left2 = insertIntoPlayerInventory(player, placed.copy());
                        if (!left2.isEmpty()) {
                            player.drop(left2, false);
                        }
                    }
                    state.crafting[sIdx] = ItemStack.EMPTY;
                }
                saveUtilityState(target.stack(), state);
                return false;
            }
            state.crafting[start + i3] = ingredient.copyWithCount(1);
        }
        saveUtilityState(target.stack(), state);
        touchBackpack(player, target);
        sendUtilitySync(player, 0, state);
        sendFullMenu(player);
        return true;
    }

    private static ItemStack pullOneMatchingFromPlayerInventory(ServerPlayer player, List<ItemStack> options) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack in = (ItemStack) player.getInventory().items.get(i);
            if (!in.isEmpty() && matchesAny(in, options)) {
                ItemStack pulled = in.copyWithCount(1);
                in.shrink(1);
                player.getInventory().items.set(i, in.isEmpty() ? ItemStack.EMPTY : in);
                return pulled;
            }
        }
        return ItemStack.EMPTY;
    }

    private static PullResult pullOneMatchingFromAnyBackpack(List<BackpackAccess> backpacks, List<ItemStack> options) {
        for (BackpackAccess bp : backpacks) {
            if (bp.handler() instanceof IItemHandlerModifiable modifiable) {
                ItemStack pulled = pullOneMatchingFromBackpack(bp.handler(), modifiable, options);
                if (!pulled.isEmpty()) {
                    return new PullResult(bp, pulled);
                }
            }
        }
        return new PullResult(null, ItemStack.EMPTY);
    }

    private static List<ItemStack> sanitizeIngredientGroup(List<ItemStack> rawGroup) {
        List<ItemStack> out = new ArrayList<>();
        if (rawGroup == null) {
            return out;
        }
        for (ItemStack option : rawGroup) {
            if (option != null && !option.isEmpty()) {
                ItemStack copy = option.copy();
                if (copy.getCount() <= 0) {
                    copy.setCount(1);
                }
                out.add(copy);
            }
        }
        return out;
    }

    private static int getRequiredCount(List<ItemStack> options) {
        int required = 1;
        for (ItemStack option : options) {
            if (option != null && !option.isEmpty()) {
                required = Math.max(required, option.getCount());
            }
        }
        return required;
    }

    private static int countAvailableInventoryMatch(ServerPlayer player, List<ItemStack> options, List<ItemStack> reserved) {
        int count = 0;
        Iterator it = player.getInventory().items.iterator();
        while (it.hasNext()) {
            ItemStack stack = (ItemStack) it.next();
            if (!stack.isEmpty() && matchesAny(stack, options)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack2 : reserved) {
            if (!stack2.isEmpty() && matchesAny(stack2, options)) {
                count += stack2.getCount();
            }
        }
        return count;
    }

    private static boolean matchesAny(ItemStack stack, List<ItemStack> options) {
        for (ItemStack option : options) {
            if (option != null && !option.isEmpty() && stack.getItem() == option.getItem()) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack pullOneMatchingFromBackpack(IItemHandler handler, IItemHandlerModifiable modifiable, List<ItemStack> options) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack inSlot = handler.getStackInSlot(i).copy();
            if (!inSlot.isEmpty() && matchesAny(inSlot, options)) {
                ItemStack pulled = inSlot.copyWithCount(1);
                inSlot.shrink(1);
                modifiable.setStackInSlot(i, inSlot.isEmpty() ? ItemStack.EMPTY : inSlot);
                return pulled;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack insertIntoBackpack(IItemHandler handler, IItemHandlerModifiable modifiable, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            ItemStack in = handler.getStackInSlot(i).copy();
            if (handler.isItemValid(i, remaining) && !in.isEmpty() && ItemStack.isSameItemSameComponents(in, remaining)) {
                int limit = getSlotLimit(handler, i, in, remaining);
                int move = Math.min(Math.max(0, limit - in.getCount()), remaining.getCount());
                if (move > 0) {
                    in.grow(move);
                    remaining.shrink(move);
                    modifiable.setStackInSlot(i, in);
                }
            }
        }
        for (int i2 = 0; i2 < handler.getSlots() && !remaining.isEmpty(); i2++) {
            if (handler.getStackInSlot(i2).isEmpty() && handler.isItemValid(i2, remaining)) {
                int limit2 = getSlotLimit(handler, i2, ItemStack.EMPTY, remaining);
                int move2 = Math.min(limit2, remaining.getCount());
                modifiable.setStackInSlot(i2, remaining.copyWithCount(move2));
                remaining.shrink(move2);
            }
        }
        return remaining;
    }

    private static ItemStack insertIntoPlayerInventory(ServerPlayer player, ItemStack stack) {
        ItemStack remaining = stack.copy();
        boolean changed = false;
        for (int i = 0; i < player.getInventory().items.size() && !remaining.isEmpty(); i++) {
            ItemStack in = (ItemStack) player.getInventory().items.get(i);
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, remaining)) {
                int limit = Math.min(in.getMaxStackSize(), remaining.getMaxStackSize());
                int move = Math.min(Math.max(0, limit - in.getCount()), remaining.getCount());
                if (move > 0) {
                    in.grow(move);
                    remaining.shrink(move);
                    player.getInventory().items.set(i, in);
                    changed = true;
                }
            }
        }
        for (int i2 = 0; i2 < player.getInventory().items.size() && !remaining.isEmpty(); i2++) {
            if (((ItemStack) player.getInventory().items.get(i2)).isEmpty()) {
                int move2 = Math.min(Math.max(1, remaining.getMaxStackSize()), remaining.getCount());
                player.getInventory().items.set(i2, remaining.copyWithCount(move2));
                remaining.shrink(move2);
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setChanged();
        }
        return remaining;
    }

    private static void touchBackpack(ServerPlayer player, BackpackAccess access) {
        try {
            player.getInventory().setChanged();
            if (access.playerInventorySlot() >= 0 && access.playerInventorySlot() < player.getInventory().items.size()) {
                player.getInventory().items.set(access.playerInventorySlot(), access.stack());
            }
        } catch (Throwable th) {
        }
    }

    private static void sendFullMenu(ServerPlayer player) {
        try {
            player.containerMenu.broadcastChanges();
        } catch (Throwable th) {
        }
    }

    private static UtilityState getUtilityState(ItemStack stack) {
        String key = getUtilityKey(stack);
        return UTILITY_STATES.computeIfAbsent(key, k -> {
            return loadUtilityState(stack);
        });
    }

    private static String getUtilityKey(ItemStack stack) {
        CompoundTag tag = getCustomTag(stack);
        if (!tag.hasUUID("BackpackSideGuiUtilityId")) {
            tag.putUUID("BackpackSideGuiUtilityId", UUID.randomUUID());
            setCustomTag(stack, tag);
        }
        return tag.getUUID("BackpackSideGuiUtilityId").toString();
    }

    private static UtilityState loadUtilityState(ItemStack stack) {
        UtilityState state = new UtilityState();
        CompoundTag root = getCustomTag(stack).getCompound("BackpackSideGuiUtility");
        readStackArray(root, "Crafting", state.crafting);
        readStackArray(root, "Furnace", state.furnace);
        readStackArray(root, "Anvil", state.anvil);
        readStackArray(root, "Smithing", state.smithing);
        state.litTime = root.getInt("LitTime");
        state.litDuration = root.getInt("LitDuration");
        state.cookProgress = root.getInt("CookProgress");
        state.cookTotal = root.contains("CookTotal") ? root.getInt("CookTotal") : 200;
        state.anvilName = root.getString("AnvilName");
        state.anvilCost = root.getInt("AnvilCost");
        state.anvilMaterialCost = root.getInt("AnvilMaterialCost");
        return state;
    }

    private static void saveUtilityState(ItemStack stack, UtilityState state) {
        CompoundTag tag = getCustomTag(stack);
        CompoundTag root = new CompoundTag();
        writeStackArray(root, "Crafting", state.crafting);
        writeStackArray(root, "Furnace", state.furnace);
        writeStackArray(root, "Anvil", state.anvil);
        writeStackArray(root, "Smithing", state.smithing);
        root.putInt("LitTime", state.litTime);
        root.putInt("LitDuration", state.litDuration);
        root.putInt("CookProgress", state.cookProgress);
        root.putInt("CookTotal", state.cookTotal <= 0 ? 200 : state.cookTotal);
        root.putString("AnvilName", state.anvilName == null ? "" : state.anvilName);
        root.putInt("AnvilCost", state.anvilCost);
        root.putInt("AnvilMaterialCost", state.anvilMaterialCost);
        tag.put("BackpackSideGuiUtility", root);
        setCustomTag(stack, tag);
    }

    private static CompoundTag getCustomTag(ItemStack stack) {
        try {
            return ((CustomData) stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
        } catch (Throwable th) {
            return new CompoundTag();
        }
    }

    private static void setCustomTag(ItemStack stack, CompoundTag tag) {
        try {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        } catch (Throwable th) {
        }
    }

    private static void readStackArray(CompoundTag root, String key, ItemStack[] target) {
        ListTag list = root.getList(key, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            int slot = e.getInt("Slot");
            if (slot >= 0 && slot < target.length) {
                target[slot] = loadBasicStack(e);
            }
        }
    }

    private static void writeStackArray(CompoundTag root, String key, ItemStack[] source) {
        ListTag list = new ListTag();
        for (int i = 0; i < source.length; i++) {
            ItemStack stack = safe(source[i]);
            if (!stack.isEmpty()) {
                CompoundTag e = new CompoundTag();
                e.putInt("Slot", i);
                e.putString("Id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                e.putInt("Count", stack.getCount());
                list.add(e);
            }
        }
        root.put(key, list);
    }

    private static ItemStack loadBasicStack(CompoundTag e) {
        try {
            ResourceLocation id = ResourceLocation.parse(e.getString("Id"));
            Item item = (Item) BuiltInRegistries.ITEM.get(id);
            return (item == null || item == Items.AIR) ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, e.getInt("Count")));
        } catch (Throwable th) {
            return ItemStack.EMPTY;
        }
    }

    private static void handleLostUtilityUpgrades(ServerPlayer player, BackpackAccess bp, UpgradeFlags flags) {
        UtilityState state = getUtilityState(bp.stack());
        boolean changed = false;
        if (!flags.crafting()) {
            changed = false | flush(player, state.crafting);
        }
        if (!flags.furnace()) {
            changed |= flush(player, state.furnace);
            state.cookProgress = 0;
            state.litDuration = 0;
            state.litTime = 0;
            state.cookTotal = 200;
        }
        if (!flags.anvil()) {
            changed |= flush(player, state.anvil);
        }
        if (!flags.smithing()) {
            changed |= flush(player, state.smithing);
        }
        if (changed) {
            saveUtilityState(bp.stack(), state);
            touchBackpack(player, bp);
        }
    }

    private static boolean flush(ServerPlayer player, ItemStack[] slots) {
        boolean changed = false;
        for (int i = 0; i < slots.length; i++) {
            ItemStack s = safe(slots[i]);
            if (!s.isEmpty()) {
                ItemStack left = insertIntoPlayerInventory(player, s.copy());
                if (!left.isEmpty()) {
                    player.drop(left, false);
                }
                slots[i] = ItemStack.EMPTY;
                changed = true;
            }
        }
        return changed;
    }

    private static String sanitizeName(String text) {
        if (text == null) {
            return "";
        }
        String text2 = text.trim();
        return text2.length() > 50 ? text2.substring(0, 50) : text2;
    }

    private static ItemStack safe(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack;
    }
}



