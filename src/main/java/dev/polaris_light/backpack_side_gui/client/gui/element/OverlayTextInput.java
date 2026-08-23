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

    public void render(GuiGraphics g, Minecraft mc, Component hint) {
        if (!visible)
            return;
        g.fill(x - 1, y - 1, x + width + 1, y + 15, -872415232);
        g.fill(x, y, x + width, y + 14, -14671840);
        String text = value.isEmpty() ? hint.getString() : value;
        g.drawString(mc.font, text, x + 3, y + 3, value.isEmpty() ? 7829367 : 16777215, true);
        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cursorX = x + 3 + mc.font.width(value);
            g.drawString(mc.font, "|", cursorX, y + 3, 16777215, true);
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
