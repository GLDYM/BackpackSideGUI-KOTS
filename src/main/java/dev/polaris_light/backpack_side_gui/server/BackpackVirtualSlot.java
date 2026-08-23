package dev.polaris_light.backpack_side_gui.server;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot;

public final class BackpackVirtualSlot extends StorageInventorySlot {
    public BackpackVirtualSlot(ItemStack backpack, int handlerSlot, Player player) {
        super(false, BackpackWrapper.fromStack(backpack), handlerSlot, player);
    }
}
