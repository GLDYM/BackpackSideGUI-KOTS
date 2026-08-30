package dev.polaris_light.backpack_side_gui.server.sync;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackSyncPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackVirtualSlot;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;

/** Owns server-to-client backpack state publication. */
public final class BackpackSyncService {
    private BackpackSyncService() {
    }

    public static void send(ServerPlayer player, BackpackAccess access) {
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()),
                new CustomPacketPayload[0]);
        PacketDistributor.sendToPlayer(player, snapshot(access, player), new CustomPacketPayload[0]);
    }

    public static BackpackSyncPayload snapshot(BackpackAccess access, ServerPlayer player) {
        ArrayList<ItemStack> items = new ArrayList<>();
        ArrayList<Integer> limits = new ArrayList<>();
        for (int i = 0; i < access.handler().getSlots(); i++) {
            ItemStack item = access.handler().getStackInSlot(i).copy();
            items.add(item);
            limits.add(new BackpackVirtualSlot(access.stack(), i, player).getMaxStackSize(item));
        }
        return new BackpackSyncPayload(access.stack().getHoverName().getString(), items, limits);
    }
}
