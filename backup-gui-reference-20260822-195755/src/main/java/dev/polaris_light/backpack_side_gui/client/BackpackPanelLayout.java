package dev.polaris_light.backpack_side_gui.client;

/** Calculates the geometry of the side backpack panel. */
final class BackpackPanelLayout {
    static final int SLOT_SIZE = 18;
    private static final int PANEL_BASE_WIDTH = 162;

    private BackpackPanelLayout() {
    }

    static PanelRect calculate(int screenWidth, int screenHeight, int totalRows, int visibleRowsSetting,
            boolean rightSide, int xOffset, int yOffset) {
        visibleRowsSetting = Math.max(2, Math.min(12, visibleRowsSetting));
        int rows = Math.min(visibleRowsSetting, Math.max(1, totalRows));
        int extraScrollbar = totalRows > rows ? 12 : 0;
        int panelWidth = PANEL_BASE_WIDTH + extraScrollbar;
        int panelHeight = (rows * SLOT_SIZE) + 22 + 14 + 4;

        int x;
        if (!rightSide) {
            x = (((screenWidth / 2) - 94) - panelWidth) + xOffset;
        } else {
            x = (screenWidth / 2) + 94 + xOffset;
        }
        int clampedX = Math.max(0, Math.min(screenWidth - panelWidth, x));
        int y = Math.max(0, ((screenHeight - panelHeight) / 2) + SLOT_SIZE + yOffset);
        int clampedY = Math.min(((screenHeight - (rows * SLOT_SIZE)) - 14) - 10, y);
        return new PanelRect(clampedX, clampedY, panelWidth, rows * SLOT_SIZE, rows);
    }

    static final class PanelRect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int visibleRows;

        private PanelRect(int x, int y, int width, int height, int visibleRows) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.visibleRows = visibleRows;
        }

        int x() { return x; }
        int y() { return y; }
        int width() { return width; }
        int height() { return height; }
        int visibleRows() { return visibleRows; }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        boolean slotsContains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + 162 && mouseY >= y && mouseY < y + height;
        }

        boolean scrollbarContains(double mouseX, double mouseY) {
            return mouseX >= x + 162 && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}

