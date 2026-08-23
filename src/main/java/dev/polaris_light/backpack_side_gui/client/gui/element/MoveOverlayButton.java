package dev.polaris_light.backpack_side_gui.client.gui.element;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlay;
import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MoveOverlayButton extends BackpackOverlayButton {
    public MoveOverlayButton(ResourceLocation icon) {
        super(icon, Component.translatable("text.backpack_side_gui.tooltip.move"));
    }

    public boolean press(IOverlayWidget widget, double mouseX, double mouseY) {
        boolean pressed = super.press(mouseX, mouseY);
        if (pressed) widget.beginDragging(mouseX, mouseY);
        return pressed;
    }
}
