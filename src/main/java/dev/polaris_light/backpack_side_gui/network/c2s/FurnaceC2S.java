package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.Optional;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.FurnaceClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.FurnaceSyncPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeWrapper;

public final class FurnaceC2S {
    private FurnaceC2S() {
    }

    private static IItemHandler inv(ServerPlayer player, BackpackAccess access) {
        var w = BackpackWrapper.fromStack(access.stack()).getUpgradeHandler()
                .getWrappersThatImplement(CookingUpgradeWrapper.class);
        return w.isEmpty() ? null : w.get(0).getCookingLogic().getCookingInventory();
    }

    public static void handleClick(ServerPlayer player, FurnaceClickPayload payload) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty() || payload.slot() < 0 || payload.slot() > 2)
            return;
        IItemHandler inventory = inv(player, access.get());
        if (inventory == null)
            return;
        ItemStack carried = player.containerMenu.getCarried();
        if (!ItemStack.matches(carried, payload.carried())) {
            if (!player.gameMode.isCreative())
                return;
            carried = payload.carried().copy();
            player.containerMenu.setCarried(carried);
        }
        int realSlot = payload.slot();
        if (payload.slot() == 2 && !carried.isEmpty())
            return;
        player.containerMenu.setCarried(payload.button() == 6 && realSlot < 2 ? collect(inventory, realSlot, carried)
                : HandlerSlotClicker.click(inventory, realSlot, payload.button(), carried));
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()));
        send(player, access.get());
    }

    private static ItemStack collect(IItemHandler inv, int source, ItemStack carried) {
        return HandlerSlotClicker.collect(inv, source, 2, carried);
    }

    public static void send(ServerPlayer player, BackpackAccess access) {
        IItemHandler inventory = inv(player, access);
        if (inventory == null)
            return;
        var w = BackpackWrapper.fromStack(access.stack()).getUpgradeHandler()
                .getWrappersThatImplement(CookingUpgradeWrapper.class).get(0).getCookingLogic();
        PacketDistributor.sendToPlayer(player,
                new FurnaceSyncPayload(inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2),
                        Math.max(0, w.getBurnTimeFinish() - player.level().getGameTime()), w.getBurnTimeTotal(),
                        Math.max(0, w.getCookTimeFinish() - player.level().getGameTime()), w.getCookTimeTotal(),
                        w.isCooking()));
    }
}
