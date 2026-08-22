package dev.polaris_light.backpack_side_gui.compat;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

/** Compile-time JEI integration. */
public final class JeiReflectionCompat {
    private static volatile IJeiRuntime runtime;
    private JeiReflectionCompat() {}
    public static void setRuntime(IJeiRuntime value) { runtime = value; }
    public static void clearRuntime() { runtime = null; }
    public static boolean showItemRecipes(ItemStack stack) { return show(stack, RecipeIngredientRole.OUTPUT); }
    public static boolean showItemUses(ItemStack stack) { return show(stack, RecipeIngredientRole.INPUT); }
    private static boolean show(ItemStack stack, RecipeIngredientRole role) {
        if (runtime == null || stack == null || stack.isEmpty()) return false;
        runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack.copy()));
        return true;
    }
    public static boolean isJeiRecipesScreen(Screen screen) {
        return screen != null && runtime != null && screen.getClass().getName().contains("jei");
    }
}
