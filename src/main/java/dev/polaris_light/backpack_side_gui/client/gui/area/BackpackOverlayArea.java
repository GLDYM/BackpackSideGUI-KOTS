package dev.polaris_light.backpack_side_gui.client.gui.area;

import java.util.ArrayList;
import java.util.List;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlayScrollbar;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlaySlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;

/**
 * Backpack Render
 */
public class BackpackOverlayArea extends IOverlayArea {
    private static final class Layout {
        private static final int COLUMNS = 9;
        private static final int SLOT_SIZE = 18;
        private static final int SLOT_AREA_WIDTH = COLUMNS * SLOT_SIZE;
        private static final int SCROLLBAR_EXTRA_WIDTH = 12;
        private static final int PANEL_PADDING = 8;
        private static final int TOP_OFFSET = 18;
        private static final int BOTTOM_PADDING = 4;
        private static final int DEFAULT_SIDE_OFFSET = 94;
        private static final int VISIBLE_ROWS = 6;
    }

    private static final class Controls {
    }

    private static final class Palette {
        private static final int PANEL = -871362544;
        private static final int PANEL_TOP_EDGE = -11184811;
        private static final int TITLE = 16777215;
        private static final int SLOT_BORDER = -12961222;
        private static final int SLOT_BACKGROUND = -14671840;
        private static final int BUTTON_BORDER = -872415232;
        private static final int BUTTON_BACKGROUND = -14013910;
        private static final int ITEM_COUNT = 16777215;
    }

    private static final class Numbers {
        private static final int VANILLA_COUNT_LIMIT = 99;
        private static final float MAX_COUNT_WIDTH = 16.0F;
        private static final float COUNT_Y_ADJUSTMENT = 3.0F;
    }

    private final List<BackpackOverlaySlot> slots = new ArrayList<>();
    private final BackpackOverlayScrollbar scrollbar = new BackpackOverlayScrollbar();
    private String title = "Backpack";

    public void setContents(String title, List<ItemStack> items) {
        this.title = title == null || title.isBlank() ? "Backpack" : title;
        slots.clear();
        for (int i = 0; i < items.size(); i++)
            slots.add(new BackpackOverlaySlot(i, items.get(i)));
    }

    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft) {
        if (slots.isEmpty())
            return;
        updateBounds(screen.width, screen.height);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 130.0F);
        if (visible) {
            int backgroundWidth = width + Layout.PANEL_PADDING;
            graphics.fill(x - 4, y - Layout.TOP_OFFSET, x - 4 + backgroundWidth,
                    y + height + Layout.BOTTOM_PADDING, Palette.PANEL);
            graphics.fill(x - 4, y - Layout.TOP_OFFSET, x - 4 + backgroundWidth,
                    y - Layout.TOP_OFFSET + 1, Palette.PANEL_TOP_EDGE);
            graphics.drawString(minecraft.font, Component.literal(title), x, y - 14, Palette.TITLE, true);
            int totalRows = Math.max(1, (slots.size() + Layout.COLUMNS - 1) / Layout.COLUMNS);
            int visibleRows = Math.max(1, height / Layout.SLOT_SIZE);
            scrollbar.update(x + Layout.SLOT_AREA_WIDTH + 3, y, visibleRows, totalRows);
            renderSlots(graphics, minecraft, scrollbar.row(), visibleRows);
            scrollbar.render(graphics);
        }
        graphics.pose().popPose();
    }

    /**
     * Renders the slot grid owned by this panel; individual slots remain reusable
     * controls.
     */
    protected void renderSlots(GuiGraphics graphics, Minecraft minecraft, int scrollRow, int visibleRows) {
        for (BackpackOverlaySlot slot : slots)
            slot.render(graphics, minecraft, x, y, scrollRow, visibleRows);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || slots.isEmpty())
            return false;
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        updateBounds(event.getScreen().width, event.getScreen().height);

        if (visible && scrollbar.press(mouseX, mouseY))
            return true;
        return false;
    }

    public boolean mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (scrollbar.drag(event.getMouseY()))
            return true;
        return false;
    }

    public boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (scrollbar.release())
            return true;
        if (event.getButton() != 0 || !dragging)
            return false;
        dragging = false;
        return true;
    }

    public boolean mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!visible || !contains(event.getMouseX(), event.getMouseY()))
            return false;
        return scrollbar.scroll(event.getScrollDeltaY());
    }

    private void updateBounds(int screenWidth, int screenHeight) {
        int rows = Math.max(1, (slots.size() + Layout.COLUMNS - 1) / Layout.COLUMNS);
        int visibleRows = Math.min(rows, Layout.VISIBLE_ROWS);
        width = Layout.SLOT_AREA_WIDTH + (rows > visibleRows ? Layout.SCROLLBAR_EXTRA_WIDTH : 0);
        height = visibleRows * Layout.SLOT_SIZE;
        if (x == Integer.MIN_VALUE) {
            int fullHeight = visibleRows * Layout.SLOT_SIZE + 22 + 14 + Layout.BOTTOM_PADDING;
            x = screenWidth / 2 + Layout.DEFAULT_SIDE_OFFSET;
            y = Math.max(4, (screenHeight - fullHeight) / 2 + Layout.SLOT_SIZE);
        }
        clamp(screenWidth, screenHeight);
    }

    protected int getButtonY() {
        int visibleRows = Math.min(Math.max(1, (slots.size() + Layout.COLUMNS - 1) / Layout.COLUMNS),
                Layout.VISIBLE_ROWS);
        return y + visibleRows * Layout.SLOT_SIZE + 5;
    }

    public int overlayX() {
        return x;
    }

    public int overlayButtonY() {
        return getButtonY();
    }

    public void prepareLayout(int screenWidth, int screenHeight) {
        if (!slots.isEmpty()) updateBounds(screenWidth, screenHeight);
    }
    public int buttonOffsetY() { return getButtonY() - y; }

    public void setOverlayPosition(int x, int y) { this.x = x; this.y = y; }
}
