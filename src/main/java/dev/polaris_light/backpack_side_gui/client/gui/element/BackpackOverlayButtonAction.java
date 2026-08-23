package dev.polaris_light.backpack_side_gui.client.gui.element;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlay;
import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayElement;
import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayWidget;

public enum BackpackOverlayButtonAction {
    MOVE {
        @Override
        void perform(IOverlay target, double mouseX, double mouseY) {
            if (target instanceof IOverlayWidget widget)
                widget.beginDragging(mouseX, mouseY);
        }
    },
    TOGGLE {
        @Override
        void perform(IOverlay target, double mouseX, double mouseY) {
            if (target instanceof IOverlayElement element)
                element.toggleVisible();
        }
    };

    abstract void perform(IOverlay target, double mouseX, double mouseY);
}
