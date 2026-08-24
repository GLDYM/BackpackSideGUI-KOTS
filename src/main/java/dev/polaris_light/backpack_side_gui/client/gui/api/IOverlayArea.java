package dev.polaris_light.backpack_side_gui.client.gui.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Set of capabilities required by reusable overlay widgets. */
public abstract class IOverlayArea extends IOverlayElement {
    protected boolean dragging;
    protected int dragOffsetX, dragOffsetY;
    protected int x, y;
    protected int width, height;

    /** Updates the screen-space anchor used when rendering and hit-testing this area. */
    public void setOverlayPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void toggleVisible() {
        visible = !visible;
    }

    // @Override
    // public void beginDragging(double mouseX, double mouseY) {
    // dragging = true;
    // dragOffsetX = (int) mouseX - x;
    // dragOffsetY = (int) mouseY - y;
    // }

    protected void clamp(int screenWidth, int screenHeight) {
        x = Math.max(0, Math.min(Math.max(0, screenWidth - this.width), x));
        y = Math.max(0, Math.min(Math.max(0, screenHeight - this.height), y));
    }

    protected boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
    }

    public abstract void render(Screen screen, GuiGraphics graphics, Minecraft minecraft);
}
