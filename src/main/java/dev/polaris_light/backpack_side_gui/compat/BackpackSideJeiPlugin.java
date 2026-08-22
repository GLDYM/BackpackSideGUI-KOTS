package dev.polaris_light.backpack_side_gui.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;

@JeiPlugin
public final class BackpackSideJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "jei_plugin");
    }
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) { JeiReflectionCompat.setRuntime(runtime); }
    @Override
    public void onRuntimeUnavailable() { JeiReflectionCompat.clearRuntime(); }
}
