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
import dev.polaris_light.backpack_side_gui.client.gui.element.OverlayTextInput;
import dev.polaris_light.backpack_side_gui.client.gui.element.SearchOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.SortOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.SortModeOverlayButton;

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
        private static final int TOP_OFFSET = 21;
        private static final int BOTTOM_PADDING = 4;
        private static final int DEFAULT_SIDE_OFFSET = 94;
        private static final int VISIBLE_ROWS = 6;
    }

    private static final class Palette {
        private static final int PANEL = -871362544;
        private static final int PANEL_TOP_EDGE = -11184811;
        private static final int TITLE = 16777215;
    }


    private final List<BackpackOverlaySlot> slots = new ArrayList<>();
    private final List<ItemStack> allItems = new ArrayList<>();
    private final BackpackOverlayScrollbar scrollbar = new BackpackOverlayScrollbar();
    private final OverlayTextInput searchInput = new OverlayTextInput(108);
    private final SearchOverlayButton searchButton = new SearchOverlayButton(searchInput);
    private final SortModeOverlayButton sortModeButton = new SortModeOverlayButton();
    private final SortOverlayButton sortButton = new SortOverlayButton(sortModeButton::mode);
    private String title = "Backpack";
    private String filter = "";


    public void setContents(String title, List<ItemStack> items) {
        setContents(title, items, filter);
    }

    public void setFilter(String filter) {
        setContents(title, allItems, filter);
    }

    public void setContents(String title, List<ItemStack> items, String filter) {
        this.title = title == null || title.isBlank() ? "Backpack" : title;
        this.filter = filter == null ? "" : filter;
        List<ItemStack> source = new ArrayList<>(items);
        allItems.clear();
        allItems.addAll(source);
        slots.clear();
        String needle = this.filter.toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < source.size(); i++) {
            ItemStack stack = source.get(i);
            if (needle.isEmpty()
                    || stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(needle))
                slots.add(new BackpackOverlaySlot(i, stack));
        }
    }

    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft) {
        updateBounds(screen.width, screen.height);
        if (visible) {
            int backgroundWidth = width + Layout.PANEL_PADDING;
            graphics.fill(x - 4, y - Layout.TOP_OFFSET, x - 4 + backgroundWidth,
                    y + height + Layout.BOTTOM_PADDING, Palette.PANEL);
            graphics.fill(x - 4, y - Layout.TOP_OFFSET, x - 4 + backgroundWidth,
                    y - Layout.TOP_OFFSET + 1, Palette.PANEL_TOP_EDGE);

            int totalRows = Math.max(1, (slots.size() + Layout.COLUMNS - 1) / Layout.COLUMNS);
            int visibleRows = Math.max(1, height / Layout.SLOT_SIZE);
            scrollbar.update(x + Layout.SLOT_AREA_WIDTH + 3, y, visibleRows, totalRows);
            renderSlots(graphics, minecraft, scrollbar.row(), visibleRows);
            scrollbar.render(graphics);

            int topY = y - 18;

            searchButton.setBounds(x + 114, topY);
            sortButton.setBounds(x + 130, topY);
            sortModeButton.setBounds(x + 146, topY);

            searchButton.render(graphics, minecraft);
            sortButton.render(graphics, minecraft);
            sortModeButton.render(graphics, minecraft);

            if (searchInput.isVisible()) {
                searchInput.setBounds(x, topY);
                searchInput.render(graphics, minecraft,
                        Component.translatable("text.backpack_side_gui.tooltip.search"));
            } else {
                graphics.drawString(minecraft.font, Component.literal(title), x + 1, y - 13, Palette.TITLE, true);
            }
        }
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
        if (event.getButton() != 0)
            return false;
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        updateBounds(event.getScreen().width, event.getScreen().height);

        // Clicking anywhere in the screen outside the input removes its focus,
        // including when the user clicks another screen's text field.
        searchInput.mousePressed(mouseX, mouseY);
        if (visible && scrollbar.press(mouseX, mouseY))
            return true;
        int topY = y - 16;
        searchButton.setBounds(x + 115, topY);
        sortButton.setBounds(x + 131, topY);
        if (searchInput.isFocused())
            return true;
        if (searchButton.press(mouseX, mouseY) || sortButton.press(mouseX, mouseY))
            return true;
        sortModeButton.setBounds(x + 147, topY);
        if (sortModeButton.press(mouseX, mouseY))
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

    public boolean keyPressed(int key) {
        boolean handled = searchInput.keyPressed(key);
        if (handled)
            setFilter(searchInput.value());
        return handled;
    }

    public boolean charTyped(char c) {
        boolean handled = searchInput.charTyped(c);
        if (handled)
            setFilter(searchInput.value());
        return handled;
    }

    public void prepareLayout(int screenWidth, int screenHeight) {
        if (!slots.isEmpty())
            updateBounds(screenWidth, screenHeight);
    }

    public int buttonOffsetY() {
        return getButtonY() - y;
    }

    public void setOverlayPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
