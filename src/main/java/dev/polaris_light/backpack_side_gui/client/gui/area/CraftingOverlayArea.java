package dev.polaris_light.backpack_side_gui.client.gui.area;

import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlaySlot;
import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import dev.polaris_light.backpack_side_gui.network.ModNetwork;

/** Crafting utility layout; slot data remains server-owned. */
public final class CraftingOverlayArea extends IOverlayArea {
    public static final class Layout {
        public int inputX = 0, inputY = 15;
        public int resultX = 72, resultY = 33;
        public int slotSize = 18, panelWidth = 98, panelHeight = 71;
    }
    public final Layout layout = new Layout();
    private final ItemStack[] stacks = new ItemStack[10];
    private final BackpackOverlaySlot[] slots = new BackpackOverlaySlot[10];

    public CraftingOverlayArea() {
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = ItemStack.EMPTY;
            slots[i] = new BackpackOverlaySlot(i, ItemStack.EMPTY);
        }
    }

    public void sync(ItemStack[] input, ItemStack result) {
        for (int i = 0; i < 9; i++) {
            stacks[i] = input != null && i < input.length && input[i] != null ? input[i].copy() : ItemStack.EMPTY;
            slots[i] = new BackpackOverlaySlot(i, stacks[i]);
        }
        stacks[9] = result == null ? ItemStack.EMPTY : result.copy();
        slots[9] = new BackpackOverlaySlot(9, stacks[9]);
        width = layout.panelWidth;
        height = layout.panelHeight;
        visible = true;
    }

    @Override public void render(Screen screen, GuiGraphics g, Minecraft mc) {
        if (!visible) return;
        int right = x + layout.panelWidth - 4, bottom = y + layout.panelHeight + 4;
        g.fill(x - 4, y - 4, right, bottom, -871362544);
        g.fill(x - 4, y - 4, right, y - 3, -11184811);
        g.drawString(mc.font, "Crafting", x + 4, y + 3, 16777215, true);
        for (int i = 0; i < 9; i++)
            slots[i].renderAt(g, mc, x + layout.inputX + (i % 3) * layout.slotSize,
                    y + layout.inputY + (i / 3) * layout.slotSize);
        // g.drawString(mc.font, "=", x + 82, y + 33, 16777215, true);
        slots[9].renderAt(g, mc, x + layout.resultX, y + layout.resultY);
    }

    public void renderTooltip(GuiGraphics g, double mx, double my) {
        if (!visible) return;
        for (int i = 0; i < 9; i++) slots[i].renderHighlightAt(g, x + layout.inputX + (i % 3) * layout.slotSize,
                y + layout.inputY + (i / 3) * layout.slotSize, mx, my);
        slots[9].renderHighlightAt(g, x + layout.resultX, y + layout.resultY, mx, my);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!visible) return false;
        for (int i = 0; i < 10; i++) {
            int sx = i == 9 ? x + layout.resultX : x + layout.inputX + (i % 3) * layout.slotSize;
            int sy = i == 9 ? y + layout.resultY : y + layout.inputY + (i / 3) * layout.slotSize;
            if (e.getMouseX() >= sx && e.getMouseX() < sx + layout.slotSize
                    && e.getMouseY() >= sy && e.getMouseY() < sy + layout.slotSize) {
                ItemStack carried = e.getScreen() instanceof AbstractContainerScreen<?> c ? c.getMenu().getCarried() : ItemStack.EMPTY;
                ModNetwork.requestCraftingClick(i, e.getButton(), net.minecraft.client.gui.screens.Screen.hasShiftDown(), carried);
                return true;
            }
        }
        return false;
    }

    public boolean panelInteractiveContains(double mx, double my, int sw, int sh) {
        return visible && mx >= x - 4 && mx < x + layout.panelWidth - 4
                && my >= y - 4 && my < y + layout.panelHeight + 4;
    }
}
