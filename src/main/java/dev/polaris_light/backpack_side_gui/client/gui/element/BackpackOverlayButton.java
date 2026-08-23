package dev.polaris_light.backpack_side_gui.client.gui.element;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BackpackOverlayButton extends IOverlayElement {
    private static final int SIZE = 14;
    private ResourceLocation icon;
    private Component label;
    private boolean visible = true;
    private int x, y;

    public BackpackOverlayButton(ResourceLocation icon, Component label) {
        this.icon = icon;
        this.label = label;
    }

    public void setIcon(ResourceLocation icon) {
        this.icon = icon;
    }

    public void setLabel(Component label) {
        this.label = label;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void toggleVisible() {
        visible = !visible;
    }

    public void setBounds(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean press(double mx, double my) {
        if (!visible || mx < x || mx >= x + SIZE || my < y || my >= y + SIZE)
            return false;
        return true;
    }

    public void render(GuiGraphics g, Minecraft mc) {
        if (!visible)
            return;
        g.fill(x - 1, y - 1, x + SIZE + 1, y + SIZE + 1, -872415232);
        g.fill(x, y, x + SIZE, y + SIZE, -14013910);
        g.blit(icon, x + 1, y + 1, 0, 0, 12, 12, 12, 12);
    }

    public void renderTooltip(GuiGraphics g, Minecraft mc, double mx, double my) {
        if (visible && mx >= x && mx < x + SIZE && my >= y && my < y + SIZE)
            g.renderTooltip(mc.font, label, (int) mx, (int) my);
    }
}
