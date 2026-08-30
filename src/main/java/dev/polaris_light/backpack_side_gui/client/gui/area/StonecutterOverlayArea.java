package dev.polaris_light.backpack_side_gui.client.gui.area;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlayScrollbar;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlaySlot;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class StonecutterOverlayArea extends IOverlayArea {
    public static final class Layout {
        private static final int INPUT_BOX_X = 0, INPUT_BOX_Y = 15;
        private static final int OUTPUT_BOX_X = 72, OUTPUT_BOX_Y = 15;
        private static final int RECIPE_BOX_X = 0, RECIPE_BOX_Y = 33;
        private static final int RECIPE_COLUMNS = 4, RECIPE_ROWS = 3;
        private static final int RECIPE_SLOT_COUNT = RECIPE_COLUMNS * RECIPE_ROWS;
        private static final int SCROLLER_X = 78, SCROLLER_Y = 33, SCROLLER_WIDTH = 6, SCROLLER_HEIGHT = 54;
        private static final int SLOT_SIZE = 18, PANEL_WIDTH = 98, PANEL_HEIGHT = 89;
    }

    public final Layout layout = new Layout();
    private ItemStack input = ItemStack.EMPTY, output = ItemStack.EMPTY;
    private ItemStack[] recipes = new ItemStack[0];
    private int selected;
    private final BackpackOverlayScrollbar scrollbar = new BackpackOverlayScrollbar();
    private long lastLeftClickTime;
    private int lastLeftClickSlot = -1;

    public void sync(ItemStack input, ItemStack output, ItemStack[] recipes, int selected) {
        this.input = input.copy();
        this.output = output.copy();
        this.recipes = recipes == null ? new ItemStack[0] : recipes;
        this.selected = selected;
        visible = true;
        width = layout.PANEL_WIDTH;
        height = layout.PANEL_HEIGHT;
        scrollbar.update(x + layout.SCROLLER_X, y + layout.SCROLLER_Y, layout.RECIPE_ROWS, totalRows());
    }

    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft) {
        if (!visible)
            return;
        graphics.fill(x - 4, y - 4, x + layout.PANEL_WIDTH - 4, y + layout.PANEL_HEIGHT + 4, -871362544);
        graphics.fill(x - 4, y - 4, x + layout.PANEL_WIDTH - 4, y - 3, -11184811);
        graphics.drawString(minecraft.font, Component.translatable("text.backpack_side_gui.utility.stonecutter"), x + 4, y + 3, 16777215, true);
        new BackpackOverlaySlot(0, input).renderAt(graphics, minecraft, x + layout.INPUT_BOX_X, y + layout.INPUT_BOX_Y);
        new BackpackOverlaySlot(1, output).renderAt(graphics, minecraft, x + layout.OUTPUT_BOX_X,
                y + layout.OUTPUT_BOX_Y);
        scrollbar.update(x + layout.SCROLLER_X, y + layout.SCROLLER_Y, layout.RECIPE_ROWS, totalRows());
        int firstRecipe = scrollbar.row() * layout.RECIPE_COLUMNS;
        for (int n = 0; n < layout.RECIPE_SLOT_COUNT && firstRecipe + n < recipes.length; n++) {
            int i = firstRecipe + n;
            new BackpackOverlaySlot(i, recipes[i]).renderAt(graphics, minecraft,
                    x + layout.RECIPE_BOX_X + (n % layout.RECIPE_COLUMNS) * layout.SLOT_SIZE,
                    y + layout.RECIPE_BOX_Y + (n / layout.RECIPE_COLUMNS) * layout.SLOT_SIZE);
            if (i == selected)
                new BackpackOverlaySlot(i, recipes[i]).renderDragHighlight(graphics,
                        x + layout.RECIPE_BOX_X + (n % layout.RECIPE_COLUMNS) * layout.SLOT_SIZE,
                        y + layout.RECIPE_BOX_Y + (n / layout.RECIPE_COLUMNS) * layout.SLOT_SIZE);
        }
        scrollbar.render(graphics);
    }

    private boolean mousePressedSlots(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible)
            return false;
        int mouseX = (int) event.getMouseX() - x, mouseY = (int) event.getMouseY() - y;
        if (mouseX >= layout.INPUT_BOX_X && mouseX < layout.INPUT_BOX_X + layout.SLOT_SIZE
                && mouseY >= layout.INPUT_BOX_Y
                && mouseY < layout.INPUT_BOX_Y + layout.SLOT_SIZE) {
            long now = net.minecraft.Util.getMillis();
            boolean dbl = event.getButton() == 0 && lastLeftClickSlot == 0 && now - lastLeftClickTime < 250;
            if (event.getButton() == 0) {
                lastLeftClickSlot = 0;
                lastLeftClickTime = now;
            }
            ClientPacketSender.stonecutterSlot(0, dbl ? 6 : event.getButton(), selected, false, carried(event));
            return true;
        }
        if (mouseX >= layout.OUTPUT_BOX_X && mouseX < layout.OUTPUT_BOX_X + layout.SLOT_SIZE
                && mouseY >= layout.OUTPUT_BOX_Y
                && mouseY < layout.OUTPUT_BOX_Y + layout.SLOT_SIZE) {
            ClientPacketSender.stonecutterSlot(2, event.getButton(), selected, Screen.hasShiftDown(), carried(event));
            return true;
        }
        if (mouseY >= layout.RECIPE_BOX_Y && mouseY < layout.RECIPE_BOX_Y + layout.RECIPE_ROWS * layout.SLOT_SIZE
                && mouseX >= layout.RECIPE_BOX_X
                && mouseX < layout.RECIPE_BOX_X + layout.RECIPE_COLUMNS * layout.SLOT_SIZE) {
            int n = (mouseY - layout.RECIPE_BOX_Y) / layout.SLOT_SIZE * layout.RECIPE_COLUMNS
                    + ((mouseX - layout.RECIPE_BOX_X) / layout.SLOT_SIZE);
            int firstRecipe = scrollbar.row() * layout.RECIPE_COLUMNS;
            if (n < layout.RECIPE_SLOT_COUNT && firstRecipe + n < recipes.length) {
                selected = firstRecipe + n;
                ClientPacketSender.stonecutterSlot(1, 0, selected, false, carried(event));
            }
            return true;
        }
        return panelInteractiveContains(event.getMouseX(), event.getMouseY(), 0, 0);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible)
            return false;
        if (scrollbar.press(event.getMouseX(), event.getMouseY()))
            return true;
        return mousePressedSlots(event);
    }

    public boolean mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        return scrollbar.drag(event.getMouseY());
    }

    public boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        return scrollbar.release();
    }

    public boolean mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!visible)
            return false;
        double mouseX = event.getMouseX() - x;
        double mouseY = event.getMouseY() - y;
        boolean overRecipeArea = mouseX >= layout.RECIPE_BOX_X
                && mouseX < layout.RECIPE_BOX_X + layout.RECIPE_COLUMNS * layout.SLOT_SIZE
                && mouseY >= layout.RECIPE_BOX_Y
                && mouseY < layout.RECIPE_BOX_Y + layout.RECIPE_ROWS * layout.SLOT_SIZE;
        boolean overScroller = mouseX >= layout.SCROLLER_X
                && mouseX < layout.SCROLLER_X + layout.SCROLLER_WIDTH
                && mouseY >= layout.SCROLLER_Y
                && mouseY < layout.SCROLLER_Y + layout.SCROLLER_HEIGHT;
        return (overRecipeArea || overScroller) && scrollbar.scroll(event.getScrollDeltaY());
    }

    private int totalRows() {
        return Math.max(1, (recipes.length + layout.RECIPE_COLUMNS - 1) / layout.RECIPE_COLUMNS);
    }

    private ItemStack carried(ScreenEvent.MouseButtonPressed.Pre event) {
        return event.getScreen() instanceof AbstractContainerScreen<?> c ? c.getMenu().getCarried() : ItemStack.EMPTY;
    }

    public void renderTooltip(GuiGraphics graphics, double mouseX, double mouseY) {
        if (!visible)
            return;
        BackpackOverlaySlot inputSlot = new BackpackOverlaySlot(0, input);
        BackpackOverlaySlot outputSlot = new BackpackOverlaySlot(1, output);
        inputSlot.renderHighlightAt(graphics, x + layout.INPUT_BOX_X, y + layout.INPUT_BOX_Y, mouseX, mouseY);
        outputSlot.renderHighlightAt(graphics, x + layout.OUTPUT_BOX_X, y + layout.OUTPUT_BOX_Y, mouseX, mouseY);
        inputSlot.renderTooltip(graphics, Minecraft.getInstance(), x + layout.INPUT_BOX_X, y + layout.INPUT_BOX_Y,
                mouseX, mouseY);
        outputSlot.renderTooltip(graphics, Minecraft.getInstance(), x + layout.OUTPUT_BOX_X, y + layout.OUTPUT_BOX_Y,
                mouseX, mouseY);
        int firstRecipe = scrollbar.row() * layout.RECIPE_COLUMNS;
        for (int n = 0; n < layout.RECIPE_SLOT_COUNT && firstRecipe + n < recipes.length; n++) {
            int i = firstRecipe + n;
            int sx = x + layout.RECIPE_BOX_X + (n % layout.RECIPE_COLUMNS) * layout.SLOT_SIZE;
            int sy = y + layout.RECIPE_BOX_Y + (n / layout.RECIPE_COLUMNS) * layout.SLOT_SIZE;
            BackpackOverlaySlot slot = new BackpackOverlaySlot(i, recipes[i]);
            slot.renderHighlightAt(graphics, sx, sy, mouseX, mouseY);
            slot.renderTooltip(graphics, Minecraft.getInstance(), sx, sy, mouseX, mouseY);
        }
    }

    public boolean panelInteractiveContains(double mouseX, double mouseY, int sw, int sh) {
        return visible && mouseX >= x - 4 && mouseX < x + layout.PANEL_WIDTH - 4 && mouseY >= y - 4
                && mouseY < y + layout.PANEL_HEIGHT + 4;
    }
}

