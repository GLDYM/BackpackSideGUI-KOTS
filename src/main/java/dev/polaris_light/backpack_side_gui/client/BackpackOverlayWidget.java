package dev.polaris_light.backpack_side_gui.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;

/**
 * Stateful floating backpack panel: layout, controls, slots and item rendering
 * live here.
 */
final class BackpackOverlayWidget {
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
        private static final int BUTTON_SIZE = 14;
        private static final int BUTTON_GAP = 3;
        private static final ResourceLocation MOVE_ICON = icon("move");
        private static final ResourceLocation SHOW_ICON = icon("show");
        private static final ResourceLocation HIDE_ICON = icon("hide");
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

    private final List<BackpackSlot> slots = new ArrayList<>();
    private final BackpackOverlayScrollbar scrollbar = new BackpackOverlayScrollbar();
    private final OverlayButton moveButton = new OverlayButton(Controls.MOVE_ICON, true);
    private final OverlayButton visibilityButton = new OverlayButton(Controls.SHOW_ICON, false);
    private String title = "Backpack";
    private boolean visible = true;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private int x = Integer.MIN_VALUE;
    private int y = Integer.MIN_VALUE;
    private int width;
    private int height;

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void toggleVisible() {
        visible = !visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setContents(String title, List<ItemStack> items) {
        this.title = title == null || title.isBlank() ? "Backpack" : title;
        slots.clear();
        for (int i = 0; i < items.size(); i++)
            slots.add(new BackpackSlot(i, items.get(i)));
    }

    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
        if (slots.isEmpty())
            return;
        updateBounds(screen.width, screen.height);
        if (visible) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 100.0F);
            int backgroundWidth = width + Layout.PANEL_PADDING;
            graphics.fill(x - 4, y - Layout.TOP_OFFSET, x - 4 + backgroundWidth,
                    y + height + Layout.BOTTOM_PADDING, Palette.PANEL);
            graphics.fill(x - 4, y - Layout.TOP_OFFSET, x - 4 + backgroundWidth,
                    y - Layout.TOP_OFFSET + 1, Palette.PANEL_TOP_EDGE);
            graphics.drawString(minecraft.font, Component.literal(title), x, y - 14, Palette.TITLE, true);
            int totalRows = Math.max(1, (slots.size() + Layout.COLUMNS - 1) / Layout.COLUMNS);
            int visibleRows = Math.max(1, height / Layout.SLOT_SIZE);
            scrollbar.update(x + Layout.SLOT_AREA_WIDTH + 3, y, visibleRows, totalRows);
            for (BackpackSlot slot : slots)
                slot.render(graphics, minecraft, x, y, scrollbar.row(), visibleRows);
            scrollbar.render(graphics);
            graphics.pose().popPose();
        }
        renderBottomButtons(graphics, minecraft, mouseX, mouseY);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || slots.isEmpty())
            return false;
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        updateBounds(event.getScreen().width, event.getScreen().height);

        int buttonY = getButtonY();

        moveButton.setBounds(x, buttonY);
        visibilityButton.setBounds(x + Controls.BUTTON_SIZE + Controls.BUTTON_GAP, buttonY);

        if (visible && scrollbar.press(mouseX, mouseY))
            return true;

        if (moveButton.press(this, mouseX, mouseY)) {
            return true;
        }
        if (visibilityButton.press(this, mouseX, mouseY)) {
            return true;
        }
        return false;
    }

    public boolean mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (scrollbar.drag(event.getMouseY()))
            return true;
        if (!dragging || event.getMouseButton() != 0)
            return false;
        x = (int) event.getMouseX() - dragOffsetX;
        y = (int) event.getMouseY() - dragOffsetY;
        clamp(event.getScreen().width, event.getScreen().height);
        return true;
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
        if (!visible || !contains(event.getMouseX(), event.getMouseY(), x - 4, y - Layout.TOP_OFFSET,
                width + Layout.PANEL_PADDING, height + Layout.TOP_OFFSET + Layout.BOTTOM_PADDING))
            return false;
        return scrollbar.scroll(event.getScrollDeltaY());
    }

    private void updateBounds(int screenWidth, int screenHeight) {
        int rows = Math.max(1, (slots.size() + Layout.COLUMNS - 1) / Layout.COLUMNS);
        int visibleRows = Math.min(rows, Layout.VISIBLE_ROWS);
        width = Layout.SLOT_AREA_WIDTH + (rows > visibleRows ? Layout.SCROLLBAR_EXTRA_WIDTH : 0);
        height = visibleRows * Layout.SLOT_SIZE;
        if (x == Integer.MIN_VALUE) {
            int fullHeight = visibleRows * Layout.SLOT_SIZE + 22 + Controls.BUTTON_SIZE + Layout.BOTTOM_PADDING;
            x = screenWidth / 2 + Layout.DEFAULT_SIDE_OFFSET;
            y = Math.max(4, (screenHeight - fullHeight) / 2 + Layout.SLOT_SIZE);
        }
        clamp(screenWidth, screenHeight);
    }

    private void clamp(int screenWidth, int screenHeight) {
        x = Math.max(0, Math.min(Math.max(0, screenWidth - width), x));
        y = Math.max(0, Math.min(Math.max(0, screenHeight - height), y));
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void renderBottomButtons(GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
        int buttonY = getButtonY();
        moveButton.setBounds(x, buttonY);
        visibilityButton.setIcon(visible ? Controls.HIDE_ICON : Controls.SHOW_ICON);
        visibilityButton.setBounds(x + Controls.BUTTON_SIZE + Controls.BUTTON_GAP, buttonY);
        moveButton.render(graphics, minecraft, mouseX, mouseY);
        visibilityButton.render(graphics, minecraft, mouseX, mouseY);
    }

    private int getButtonY() {
        int visibleRows = Math.min(Math.max(1, (slots.size() + Layout.COLUMNS - 1) / Layout.COLUMNS),
                Layout.VISIBLE_ROWS);
        return y + visibleRows * Layout.SLOT_SIZE + 5;
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath("backpack_side_gui", "textures/gui/" + name + ".png");
    }

    private static void renderIconButton(GuiGraphics graphics, ResourceLocation icon, int x, int y) {
        graphics.fill(x - 1, y - 1, x + Controls.BUTTON_SIZE + 1, y + Controls.BUTTON_SIZE + 1, Palette.BUTTON_BORDER);
        graphics.fill(x, y, x + Controls.BUTTON_SIZE, y + Controls.BUTTON_SIZE, Palette.BUTTON_BACKGROUND);
        graphics.blit(icon, x + 1, y + 1, 0, 0, 12, 12, 12, 12);
    }

    private static final class OverlayButton {
        private ResourceLocation icon;
        private Component label;
        private final ButtonAction action;
        private int x;
        private int y;

        private OverlayButton(ResourceLocation icon, boolean move) {
            this.icon = icon;
            this.action = move ? ButtonAction.MOVE : ButtonAction.TOGGLE;
            this.label = move
                    ? Component.translatable("text.backpack_side_gui.tooltip.move")
                    : Component.translatable("text.backpack_side_gui.tooltip.show");
        }

        private void setIcon(ResourceLocation icon) {
            this.icon = icon;
            this.label = icon.toString().endsWith("hide")
                    ? Component.translatable("text.backpack_side_gui.tooltip.hide")
                    : Component.translatable("text.backpack_side_gui.tooltip.show");
        }

        private void setBounds(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private boolean contains(double mouseX, double mouseY) {
            return BackpackOverlayWidget.contains(mouseX, mouseY, x, y, Controls.BUTTON_SIZE, Controls.BUTTON_SIZE);
        }

        private boolean press(BackpackOverlayWidget widget, double mouseX, double mouseY) {
            if (!contains(mouseX, mouseY))
                return false;
            action.perform(widget, mouseX, mouseY);
            return true;
        }

        private void render(GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
            renderIconButton(graphics, icon, x, y);
            if (contains(mouseX, mouseY)) {
                graphics.renderTooltip(minecraft.font, label, (int) mouseX, (int) mouseY);
            }
        }
    }

    private enum ButtonAction {
        MOVE {
            @Override
            void perform(BackpackOverlayWidget widget, double mouseX, double mouseY) {
                widget.dragging = true;
                widget.dragOffsetX = (int) mouseX - widget.x;
                widget.dragOffsetY = (int) mouseY - widget.y;
            }
        },
        TOGGLE {
            @Override
            void perform(BackpackOverlayWidget widget, double mouseX, double mouseY) {
                widget.visible = !widget.visible;
            }
        };

        abstract void perform(BackpackOverlayWidget widget, double mouseX, double mouseY);
    }

    private static final class BackpackSlot {
        private final int index;
        private final ItemStack stack;

        private BackpackSlot(int index, ItemStack stack) {
            this.index = index;
            this.stack = stack == null ? ItemStack.EMPTY : stack;
        }

        private void render(GuiGraphics graphics, Minecraft minecraft, int originX, int originY,
                int scrollRow, int visibleRows) {
            int row = index / Layout.COLUMNS;
            if (row < scrollRow || row >= scrollRow + visibleRows)
                return;
            int slotX = originX + (index % Layout.COLUMNS) * Layout.SLOT_SIZE;
            int slotY = originY + (row - scrollRow) * Layout.SLOT_SIZE;
            graphics.fill(slotX, slotY, slotX + Layout.SLOT_SIZE, slotY + Layout.SLOT_SIZE, Palette.SLOT_BORDER);
            graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, Palette.SLOT_BACKGROUND);
            if (stack.isEmpty())
                return;
            graphics.renderItem(stack, slotX + 1, slotY + 1);
            if (stack.getCount() <= Numbers.VANILLA_COUNT_LIMIT) {
                graphics.renderItemDecorations(minecraft.font, stack, slotX + 1, slotY + 1);
                return;
            }
            String count = CountAbbreviator.abbreviate(stack.getCount());
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            float scale = Math.min(1.0F, Numbers.MAX_COUNT_WIDTH / minecraft.font.width(count));
            if (scale < 1.0F)
                graphics.pose().scale(scale, scale, 1.0F);
            float drawX = (slotX + 20 - minecraft.font.width(count) * scale - 2.0F) / scale;
            float drawY = (slotY + 10.0F + Numbers.COUNT_Y_ADJUSTMENT * (1.0F - scale)) / scale;
            graphics.drawString(minecraft.font, count, drawX, drawY, Palette.ITEM_COUNT, true);
            graphics.pose().popPose();
        }
    }
}
