package dev.polaris_light.backpack_side_gui.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;

public final class BackpackOverlayScrollbar {
    private int row;
    private int maxRow;
    private int x, y, height, thumbY, thumbHeight;
    private boolean dragging;
    private int dragOffset;

    public void update(int x, int y, int visibleRows, int totalRows) {
        this.x = x;
        this.y = y;
        this.height = visibleRows * 18;
        maxRow = Math.max(0, totalRows - visibleRows);
        row = Math.max(0, Math.min(maxRow, row));
        thumbHeight = Math.max(12, height * visibleRows / Math.max(1, totalRows));
        int travel = Math.max(1, height - thumbHeight);
        thumbY = y + Math.round(travel * row / (float) Math.max(1, maxRow));
    }

    public int row() {
        return row;
    }

    public boolean scroll(double mouseX, double mouseY, double amount) {
        if (maxRow == 0 || !contains(mouseX, mouseY))
            return false;
        return scroll(amount);
    }

    public boolean scroll(double amount) {
        if (maxRow == 0)
            return false;
        row = Math.max(0, Math.min(maxRow, row - (amount > 0 ? 1 : -1)));
        return true;
    }

    public boolean press(double mouseX, double mouseY) {
        if (maxRow == 0 || !contains(mouseX, mouseY))
            return false;
        dragging = true;
        dragOffset = mouseY >= thumbY && mouseY < thumbY + thumbHeight ? (int) mouseY - thumbY : thumbHeight / 2;
        setFromMouse(mouseY - dragOffset);
        return true;
    }

    public boolean drag(double mouseY) {
        if (!dragging)
            return false;
        setFromMouse(mouseY - dragOffset);
        return true;
    }

    public boolean release() {
        if (!dragging)
            return false;
        dragging = false;
        return true;
    }

    public void render(GuiGraphics g) {
        if (maxRow == 0)
            return;
        g.fill(x, y, x + 6, y + height, -14671840);
        g.fill(x + 1, thumbY, x + 5, thumbY + thumbHeight, -6645094);
    }

    private void setFromMouse(double mouseY) {
        int travel = Math.max(1, height - thumbHeight);
        row = (int) Math.round(Math.max(0, Math.min(1, (mouseY - y) / travel)) * maxRow);
    }

    private boolean contains(double mx, double my) {
        return mx >= x && mx < x + 6 && my >= y && my < y + height;
    }
}
