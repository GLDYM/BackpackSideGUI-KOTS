package dev.polaris_light.backpack_side_gui.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import dev.polaris_light.backpack_side_gui.client.SideBackpackClient;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class BackpackSideJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiReflectionCompat.setRuntime(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiReflectionCompat.clearRuntime();
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(new SideCraftingTransferHandler<>(CraftingMenu.class));
    }

    private static final class SideCraftingTransferHandler<C extends AbstractContainerMenu>
            implements IUniversalRecipeTransferHandler<C> {
        private final Class<? extends C> containerClass;

        private SideCraftingTransferHandler(Class<? extends C> containerClass) {
            this.containerClass = containerClass;
        }

        @Override
        public Class<? extends C> getContainerClass() {
            return containerClass;
        }

        @Override
        public Optional<MenuType<C>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public IRecipeTransferError transferRecipe(C menu, Object recipe, IRecipeSlotsView slots,
                Player player, boolean maxTransfer, boolean doTransfer) {
            if (!SideBackpackClient.isCraftingUtilityVisible())
                return null;
            if (doTransfer) {
                List<List<ItemStack>> groups = new ArrayList<>();
                slots.getSlotViews(RecipeIngredientRole.INPUT).stream().limit(9)
                        .forEach(slot -> groups.add(slot.getItemStacks().map(ItemStack::copy).toList()));
                while (groups.size() < 9)
                    groups.add(List.of());
                ClientPacketSender.jeiCraftingFill(groups, maxTransfer);
            }
            return null;
        }
    }
}
