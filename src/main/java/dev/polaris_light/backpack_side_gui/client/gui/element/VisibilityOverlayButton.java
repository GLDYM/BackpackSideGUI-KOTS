package dev.polaris_light.backpack_side_gui.client.gui.element;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    @Override
    public void render(GuiGraphics g, Minecraft mc) {
        int color = targetVisible ? -2047904 : -872415232;
        int color2 = targetVisible ? -11187676 : -14013910;
        if (!visible)
            return;
        g.fill(x, y, x + SIZE, y + SIZE, color);
        g.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, color2);
        g.blit(getIcon(), x + 2, y + 2, 0, 0, 12, 12, 12, 12);
    }

    @Override
    public boolean press(double mouseX, double mouseY) {
        boolean pressed = super.press(mouseX, mouseY);
        if (pressed) {
            target.toggleVisible();
            targetVisible = target.isVisible();
            updateIconAndLabel();
        }
        return pressed;
    }

    public boolean isTargetVisible() {
        return targetVisible;
    }

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
