package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.ArrayList;
import java.util.Optional;
import dev.polaris_light.backpack_side_gui.server.record.UpgradeFlags;
import dev.polaris_light.backpack_side_gui.network.payload.*;
import dev.polaris_light.backpack_side_gui.server.*;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class UtilityC2S {
    private UtilityC2S() {
    }

    public static void open(ServerPlayer player) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new BackpackSyncPayload("", java.util.List.of()),
                    new CustomPacketPayload[0]);
        } else {
            sendFlags(player, access.get());
            ArrayList<ItemStack> items = new ArrayList<>();
            for (int slot = 0; slot < access.get().handler().getSlots(); slot++)
                items.add(access.get().handler().getStackInSlot(slot).copy());
            PacketDistributor.sendToPlayer(player,
                    new BackpackSyncPayload(access.get().stack().getHoverName().getString(), items),
                    new CustomPacketPayload[0]);
        }
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
            }
        });
    }

    public static void sendFlags(ServerPlayer player, BackpackAccess access) {
        UpgradeFlags flags = FlagResolver.resolve(access);
        PacketDistributor.sendToPlayer(player,
                new UtilityFlagsPayload(flags.crafting(), flags.furnace(), flags.anvil(), flags.smithing(), flags.stonecutter()),
                new CustomPacketPayload[0]);
    }
}
