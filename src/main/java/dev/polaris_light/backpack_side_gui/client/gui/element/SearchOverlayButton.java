package dev.polaris_light.backpack_side_gui.client.gui.element;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class SearchOverlayButton extends BackpackOverlayButton {
    private final OverlayTextInput input;

    public SearchOverlayButton(OverlayTextInput input) {
        super(ResourceLocation.fromNamespaceAndPath("backpack_side_gui", "textures/gui/search.png"),
                Component.translatable("text.backpack_side_gui.tooltip.search"));
        this.input = input;
    }

    public boolean press(double mouseX, double mouseY) {
        if (!super.press(mouseX, mouseY))
            return false;
        input.toggleVisible();
        if (input.isVisible())
            input.focus();
        return true;
    }

    public OverlayTextInput input() {
        return input;
    }
}
