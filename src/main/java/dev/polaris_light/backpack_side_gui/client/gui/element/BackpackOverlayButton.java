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

    public void setBounds(int screenX, int screenY) {
        this.x = screenX;
        this.y = screenY;
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

    public boolean press(double mouseX, double mouseY) {
        if (!visible || mouseX < x || mouseX >= x + SIZE || mouseY < y || mouseY >= y + SIZE)
            return false;
        return true;
    }

    public void render(GuiGraphics graphics, Minecraft minecraft) {
        if (!visible)
            return;
        graphics.fill(x, y, x + SIZE, y + SIZE, -872415232);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, -14013910);
        graphics.blit(icon, x + 2, y + 2, 0, 0, 12, 12, 12, 12);
    }

    public void renderTooltip(GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
        if (visible && mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE)
            graphics.renderTooltip(minecraft.font, label, (int) mouseX, (int) mouseY);
    }
}
