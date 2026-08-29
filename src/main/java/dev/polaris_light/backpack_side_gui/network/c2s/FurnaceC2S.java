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

    private static IItemHandler inv(ServerPlayer p, BackpackAccess a) {
        var w = BackpackWrapper.fromStack(a.stack()).getUpgradeHandler()
                .getWrappersThatImplement(CookingUpgradeWrapper.class);
        return w.isEmpty() ? null : w.get(0).getCookingLogic().getCookingInventory();
    }

    public static void handleClick(ServerPlayer p, FurnaceClickPayload q) {
        Optional<BackpackAccess> a = BackpackResolver.resolve(p);
        if (a.isEmpty() || q.slot() < 0 || q.slot() > 2)
            return;
        IItemHandler i = inv(p, a.get());
        if (i == null)
            return;
        ItemStack carried = p.containerMenu.getCarried();
        if (!ItemStack.matches(carried, q.carried())) {
            if (!p.gameMode.isCreative())
                return;
            carried = q.carried().copy();
            p.containerMenu.setCarried(carried);
        }
        int realSlot = q.slot();
        if (q.slot() == 2 && !carried.isEmpty())
            return;
        p.containerMenu.setCarried(q.button() == 6 && realSlot < 2 ? collect(i, realSlot, carried)
                : HandlerSlotClicker.click(i, realSlot, q.button(), carried));
        p.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(p, new BackpackCarriedPayload(p.containerMenu.getCarried().copy()));
        send(p, a.get());
    }

    private static ItemStack collect(IItemHandler inv, int source, ItemStack carried) {
        return HandlerSlotClicker.collect(inv, source, 2, carried);
    }

    public static void send(ServerPlayer p, BackpackAccess a) {
        IItemHandler i = inv(p, a);
        if (i == null)
            return;
        var w = BackpackWrapper.fromStack(a.stack()).getUpgradeHandler()
                .getWrappersThatImplement(CookingUpgradeWrapper.class).get(0).getCookingLogic();
        PacketDistributor.sendToPlayer(p,
                new FurnaceSyncPayload(i.getStackInSlot(0), i.getStackInSlot(1), i.getStackInSlot(2),
                        Math.max(0, w.getBurnTimeFinish() - p.level().getGameTime()), w.getBurnTimeTotal(),
                        Math.max(0, w.getCookTimeFinish() - p.level().getGameTime()), w.getCookTimeTotal(),
                        w.isCooking()));
    }
}
