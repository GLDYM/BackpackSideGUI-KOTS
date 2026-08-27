package dev.polaris_light.backpack_side_gui.client.gui.element;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Reusable single-line text input control for overlay areas. */
public class OverlayTextInput extends IOverlayElement {
    private String value = "";
    private int x, y, width;
    private boolean focused;

    public OverlayTextInput(int width) {
        this.width = Math.max(1, width);
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setBounds(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void focus() {
        if (visible)
            focused = true;
    }

    public void render(GuiGraphics graphics, Minecraft minecraft, Component hint) {
        if (!visible)
            return;
        graphics.fill(x, y, x + width, y + 16, -872415232);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 15, -14671840);
        String text = value.isEmpty() ? hint.getString() : value;
        graphics.drawString(minecraft.font, text, x + 4, y + 4, value.isEmpty() ? 7829367 : 16777215, true);
        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cursorX = x + 3 + minecraft.font.width(value);
            graphics.drawString(minecraft.font, "|", cursorX, y + 4, 16777215, true);
        }
    }

    public boolean mousePressed(double mouseX, double mouseY) {
        focused = visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 14;
        return focused;
    }

    public boolean keyPressed(int keyCode) {
        if (!focused)
            return false;
        if (keyCode == 259 && !value.isEmpty())
            value = value.substring(0, value.length() - 1);
        if (keyCode == 256)
            focused = false;
        return true;
    }

    public boolean charTyped(char codePoint) {
        if (!focused || codePoint < ' ' || codePoint == 127 || value.length() >= 64)
            return false;
        value += codePoint;
        return true;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible)
            focused = false;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void toggleVisible() {
        setVisible(!visible);
    }
}
