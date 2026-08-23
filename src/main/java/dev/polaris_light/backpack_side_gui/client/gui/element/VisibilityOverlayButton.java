package dev.polaris_light.backpack_side_gui.client.gui.element;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class VisibilityOverlayButton extends BackpackOverlayButton {
    private final IOverlayElement target;
    private final ResourceLocation showIcon;
    private final ResourceLocation hideIcon;
    private boolean targetVisible;

    public VisibilityOverlayButton(IOverlayElement target, ResourceLocation showIcon, ResourceLocation hideIcon) {
        super(showIcon, Component.translatable("text.backpack_side_gui.tooltip.show"));
        this.target = target;
        this.showIcon = showIcon;
        this.hideIcon = hideIcon;
        this.targetVisible = target.isVisible();
        updateIconAndLabel();
    }

    public void updateState() {
        targetVisible = target.isVisible();
        updateIconAndLabel();
    }

    public boolean press(double mouseX, double mouseY) {
        boolean pressed = super.press(mouseX, mouseY);
        if (pressed) {
            target.toggleVisible();
            targetVisible = target.isVisible();
            updateIconAndLabel();
        }
        return pressed;
    }

    public boolean isTargetVisible() { return targetVisible; }

    public void setTargetVisible(boolean visible) {
        targetVisible = visible;
        target.setVisible(visible);
        updateIconAndLabel();
    }

    private void updateIconAndLabel() {
        setIcon(targetVisible ? showIcon : hideIcon);
        setLabel(Component.translatable(targetVisible ? "text.backpack_side_gui.tooltip.hide"
                : "text.backpack_side_gui.tooltip.show"));
    }
}
