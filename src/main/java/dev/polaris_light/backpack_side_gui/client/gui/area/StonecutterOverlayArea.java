package dev.polaris_light.backpack_side_gui.client.gui.area;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlaySlot;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlayScrollbar;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class StonecutterOverlayArea extends IOverlayArea {
    public static final class Layout {
        public int inputBoxX = 0, inputBoxY = 15;
        public int outputBoxX = 54, outputBoxY = 15;
        public int recipeBoxX = 0, recipeBoxY = 33;
        public int recipeColumns = 4, recipeRows = 3;
        public int recipeSlotCount = recipeColumns * recipeRows;
        public int scrollerX = 78, scrollerY = 33, scrollerWidth = 6, scrollerHeight = 54;
        public int slotSize = 18, panelWidth = 98, panelHeight = 89;
    }

    public final Layout layout = new Layout();
    private ItemStack input = ItemStack.EMPTY, output = ItemStack.EMPTY;
    private ItemStack[] recipes = new ItemStack[0];
    private int selected;
    private final BackpackOverlayScrollbar scrollbar = new BackpackOverlayScrollbar();

    public void sync(ItemStack i, ItemStack o, ItemStack[] r, int s) {
        input = i.copy();
        output = o.copy();
        recipes = r == null ? new ItemStack[0] : r;
        selected = s;
        visible = true;
        width = layout.panelWidth;
        height = layout.panelHeight;
        scrollbar.update(x + layout.scrollerX, y + layout.scrollerY, layout.recipeRows, totalRows());
    }

    public void render(Screen s, GuiGraphics g, Minecraft mc) {
        if (!visible)
            return;
        g.fill(x - 4, y - 4, x + layout.panelWidth - 4, y + layout.panelHeight + 4, -871362544);
        g.fill(x - 4, y - 4, x + layout.panelWidth - 4, y - 3, -11184811);
        g.drawString(mc.font, "Stonecutter", x + 4, y + 3, 16777215, true);
        new BackpackOverlaySlot(0, input).renderAt(g, mc, x + layout.inputBoxX, y + layout.inputBoxY);
        new BackpackOverlaySlot(1, output).renderAt(g, mc, x + layout.outputBoxX, y + layout.outputBoxY);
        scrollbar.update(x + layout.scrollerX, y + layout.scrollerY, layout.recipeRows, totalRows());
        int firstRecipe = scrollbar.row() * layout.recipeColumns;
        for (int n = 0; n < layout.recipeSlotCount && firstRecipe + n < recipes.length; n++) {
            int i = firstRecipe + n;
            new BackpackOverlaySlot(i, recipes[i]).renderAt(g, mc,
                    x + layout.recipeBoxX + (n % layout.recipeColumns) * layout.slotSize,
                    y + layout.recipeBoxY + (n / layout.recipeColumns) * layout.slotSize);
            if (i == selected)
                new BackpackOverlaySlot(i, recipes[i]).renderDragHighlight(g,
                        x + layout.recipeBoxX + (n % layout.recipeColumns) * layout.slotSize,
                        y + layout.recipeBoxY + (n / layout.recipeColumns) * layout.slotSize);
        }
        scrollbar.render(g);
    }

    private boolean mousePressedSlots(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!visible)
            return false;
        int mx = (int) e.getMouseX() - x, my = (int) e.getMouseY() - y;
        if (mx >= layout.inputBoxX && mx < layout.inputBoxX + layout.slotSize && my >= layout.inputBoxY
                && my < layout.inputBoxY + layout.slotSize) {
            ClientPacketSender.stonecutterSlot(0, e.getButton(), selected, false, carried(e));
            return true;
        }
        if (mx >= layout.outputBoxX && mx < layout.outputBoxX + layout.slotSize && my >= layout.outputBoxY
                && my < layout.outputBoxY + layout.slotSize) {
            ClientPacketSender.stonecutterSlot(2, e.getButton(), selected, Screen.hasShiftDown(), carried(e));
            return true;
        }
        if (my >= layout.recipeBoxY && my < layout.recipeBoxY + layout.recipeRows * layout.slotSize
                && mx >= layout.recipeBoxX && mx < layout.recipeBoxX + layout.recipeColumns * layout.slotSize) {
            int n = (my - layout.recipeBoxY) / layout.slotSize * layout.recipeColumns
                    + ((mx - layout.recipeBoxX) / layout.slotSize);
            int firstRecipe = scrollbar.row() * layout.recipeColumns;
            if (n < layout.recipeSlotCount && firstRecipe + n < recipes.length) {
                selected = firstRecipe + n;
                ClientPacketSender.stonecutterSlot(1, 0, selected, false, carried(e));
            }
            return true;
        }
        return panelInteractiveContains(e.getMouseX(), e.getMouseY(), 0, 0);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!visible)
            return false;
        if (scrollbar.press(e.getMouseX(), e.getMouseY()))
            return true;
        return mousePressedSlots(e);
    }

    public boolean mouseDragged(ScreenEvent.MouseDragged.Pre e) {
        return scrollbar.drag(e.getMouseY());
    }

    public boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre e) {
        return scrollbar.release();
    }

    public boolean mouseScrolled(ScreenEvent.MouseScrolled.Pre e) {
        if (!visible)
            return false;
        double mx = e.getMouseX() - x;
        double my = e.getMouseY() - y;
        boolean overRecipeArea = mx >= layout.recipeBoxX
                && mx < layout.recipeBoxX + layout.recipeColumns * layout.slotSize
                && my >= layout.recipeBoxY
                && my < layout.recipeBoxY + layout.recipeRows * layout.slotSize;
        boolean overScroller = mx >= layout.scrollerX
                && mx < layout.scrollerX + layout.scrollerWidth
                && my >= layout.scrollerY
                && my < layout.scrollerY + layout.scrollerHeight;
        return (overRecipeArea || overScroller) && scrollbar.scroll(e.getScrollDeltaY());
    }

    private int totalRows() {
        return Math.max(1, (recipes.length + layout.recipeColumns - 1) / layout.recipeColumns);
    }

    private ItemStack carried(ScreenEvent.MouseButtonPressed.Pre e) {
        return e.getScreen() instanceof AbstractContainerScreen<?> c ? c.getMenu().getCarried() : ItemStack.EMPTY;
    }

    public void renderTooltip(GuiGraphics g, double mx, double my) {
        if (!visible)
            return;
        BackpackOverlaySlot inputSlot = new BackpackOverlaySlot(0, input);
        BackpackOverlaySlot outputSlot = new BackpackOverlaySlot(1, output);
        inputSlot.renderHighlightAt(g, x + layout.inputBoxX, y + layout.inputBoxY, mx, my);
        outputSlot.renderHighlightAt(g, x + layout.outputBoxX, y + layout.outputBoxY, mx, my);
        inputSlot.renderTooltip(g, Minecraft.getInstance(), x + layout.inputBoxX, y + layout.inputBoxY, mx, my);
        outputSlot.renderTooltip(g, Minecraft.getInstance(), x + layout.outputBoxX, y + layout.outputBoxY, mx, my);
        int firstRecipe = scrollbar.row() * layout.recipeColumns;
        for (int n = 0; n < layout.recipeSlotCount && firstRecipe + n < recipes.length; n++) {
            int i = firstRecipe + n;
            int sx = x + layout.recipeBoxX + (n % layout.recipeColumns) * layout.slotSize;
            int sy = y + layout.recipeBoxY + (n / layout.recipeColumns) * layout.slotSize;
            BackpackOverlaySlot slot = new BackpackOverlaySlot(i, recipes[i]);
            slot.renderHighlightAt(g, sx, sy, mx, my);
            slot.renderTooltip(g, Minecraft.getInstance(), sx, sy, mx, my);
        }
    }

    public boolean panelInteractiveContains(double mx, double my, int sw, int sh) {
        return visible && mx >= x - 4 && mx < x + layout.panelWidth - 4 && my >= y - 4
                && my < y + layout.panelHeight + 4;
    }
}
