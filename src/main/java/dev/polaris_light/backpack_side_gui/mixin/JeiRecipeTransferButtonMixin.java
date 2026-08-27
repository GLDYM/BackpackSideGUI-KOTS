package dev.polaris_light.backpack_side_gui.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.polaris_light.backpack_side_gui.client.SideBackpackClient;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.gui.recipes.RecipesGui;

@Mixin(targets = "mezz.jei.gui.recipes.RecipeTransferButtonController")
public abstract class JeiRecipeTransferButtonMixin {
    @Shadow private IRecipeLayoutDrawable<?> recipeLayout;
    @Shadow private RecipesGui recipesGui;

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true, remap = false)
    private void backpack_side_gui$transfer(IJeiUserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (input.isSimulate() || (!SideBackpackClient.isCraftingUtilityVisible() && !SideBackpackClient.isSmithingUtilityVisible()))
            return;
        if (Minecraft.getInstance().player == null)
            return;
        IRecipeSlotsView view = recipeLayout.getRecipeSlotsView();
        List<List<net.minecraft.world.item.ItemStack>> groups = new ArrayList<>();
        view.getSlotViews(RecipeIngredientRole.INPUT).stream().limit(9)
                .forEach(slot -> groups.add(slot.getItemStacks().map(s -> s.copy()).toList()));
        if (groups.isEmpty())
            return;
        boolean smith = SideBackpackClient.isSmithingUtilityVisible();
        if (smith) { List<List<net.minecraft.world.item.ItemStack>> smithGroups = new ArrayList<>(groups.subList(0, Math.min(3, groups.size()))); ClientPacketSender.jeiSmithingFill(smithGroups, net.minecraft.client.gui.screens.Screen.hasShiftDown()); }
        else { while (groups.size() < 9) groups.add(List.of()); ClientPacketSender.jeiCraftingFill(groups, net.minecraft.client.gui.screens.Screen.hasShiftDown()); }
        recipesGui.onClose();
        cir.setReturnValue(true);
    }

    @Inject(method = "updateState", at = @At("RETURN"), remap = false)
    private void backpack_side_gui$forceVisible(IButtonState state, CallbackInfo ci) {
        if (SideBackpackClient.isCraftingUtilityVisible() || SideBackpackClient.isSmithingUtilityVisible()) {
            state.setActive(true);
            state.setVisible(true);
        }
    }
}
