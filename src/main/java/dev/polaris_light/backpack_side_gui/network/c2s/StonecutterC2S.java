package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.List;
import java.util.Optional;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.StonecutterClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.StonecutterSyncPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

public final class StonecutterC2S {
    private StonecutterC2S() {
    }

    private static StonecutterUpgradeItem.Wrapper wrapper(ServerPlayer p, BackpackAccess a) {
        var w = BackpackWrapper.fromStack(a.stack()).getUpgradeHandler()
                .getWrappersThatImplement(StonecutterUpgradeItem.Wrapper.class);
        return w.isEmpty() ? null : w.get(0);
    }

    public static void handleClick(ServerPlayer p, StonecutterClickPayload q) {
        var a = BackpackResolver.resolve(p);
        if (a.isEmpty())
            return;
        var w = wrapper(p, a.get());
        if (w == null)
            return;
        IItemHandlerModifiable inv = w.getInputInventory();
        ItemStack carried = p.containerMenu.getCarried();
        if (!ItemStack.matches(carried, q.carried())) {
            if (!p.gameMode.isCreative())
                return;
            carried = q.carried().copy();
            p.containerMenu.setCarried(carried);
        }
        if (q.slot() == 1) {
            var rs = recipes(p, inv);
            if (q.recipe() >= 0 && q.recipe() < rs.size())
                w.setRecipeId(rs.get(q.recipe()).id());
        } else if (q.slot() == 0) {
            p.containerMenu.setCarried(q.button() == 6 ? collect(inv, carried)
                    : HandlerSlotClicker.click(inv, 0, q.button(), carried));
        } else if (q.slot() == 2) {
            var out = result(p, inv, q.recipe());
            if (!out.isEmpty()) {
                if (q.shift()) {
                    while (!out.isEmpty() && canInsert(p, out)) {
                        if (!p.getInventory().add(out.copy()))
                            break;
                        inv.extractItem(0, 1, false);
                        out = result(p, inv, q.recipe());
                    }
                } else if (carried.isEmpty()) {
                    p.containerMenu.setCarried(out.copy());
                    inv.extractItem(0, 1, false);
                } else if (ItemStack.isSameItemSameComponents(carried, out)
                        && carried.getCount() + out.getCount() <= carried.getMaxStackSize()) {
                    carried.grow(out.getCount());
                    p.containerMenu.setCarried(carried);
                    inv.extractItem(0, 1, false);
                }
            }
        }
        p.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(p, new BackpackCarriedPayload(p.containerMenu.getCarried().copy()));
        send(p, a.get());
    }
    private static ItemStack collect(IItemHandlerModifiable inv, ItemStack carried) {
        ItemStack stack = inv.getStackInSlot(0);
        if (stack.isEmpty() && carried.isEmpty()) return carried;
        if (carried.isEmpty()) return inv.extractItem(0, stack.getCount(), false);
        if (!ItemStack.isSameItemSameComponents(stack, carried)) return carried;
        ItemStack taken = inv.extractItem(0, Math.min(carried.getMaxStackSize() - carried.getCount(), stack.getCount()), false);
        return carried.copyWithCount(carried.getCount() + taken.getCount());
    }

    private static boolean canInsert(ServerPlayer p, ItemStack stack) {
        int room = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.isEmpty())
                room += stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(s, stack))
                room += Math.max(0, s.getMaxStackSize() - s.getCount());
            if (room >= stack.getCount())
                return true;
        }
        return false;
    }

    private static List<RecipeHolder<StonecutterRecipe>> recipes(ServerPlayer p, IItemHandlerModifiable i) {
        var in = i.getStackInSlot(0);
        return in.isEmpty() ? List.of()
                : RecipeHelper.getRecipesOfType(RecipeType.STONECUTTING, new SingleRecipeInput(in));
    }

    private static ItemStack result(ServerPlayer p, IItemHandlerModifiable i, int n) {
        var r = recipes(p, i);
        if (n < 0 || n >= r.size())
            return ItemStack.EMPTY;
        return r.get(n).value().assemble(new SingleRecipeInput(i.getStackInSlot(0)), p.registryAccess());
    }

    public static void send(ServerPlayer p, BackpackAccess a) {
        var w = wrapper(p, a);
        if (w == null)
            return;
        var i = w.getInputInventory();
        var rs = recipes(p, i);
        ItemStack[] out = rs.stream().map(r -> r.value().getResultItem(p.registryAccess())).toArray(ItemStack[]::new);
        int selected = w.getRecipeId().flatMap(id -> {
            for (int n = 0; n < rs.size(); n++)
                if (rs.get(n).id().equals(id))
                    return Optional.of(n);
            return Optional.empty();
        }).orElse(0);
        PacketDistributor.sendToPlayer(p,
                new StonecutterSyncPayload(i.getStackInSlot(0), result(p, i, selected), out, selected));
    }
}
