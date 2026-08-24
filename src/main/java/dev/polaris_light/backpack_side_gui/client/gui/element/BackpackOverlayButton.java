package dev.polaris_light.backpack_side_gui.client.gui.element;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BackpackOverlayButton extends IOverlayElement {
    public static final int SIZE = 16;
    public ResourceLocation icon;
    public Component label;
    public boolean visible = true;
    public int x, y;

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

    protected int getX() {
        return x;
    }

    protected int getY() {
        return y;
    }

    protected ResourceLocation getIcon() {
        return icon;
    }

    public boolean press(double mx, double my) {
        if (!visible || mx < x || mx >= x + SIZE || my < y || my >= y + SIZE)
            return false;
        return true;
    }

    public void render(GuiGraphics g, Minecraft mc) {
        if (!visible)
            return;
        g.fill(x, y, x + SIZE, y + SIZE, -872415232);
        g.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, -14013910);
        g.blit(icon, x + 2, y + 2, 0, 0, 12, 12, 12, 12);
    }

    public void renderTooltip(GuiGraphics g, Minecraft mc, double mx, double my) {
        if (visible && mx >= x && mx < x + SIZE && my >= y && my < y + SIZE)
            g.renderTooltip(mc.font, label, (int) mx, (int) my);
    }
}
