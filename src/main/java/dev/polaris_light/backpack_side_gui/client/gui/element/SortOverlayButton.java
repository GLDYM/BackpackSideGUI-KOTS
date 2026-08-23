package dev.polaris_light.backpack_side_gui.client.gui.element;

import dev.polaris_light.backpack_side_gui.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.function.IntSupplier;

public final class SortOverlayButton extends BackpackOverlayButton {
    private final IntSupplier mode;
    public SortOverlayButton() {
        this(() -> 0);
    }
    public SortOverlayButton(IntSupplier mode) {
        super(ResourceLocation.fromNamespaceAndPath("backpack_side_gui", "textures/gui/sort.png"), Component.translatable("text.backpack_side_gui.tooltip.sort"));
        this.mode = mode;
    }
    
    public boolean press(double mouseX, double mouseY) {
        if (!super.press(mouseX, mouseY)) return false;
        ModNetwork.requestSort(mode.getAsInt());
        return true;
    }
}
