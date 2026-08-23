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
                                var stacks = new ArrayList<net.minecraft.world.item.ItemStack>();
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
        registrar.playToServer(SortPayload.TYPE, SortPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player)
                        sort(player, payload.sortMode());
                }));
    }

    public static void requestOpen() {
        PacketDistributor.sendToServer(new OpenBackpackPayload(), new CustomPacketPayload[0]);
    }
    public static void requestSort(int mode) { PacketDistributor.sendToServer(new SortPayload(mode), new CustomPacketPayload[0]); }

    private static void sort(ServerPlayer player, int mode) {
        var access = BackpackResolver.resolve(player);
        if (access.isEmpty() || !(access.get().handler() instanceof IItemHandlerModifiable inv)) return;
        var stacks = new ArrayList<net.minecraft.world.item.ItemStack>();
        for (int i = 0; i < inv.getSlots(); i++) { var s = inv.getStackInSlot(i).copy(); if (!s.isEmpty()) stacks.add(s); inv.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY); }
        var merged = new ArrayList<net.minecraft.world.item.ItemStack>();
        for (var s : stacks) { var left = s.copy(); for (var e : merged) if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(e, left)) { int n = Math.min(left.getCount(), Math.max(0, Math.max(access.get().stackLimit(), e.getMaxStackSize()) - e.getCount())); e.grow(n); left.shrink(n); if (left.isEmpty()) break; } while (!left.isEmpty()) { var part = left.copy(); part.setCount(Math.min(left.getCount(), Math.max(access.get().stackLimit(), left.getMaxStackSize()))); merged.add(part); left.shrink(part.getCount()); } }
        Comparator<net.minecraft.world.item.ItemStack> name = Comparator.comparing(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT));
        if (mode == 0) merged.sort(Comparator.comparingInt(net.minecraft.world.item.ItemStack::getCount).reversed().thenComparing(name));
        else if (mode == 1) merged.sort(Comparator.comparing((net.minecraft.world.item.ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace()).thenComparing(name));
        else if (mode == 2) merged.sort(name);
        else merged.sort(Comparator.comparing((net.minecraft.world.item.ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()).thenComparing(name));
        for (int i = 0; i < merged.size() && i < inv.getSlots(); i++) inv.setStackInSlot(i, merged.get(i));
        PacketDistributor.sendToPlayer(player, snapshot(access.get()), new CustomPacketPayload[0]);
    }

    private static BackpackSyncPayload snapshot(dev.polaris_light.backpack_side_gui.server.record.BackpackAccess a) { var s = new ArrayList<net.minecraft.world.item.ItemStack>(); for (int i=0;i<a.handler().getSlots();i++) s.add(a.handler().getStackInSlot(i).copy()); return new BackpackSyncPayload(a.stack().getHoverName().getString(), s); }
}
