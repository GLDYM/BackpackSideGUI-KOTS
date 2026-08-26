package dev.polaris_light.backpack_side_gui.client.gui.element;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class UtilityOverlayButton extends BackpackOverlayButton {
    private final UtilityType utilityType;
    private boolean targetVisible;
    private final Consumer<UtilityOverlayButton> onPressed;

    public UtilityOverlayButton(UtilityType utilityType, ResourceLocation icon,
            Consumer<UtilityOverlayButton> onPressed) {
        super(icon, Component.translatable(utilityType.tooltipKey()));
        this.utilityType = utilityType;
        this.onPressed = onPressed == null ? button -> {
        } : onPressed;
    }

    public UtilityType utilityType() {
        return utilityType;
    }

    public boolean targetVisible() {
        return targetVisible;
    }

    public void setTargetVisible(boolean visible) {
        targetVisible = visible;
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
        if (pressed)
            onPressed.accept(this);
        return pressed;
    }
}
