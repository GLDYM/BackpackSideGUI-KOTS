package dev.polaris_light.backpack_side_gui.network.c2s;

import java.util.List;
import java.util.Optional;

import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SmithingClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SmithingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.JeiSmithingFillPayload;
import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.smithing.SmithingUpgradeWrapper;

public final class SmithingC2S {
    private SmithingC2S() {
    }

    private static IItemHandler findInventory(ServerPlayer player) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty())
            return null;
        List<SmithingUpgradeWrapper> wrappers = BackpackWrapper.fromStack(access.get().stack()).getUpgradeHandler()
                .getWrappersThatImplement(SmithingUpgradeWrapper.class);
        return wrappers.isEmpty() ? null : wrappers.get(0).getInventory();
    }

    public static void handleClick(ServerPlayer player, SmithingClickPayload payload) {
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        if (access.isEmpty() || payload.slot() < 0)
            return;
        IItemHandler inventory = findInventory(player);
        if (inventory == null || payload.slot() > inventory.getSlots())
            return;
        ItemStack carried = player.containerMenu.getCarried();
        if (!ItemStack.matches(carried, payload.carried())) {
            if (!player.gameMode.isCreative())
                return;
            carried = payload.carried().copy();
            player.containerMenu.setCarried(carried);
        }
        if (payload.slot() == 3) {
            ItemStack result = getResult(player, inventory);
            if (!result.isEmpty() && carried.isEmpty()) {
                player.containerMenu.setCarried(result);
                for (int slotIndex = 0; slotIndex < 3; slotIndex++)
                    inventory.extractItem(slotIndex, 1, false);
            }
        } else
            player.containerMenu
                    .setCarried(HandlerSlotClicker.click(inventory, payload.slot(), payload.button(), carried));
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCarriedPayload(player.containerMenu.getCarried().copy()));
        send(player, inventory);
    }
    public static void handleJeiFill(ServerPlayer player, JeiSmithingFillPayload payload) {
        IItemHandler inv = findInventory(player); if (inv == null) return;
        for (int i=0;i<Math.min(3,payload.ingredients().size());i++) if (inv.getStackInSlot(i).isEmpty()) for (ItemStack o: payload.ingredients().get(i)) { if (o==null||o.isEmpty()) continue; ItemStack f=find(player,o); if(!f.isEmpty()){inv.insertItem(i,f,false);break;} }
        player.containerMenu.broadcastChanges(); send(player, (BackpackAccess)null);
    }
    private static ItemStack find(ServerPlayer p, ItemStack w) {
        for (int i=0;i<p.getInventory().items.size();i++){ItemStack s=p.getInventory().items.get(i);if(ItemStack.isSameItemSameComponents(s,w)){ItemStack o=s.copyWithCount(1);s.shrink(1);return o;}}
        for (BackpackAccess a: BackpackResolver.getAllBackpacks(p)) for(int i=0;i<a.handler().getSlots();i++){ItemStack s=a.handler().getStackInSlot(i);if(ItemStack.isSameItemSameComponents(s,w)) return a.handler().extractItem(i,1,false);} return ItemStack.EMPTY;
    }

    private static ItemStack getResult(ServerPlayer player, IItemHandler inventory) {
        SmithingRecipeInput input = new SmithingRecipeInput(inventory.getStackInSlot(0).copy(),
                inventory.getStackInSlot(1).copy(),
                inventory.getStackInSlot(2).copy());
        return player.level().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, player.level())
                .map(holder -> holder.value().assemble(input, player.registryAccess())).orElse(ItemStack.EMPTY);
    }

    public static void send(ServerPlayer player, BackpackAccess ignoredAccess) {
        IItemHandler inventory = findInventory(player);
        if (inventory != null)
            send(player, inventory);
    }

    private static void send(ServerPlayer player, IItemHandler inventory) {
        PacketDistributor.sendToPlayer(player, new SmithingSyncPayload(inventory.getStackInSlot(0),
                inventory.getStackInSlot(1), inventory.getStackInSlot(2), getResult(player, inventory)));
    }
}
