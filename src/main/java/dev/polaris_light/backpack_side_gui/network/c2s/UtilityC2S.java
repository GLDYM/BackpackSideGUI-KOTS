package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.Optional;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackAvailabilityPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.UtilityFlagsPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.FlagResolver;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import dev.polaris_light.backpack_side_gui.server.record.UpgradeFlags;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class UtilityC2S {
    private UtilityC2S() {
    }

    public static void open(ServerPlayer player) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new BackpackAvailabilityPayload(java.util.List.of()),
                    new CustomPacketPayload[0]);
            PacketDistributor.sendToPlayer(player, new BackpackSyncPayload("", java.util.List.of()),
                    new CustomPacketPayload[0]);
        } else {
            PacketDistributor.sendToPlayer(player, availability(player), new CustomPacketPayload[0]);
            sendFlags(player, access.get());
            PacketDistributor.sendToPlayer(player, dev.polaris_light.backpack_side_gui.server.sync.BackpackSyncService.snapshot(access.get(), player),
                    new CustomPacketPayload[0]);
            if (FlagResolver.resolve(access.get()).furnace())
                FurnaceC2S.send(player, access.get());
        }
    }

    private static BackpackAvailabilityPayload availability(ServerPlayer player) {
        java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
        for (BackpackAccess access : BackpackResolver.getAllBackpacks(player)) {
            for (int i = 0; i < access.handler().getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = access.handler().getStackInSlot(i);
                if (!stack.isEmpty())
                    items.add(stack.copy());
            }
        }
        return new BackpackAvailabilityPayload(items);
    }

    public static void request(ServerPlayer player, int type) {
        if (type < 0 || type >= 5)
            return;
        BackpackResolver.resolve(player).ifPresent(access -> {
            UpgradeFlags flags = FlagResolver.resolve(access);
            boolean exist = switch (type) {
                case 0 -> flags.crafting();
                case 1 -> flags.furnace();
                case 2 -> flags.anvil();
                case 3 -> flags.smithing();
                case 4 -> flags.stonecutter();
                default -> false;
            };
            if (exist) {
                player.containerMenu.broadcastChanges();
                if (type == 3)
                    SmithingC2S.send(player, access);
                if (type == 0)
                    CraftingC2S.send(player, access);
                if (type == 2)
                    AnvilC2S.send(player, access);
                if (type == 1)
                    FurnaceC2S.send(player, access);
                if (type == 4)
                    StonecutterC2S.send(player, access);
            }
        });
    }

    public static void sendFlags(ServerPlayer player, BackpackAccess access) {
        UpgradeFlags flags = FlagResolver.resolve(access);
        PacketDistributor.sendToPlayer(player,
                new UtilityFlagsPayload(flags.crafting(), flags.furnace(), flags.anvil(), flags.smithing(),
                        flags.stonecutter()),
                new CustomPacketPayload[0]);
    }
}
