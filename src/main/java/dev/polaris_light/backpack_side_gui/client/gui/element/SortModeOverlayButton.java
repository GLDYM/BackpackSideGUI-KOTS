package dev.polaris_light.backpack_side_gui.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Compact text button used by the backpack header to switch sort modes. */
public final class SortModeOverlayButton extends BackpackOverlayButton {
    private static final String[] SORT_LABELS = {"1", "T", "A", "M"};
    private int mode;

    public SortModeOverlayButton() {
        super(ResourceLocation.fromNamespaceAndPath("backpack_side_gui", "textures/gui/sort.png"), Component.translatable("text.backpack_side_gui.tooltip.sort"));
    }

    public int mode() { return mode; }

    @Override
    public boolean press(double mouseX, double mouseY) {
        if (!super.press(mouseX, mouseY))
            return false;
        mode = (mode + 1) % SORT_LABELS.length;
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, Minecraft minecraft) {
        if (!isVisible())
            return;
        int x = getX(), y = getY();
        graphics.fill(x, y, x + 16, y + 16, -872415232);
        graphics.fill(x + 1, y + 1, x + 15, y + 15, -14013910);
        String text = SORT_LABELS[mode];
        graphics.drawString(minecraft.font, text, x + (16 - minecraft.font.width(text)) / 2, y + 4, 16777215, true);
    }
}
