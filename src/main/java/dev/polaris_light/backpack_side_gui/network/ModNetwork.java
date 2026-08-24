package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import dev.polaris_light.backpack_side_gui.client.SideBackpackClient;
import java.util.ArrayList;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;
import dev.polaris_light.backpack_side_gui.server.BackpackVirtualSlot;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import dev.polaris_light.backpack_side_gui.server.FlagResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.smithing.SmithingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.CraftingInput;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        var resolved = BackpackResolver.resolve(player);
                        if (resolved.isEmpty()) {
                            PacketDistributor.sendToPlayer(player, new BackpackSyncPayload("", java.util.List.of()),
                                    new CustomPacketPayload[0]);
                        } else
                            resolved.ifPresent(access -> {
                                sendUtilityFlags(player, access);
                                var stacks = new ArrayList<ItemStack>();
                                for (int i = 0; i < access.handler().getSlots(); i++)
                                    stacks.add(access.handler().getStackInSlot(i).copy());
                                PacketDistributor.sendToPlayer(player,
                                        new BackpackSyncPayload(access.stack().getHoverName().getString(), stacks),
                                        new CustomPacketPayload[0]);
                            });
                    }
                }));
        registrar.playToClient(BackpackSyncPayload.TYPE, BackpackSyncPayload.STREAM_CODEC,
                (payload, context) -> context
                        .enqueueWork(() -> SideBackpackClient.receive(payload.title(), payload.items())));
        registrar.playToClient(UtilityFlagsPayload.TYPE, UtilityFlagsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SideBackpackClient.receiveUtilityFlags(payload)));
        registrar.playToClient(SmithingSyncPayload.TYPE, SmithingSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SideBackpackClient.receiveSmithing(payload)));
        registrar.playToClient(CraftingSyncPayload.TYPE, CraftingSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SideBackpackClient.receiveCrafting(payload)));
        registrar.playToServer(CraftingClickPayload.TYPE, CraftingClickPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer p) handleCraftingClick(p, payload); }));
        registrar.playToServer(SmithingClickPayload.TYPE, SmithingClickPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer p)
                        handleSmithingClick(p, payload);
                }));
        registrar.playToServer(BackpackSlotPayload.TYPE, BackpackSlotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player)
                        handleSlot(player, payload);
                }));
        registrar.playToClient(BackpackCarriedPayload.TYPE, BackpackCarriedPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SideBackpackClient.receiveCarried(payload.carried())));
        registrar.playToServer(SortPayload.TYPE, SortPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player)
                        sort(player, payload.sortMode());
                }));
        registrar.playToServer(UtilityRequestPayload.TYPE, UtilityRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && payload.utilityType() >= 0
                            && payload.utilityType() < 5)
                        BackpackResolver.resolve(player).ifPresent(access -> {
                            var flags = FlagResolver.resolve(access);
                            boolean allowed = switch (payload.utilityType()) {
                                case 0 -> flags.crafting();
                                case 1 -> flags.furnace();
                                case 2 -> flags.anvil();
                                case 3 -> flags.smithing();
                                case 4 -> flags.stonecutter();
                                default -> false;
                            };
                            if (allowed) {
                                player.containerMenu.broadcastChanges();
                                if (payload.utilityType() == 3)
                                    sendSmithing(player, access);
                                if (payload.utilityType() == 0)
                                    sendCrafting(player, access);
                            }
                        });
                }));
    }

    public static void requestOpen() {
        PacketDistributor.sendToServer(new OpenBackpackPayload(), new CustomPacketPayload[0]);
    }

    public static void requestSlot(int slot, int clickType, ItemStack carried) {
        PacketDistributor.sendToServer(new BackpackSlotPayload(slot, clickType, carried.copy()),
                new CustomPacketPayload[0]);
    }

    private static void handleSlot(ServerPlayer player, BackpackSlotPayload p) {
        var access = BackpackResolver.resolve(player);
        if (access.isEmpty() || p.slot() < 0 || p.slot() >= access.get().handler().getSlots())
            return;
        ItemStack carried = player.containerMenu.getCarried();
        if (!player.gameMode.isCreative() && !ItemStack.matches(carried, p.carried()))
            return;
        if (player.gameMode.isCreative() && !ItemStack.matches(carried, p.carried()))
            carried = p.carried().copy();
        var slot = new BackpackVirtualSlot(access.get().stack(), p.slot(), player);
        if (p.clickType() == 4 || p.clickType() == 5) {
            if (!carried.isEmpty())
                return;
            ItemStack picked = slot.getItem().copy();
            int cursorLimit = Math.max(1, picked.getMaxStackSize());
            int amount = p.clickType() == 5
                    ? Math.min(cursorLimit, (picked.getCount() + 1) / 2)
                    : Math.min(cursorLimit, picked.getCount());
            ItemStack moved = slot.remove(amount);
            if (!player.getInventory().add(moved))
                slot.set(moved);
        } else if (p.clickType() <= 1) {
            if (!carried.isEmpty())
                return;
            ItemStack in = slot.getItem();
            int cursorLimit = Math.max(1, in.getMaxStackSize());
            int max = Math.min(cursorLimit, in.getCount());
            int amount = p.clickType() == 1 ? Math.min(max, (in.getCount() + 1) / 2) : max;
            ItemStack picked = slot.isInfinite()
                    ? slot.remove(amount)
                    : slot.safeTake(amount, amount, player);
            player.containerMenu.setCarried(picked);
        } else {
            if (carried.isEmpty() || !slot.mayPlace(carried))
                return;
            int amount = p.clickType() == 2 ? 1 : carried.getCount();
            ItemStack rest = slot.safeInsert(carried.copyWithCount(amount));
            player.containerMenu.setCarried(rest.isEmpty() ? ItemStack.EMPTY : rest);
        }
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()),
                new CustomPacketPayload[0]);
        PacketDistributor.sendToPlayer(player, snapshot(access.get()), new CustomPacketPayload[0]);
    }

    public static void requestSort(int mode) {
        PacketDistributor.sendToServer(new SortPayload(mode), new CustomPacketPayload[0]);
    }

    private static void sendUtilityFlags(ServerPlayer player, BackpackAccess access) {
        var flags = FlagResolver.resolve(access);
        PacketDistributor.sendToPlayer(player, new UtilityFlagsPayload(flags.crafting(), flags.furnace(), flags.anvil(),
                flags.smithing(), flags.stonecutter()), new CustomPacketPayload[0]);
    }

    public static void requestUtility(int utilityType) {
        PacketDistributor.sendToServer(new UtilityRequestPayload(utilityType), new CustomPacketPayload[0]);
    }

    public static void requestSmithingClick(int slot, int button, ItemStack carried) {
        PacketDistributor.sendToServer(new SmithingClickPayload(slot, button, carried.copy()));
    }

    private static void handleSmithingClick(ServerPlayer player, SmithingClickPayload p) {
        var access = BackpackResolver.resolve(player);
        if (access.isEmpty())
            return;
        var wrappers = net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper
                .fromStack(access.get().stack()).getUpgradeHandler()
                .getWrappersThatImplement(SmithingUpgradeWrapper.class);
        if (wrappers.isEmpty())
            return;
        var inv = wrappers.get(0).getInventory();
        if (p.slot() < 0 || p.slot() > inv.getSlots())
            return;
        var carried = player.containerMenu.getCarried();
        if (!ItemStack.matches(carried, p.carried())) {
            if (!player.gameMode.isCreative())
                return;
            carried = p.carried().copy();
            player.containerMenu.setCarried(carried);
        }
        if (p.slot() == 3) {
            var input = new SmithingRecipeInput(inv.getStackInSlot(0).copy(), inv.getStackInSlot(1).copy(),
                    inv.getStackInSlot(2).copy());
            var result = player.level().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, player.level())
                    .map(h -> h.value().assemble(input, player.registryAccess())).orElse(ItemStack.EMPTY);
            if (!result.isEmpty() && carried.isEmpty()) {
                player.containerMenu.setCarried(result);
                for (int i = 0; i < 3; i++)
                    inv.extractItem(i, 1, false);
            }
            player.containerMenu.broadcastChanges();
            PacketDistributor.sendToPlayer(player,
                    new BackpackCarriedPayload(player.containerMenu.getCarried().copy()));
            sendSmithing(player, access.get());
            return;
        }
        var current = inv.getStackInSlot(p.slot());
        if (carried.isEmpty()) {
            player.containerMenu.setCarried(
                    inv.extractItem(p.slot(), p.button() == 1 ? current.getCount() / 2 : current.getCount(), false));
        } else {
            player.containerMenu.setCarried(inv.insertItem(p.slot(), carried, false));
        }
        access.get().stack().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM);
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()));
        sendSmithing(player, access.get());
    }

    private static void sendSmithing(ServerPlayer player, BackpackAccess access) {
        var wrappers = net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper.fromStack(access.stack())
                .getUpgradeHandler().getWrappersThatImplement(SmithingUpgradeWrapper.class);
        if (wrappers.isEmpty())
            return;
        var inv = wrappers.get(0).getInventory();
        var input = new SmithingRecipeInput(inv.getStackInSlot(0).copy(), inv.getStackInSlot(1).copy(),
                inv.getStackInSlot(2).copy());
        var result = player.level().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, player.level())
                .map(holder -> holder.value().assemble(input, player.registryAccess())).orElse(ItemStack.EMPTY);
        PacketDistributor.sendToPlayer(player,
                new SmithingSyncPayload(inv.getStackInSlot(0), inv.getStackInSlot(1), inv.getStackInSlot(2), result));
    }

    public static void requestCraftingClick(int slot, int button, boolean shift, ItemStack carried) {
        PacketDistributor.sendToServer(new CraftingClickPayload(slot, button, shift, carried.copy()));
    }

    private static void handleCraftingClick(ServerPlayer player, CraftingClickPayload p) {
        var access = BackpackResolver.resolve(player); if (access.isEmpty() || p.slot() < 0 || p.slot() > 9) return;
        var wrappers = net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper.fromStack(access.get().stack()).getUpgradeHandler().getWrappersThatImplement(CraftingUpgradeWrapper.class);
        if (wrappers.isEmpty()) return;
        var inv = wrappers.get(0).getInventory(); var serverCarried = player.containerMenu.getCarried();
        if (!ItemStack.matches(serverCarried, p.carried())) { if (!player.gameMode.isCreative()) return; serverCarried = p.carried().copy(); player.containerMenu.setCarried(serverCarried); }
        if (p.slot() == 9) {
            ItemStack[] inputs = new ItemStack[9];
            for (int i = 0; i < 9; i++) inputs[i] = inv.getStackInSlot(i).copy();
            var input = CraftingInput.of(3, 3, java.util.Arrays.asList(inputs));
            var result = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level())
                    .map(holder -> holder.value().assemble(input, player.registryAccess())).orElse(ItemStack.EMPTY);
            if (!result.isEmpty()) {
                if (p.shift()) {
                    while (!result.isEmpty() && canInsertCraftResult(player, result)) {
                        if (!player.getInventory().add(result.copy())) break;
                        consumeCraftingInputs(inv);
                        result = getCraftingResult(player, inv);
                    }
                } else if (serverCarried.isEmpty()) {
                    player.containerMenu.setCarried(result.copy());
                    consumeCraftingInputs(inv);
                } else if (ItemStack.isSameItemSameComponents(serverCarried, result)
                        && serverCarried.getCount() + result.getCount() <= serverCarried.getMaxStackSize()) {
                    serverCarried.grow(result.getCount());
                    player.containerMenu.setCarried(serverCarried);
                    consumeCraftingInputs(inv);
                }
            }
        } else if (serverCarried.isEmpty()) player.containerMenu.setCarried(inv.extractItem(p.slot(), p.button() == 1 ? Math.max(1, inv.getStackInSlot(p.slot()).getCount() / 2) : inv.getStackInSlot(p.slot()).getCount(), false));
        else player.containerMenu.setCarried(inv.insertItem(p.slot(), serverCarried, false));
        player.containerMenu.broadcastChanges(); PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy())); sendCrafting(player, access.get());
    }

    private static void consumeCraftingInputs(net.neoforged.neoforge.items.IItemHandler inv) {
        for (int i = 0; i < 9; i++) if (!inv.getStackInSlot(i).isEmpty()) inv.extractItem(i, 1, false);
    }

    private static ItemStack getCraftingResult(ServerPlayer player, net.neoforged.neoforge.items.IItemHandler inv) {
        ItemStack[] items = new ItemStack[9];
        for (int i = 0; i < 9; i++) items[i] = inv.getStackInSlot(i).copy();
        var input = CraftingInput.of(3, 3, java.util.Arrays.asList(items));
        return player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .map(h -> h.value().assemble(input, player.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static boolean canInsertCraftResult(ServerPlayer player, ItemStack result) {
        int capacity = 0;
        for (ItemStack slot : player.getInventory().items) {
            if (slot.isEmpty()) capacity += result.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(slot, result)) capacity += Math.max(0, slot.getMaxStackSize() - slot.getCount());
            if (capacity >= result.getCount()) return true;
        }
        return false;
    }

    private static void sendCrafting(ServerPlayer player, BackpackAccess access) {
        var wrappers = net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper.fromStack(access.stack())
                .getUpgradeHandler().getWrappersThatImplement(CraftingUpgradeWrapper.class);
        if (wrappers.isEmpty()) return;
        var inv = wrappers.get(0).getInventory();
        ItemStack[] items = new ItemStack[10];
        for (int i = 0; i < 9; i++) items[i] = inv.getStackInSlot(i).copy();
        var input = CraftingInput.of(3, 3, java.util.Arrays.asList(items).subList(0, 9));
        items[9] = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .map(holder -> holder.value().assemble(input, player.registryAccess())).orElse(ItemStack.EMPTY);
        PacketDistributor.sendToPlayer(player, new CraftingSyncPayload(items));
    }

    private static void sort(ServerPlayer player, int mode) {
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
        Comparator<ItemStack> name = Comparator
                .comparing(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT));
        if (mode == 0)
            merged.sort(Comparator.comparingInt(ItemStack::getCount).reversed()
                    .thenComparing(name));
        else if (mode == 1)
            merged.sort(Comparator.comparing(
                    (ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace())
                    .thenComparing(name));
        else if (mode == 2)
            merged.sort(name);
        else
            merged.sort(Comparator.comparing(
                    (ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                    .thenComparing(name));
        for (int i = 0; i < merged.size() && i < inv.getSlots(); i++)
            inv.setStackInSlot(i, merged.get(i));
        PacketDistributor.sendToPlayer(player, snapshot(access.get()), new CustomPacketPayload[0]);
    }

    private static BackpackSyncPayload snapshot(BackpackAccess a) {
        var s = new ArrayList<ItemStack>();
        for (int i = 0; i < a.handler().getSlots(); i++) {
            var stack = a.handler().getStackInSlot(i).copy();
            s.add(stack);
        }
        return new BackpackSyncPayload(a.stack().getHoverName().getString(), s);
    }
}
