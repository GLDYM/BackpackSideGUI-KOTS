package dev.polaris_light.backpack_side_gui.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.polaris_light.backpack_side_gui.client.SideBackpackClient;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Mixin(targets = "mezz.jei.gui.recipes.RecipeTransferButtonController")
public abstract class JeiRecipeTransferButtonMixin {
    @Shadow(remap = false)
    private IRecipeLayoutDrawable<?> recipeLayout;
    @Shadow(remap = false)
    private Supplier<AbstractContainerScreen<?>> screenSupplier;
    @Shadow(remap = false)
    private Runnable onSuccessfulTransfer;
    @Shadow(remap = false)
    private IRecipeTransferError recipeTransferError;

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true, remap = false)
    private void backpack_side_gui$transfer(IJeiUserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (input.isSimulate())
            return;
        if (Minecraft.getInstance().player == null)
            return;
        if (!backpack_side_gui$isUtilityVisible())
            return;
        IRecipeSlotsView view = recipeLayout.getRecipeSlotsView();
        List<List<ItemStack>> groups = new ArrayList<>();
        view.getSlotViews(RecipeIngredientRole.INPUT).stream().limit(9)
                .forEach(slot -> groups.add(slot.getItemStacks().map(stack -> stack.copy()).toList()));
        if (groups.isEmpty())
            return;
        boolean smith = SideBackpackClient.isSmithingUtilityVisible();
        if (smith) {
            List<List<ItemStack>> smithGroups = new ArrayList<>(
                    groups.subList(0, Math.min(3, groups.size())));
            ClientPacketSender.jeiSmithingFill(smithGroups, Screen.hasShiftDown());
        } else {
            while (groups.size() < 9)
                groups.add(List.of());
            ClientPacketSender.jeiCraftingFill(groups, Screen.hasShiftDown());
        }
        onSuccessfulTransfer.run();
        cir.setReturnValue(true);
    }

    @Inject(method = "updateState", at = @At("RETURN"), remap = false)
    private void backpack_side_gui$forceVisible(IButtonState state, CallbackInfo ci) {
        boolean overlay = SideBackpackClient.isCraftingUtilityVisible()
                || SideBackpackClient.isSmithingUtilityVisible();
        if (!overlay)
            return;
        if (overlay && Minecraft.getInstance().player != null) {
            List<IRecipeSlotView> inputSlots = backpack_side_gui$inputSlots();
            List<List<ItemStack>> groups = inputSlots.stream()
                    .map(slot -> slot.getItemStacks().map(stack -> stack.copy()).toList()).toList();
            List<Integer> missing = SideBackpackClient.missingIngredientIndexes(groups);
            if (!missing.isEmpty()) {
                recipeTransferError = backpack_side_gui$missingError(inputSlots, missing);
                state.setActive(false);
                state.setVisible(true);
                return;
            }
            recipeTransferError = null;
        }
        state.setActive(true);
        state.setVisible(true);
    }

    @Inject(method = "getTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void backpack_side_gui$hideMissingTooltip(ITooltipBuilder tooltip, CallbackInfo ci) {
        if (backpack_side_gui$canFillFromBackpacks()) {
            tooltip.add(Component.translatable("jei.tooltip.transfer"));
            ci.cancel();
        }
    }

    @Inject(method = "drawExtras", at = @At("HEAD"), cancellable = true, remap = false)
    private void backpack_side_gui$hideMissingHighlight(GuiGraphics graphics, Rect2i buttonArea, int mouseX,
            int mouseY, float partialTicks, CallbackInfo ci) {
        if (backpack_side_gui$canFillFromBackpacks())
            ci.cancel();
    }

    private boolean backpack_side_gui$canFillFromBackpacks() {
        if (!backpack_side_gui$isUtilityVisible() || !backpack_side_gui$hasParentContainer()
                || Minecraft.getInstance().player == null)
            return false;
        List<List<ItemStack>> groups = new ArrayList<>();
        recipeLayout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT).stream().limit(9)
                .forEach(slot -> groups.add(slot.getItemStacks().map(stack -> stack.copy()).toList()));
        return SideBackpackClient.canFillFromBackpacks(groups);
    }

    private List<IRecipeSlotView> backpack_side_gui$inputSlots() {
        return recipeLayout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT).stream().limit(9).toList();
    }

    private IRecipeTransferError backpack_side_gui$missingError(
            List<IRecipeSlotView> inputSlots, List<Integer> missing) {
        return new IRecipeTransferError() {
            @Override
            public Type getType() {
                return Type.USER_FACING;
            }

            @Override
            public void getTooltip(ITooltipBuilder tooltip) {
                tooltip.add(Component.translatable("jei.tooltip.transfer"));
                tooltip.add(Component.translatable("jei.tooltip.error.recipe.transfer.missing")
                        .withStyle(ChatFormatting.RED));
            }

            @Override
            public void showError(GuiGraphics graphics, int mouseX, int mouseY,
                    IRecipeSlotsView slotsView, int recipeX, int recipeY) {
                graphics.pose().pushPose();
                graphics.pose().translate(recipeX, recipeY, 0);
                try {
                    for (int index : missing)
                        if (index >= 0 && index < inputSlots.size())
                            inputSlots.get(index).drawHighlight(graphics, 0x66FF0000);
                } finally {
                    graphics.pose().popPose();
                }
            }

            @Override
            public int getMissingCountHint() {
                return missing.size();
            }
        };
    }

    private boolean backpack_side_gui$hasParentContainer() {
        AbstractContainerScreen<?> screen = screenSupplier.get();
        return screen != null && screen.getMenu() != null;
    }

    private boolean backpack_side_gui$isUtilityVisible() {
        return SideBackpackClient.isCraftingUtilityVisible() || SideBackpackClient.isSmithingUtilityVisible();
    }

}
