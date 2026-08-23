package dev.polaris_light.backpack_side_gui.mixin;

import dev.polaris_light.backpack_side_gui.client.SideBackpackClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void backpackSideGui$blockClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (SideBackpackClient.shouldBlockContainerInput((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY)) cir.setReturnValue(true);
    }
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void backpackSideGui$blockRelease(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (SideBackpackClient.shouldBlockContainerInput((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY)) cir.setReturnValue(true);
    }
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void backpackSideGui$blockDrag(double mouseX, double mouseY, int button, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (SideBackpackClient.shouldBlockContainerInput((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY)) cir.setReturnValue(true);
    }
}
