package dev.polaris_light.backpack_side_gui.client.gui.area;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlaySlot;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import dev.polaris_light.backpack_side_gui.network.c2s.HandlerSlotClicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;

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
    private boolean itemDragging;
    private int dragButton;
    private final java.util.List<Integer> dragSlots = new java.util.ArrayList<>();
    private ItemStack dragCarried = ItemStack.EMPTY;
    private long lastLeftClickTime;
    private int lastLeftClickSlot = -1;

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

    @Override
    public void render(Screen screen, GuiGraphics g, Minecraft mc) {
        if (!visible)
            return;
        int right = x + layout.panelWidth - 4, bottom = y + layout.panelHeight + 4;
        g.fill(x - 4, y - 4, right, bottom, -871362544);
        g.fill(x - 4, y - 4, right, y - 3, -11184811);
        g.drawString(mc.font, "Crafting", x + 4, y + 3, 16777215, true);
        for (int i = 0; i < 9; i++)
            slots[i].renderAt(g, mc, x + layout.inputX + (i % 3) * layout.slotSize,
                    y + layout.inputY + (i / 3) * layout.slotSize);
        // g.drawString(mc.font, "=", x + 82, y + 33, 16777215, true);
        slots[9].renderAt(g, mc, x + layout.resultX, y + layout.resultY);
        if (itemDragging)
            for (int i : dragSlots) {
                int amount = HandlerSlotClicker.dragAmount(dragCarried, dragButton, dragSlots.size());
                ItemStack base = stacks[i];
                if (base.isEmpty() || ItemStack.isSameItemSameComponents(base, dragCarried)) {
                    slots[i].renderPreview(g, mc, x + layout.inputX + (i % 3) * 18,
                            y + layout.inputY + (i / 3) * 18,
                            (base.isEmpty() ? dragCarried : base).copyWithCount(base.getCount() + amount));
                    slots[i].renderDragHighlight(g, x + layout.inputX + (i % 3) * 18,
                            y + layout.inputY + (i / 3) * 18);
                }
            }
    }

    public void renderTooltip(GuiGraphics g, double mx, double my) {
        if (!visible)
            return;
        for (int i = 0; i < 9; i++)
            slots[i].renderHighlightAt(g, x + layout.inputX + (i % 3) * layout.slotSize,
                    y + layout.inputY + (i / 3) * layout.slotSize, mx, my);
        slots[9].renderHighlightAt(g, x + layout.resultX, y + layout.resultY, mx, my);
        for (int i = 0; i < 9; i++)
            slots[i].renderTooltip(g, Minecraft.getInstance(), x + layout.inputX + (i % 3) * layout.slotSize,
                    y + layout.inputY + (i / 3) * layout.slotSize, mx, my);
        slots[9].renderTooltip(g, Minecraft.getInstance(), x + layout.resultX, y + layout.resultY, mx, my);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!visible)
            return false;
        for (int i = 0; i < 10; i++) {
            int sx = i == 9 ? x + layout.resultX : x + layout.inputX + (i % 3) * layout.slotSize;
            int sy = i == 9 ? y + layout.resultY : y + layout.inputY + (i / 3) * layout.slotSize;
            if (e.getMouseX() >= sx && e.getMouseX() < sx + layout.slotSize
                    && e.getMouseY() >= sy && e.getMouseY() < sy + layout.slotSize) {
                ItemStack carried = e.getScreen() instanceof AbstractContainerScreen<?> c ? c.getMenu().getCarried()
                        : ItemStack.EMPTY;
                long now = net.minecraft.Util.getMillis();
                boolean dbl = i < 9 && e.getButton() == 0 && i == lastLeftClickSlot && now - lastLeftClickTime < 250;
                if (e.getButton() == 0) {
                    lastLeftClickSlot = i;
                    lastLeftClickTime = now;
                }
                if (dbl) {
                    itemDragging = false;
                    dragSlots.clear();
                    ClientPacketSender.craftingSlot(i, 6, false, carried);
                    return true;
                }
                if (i < 9 && !carried.isEmpty()) {
                    itemDragging = true;
                    dragButton = e.getButton();
                    dragCarried = carried.copy();
                    dragSlots.clear();
                    dragSlots.add(i);
                    updateDragPreview(e);
                } else
                    ClientPacketSender.craftingSlot(i, e.getButton(),
                            net.minecraft.client.gui.screens.Screen.hasShiftDown(), carried);
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(ScreenEvent.MouseDragged.Pre e) {
        if (!itemDragging)
            return false;
        int col = (int) ((e.getMouseX() - x - layout.inputX) / 18),
                row = (int) ((e.getMouseY() - y - layout.inputY) / 18);
        if (col >= 0 && col < 3 && row >= 0 && row < 3) {
            int i = row * 3 + col;
            if (!dragSlots.contains(i))
                dragSlots.add(i);
            updateDragPreview(e);
        }
        return true;
    }

    public boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre e) {
        if (!itemDragging)
            return false;
        ItemStack carried = dragCarried;
        if (dragSlots.size() > 1 && !carried.isEmpty())
            ClientPacketSender.craftingDrag(dragSlots, dragButton, carried);
        else if (dragSlots.size() == 1 && !carried.isEmpty())
            ClientPacketSender.craftingSlot(dragSlots.get(0), dragButton, false, carried);
        itemDragging = false;
        dragSlots.clear();
        dragCarried = ItemStack.EMPTY;
        return true;
    }

    private void updateDragPreview(ScreenEvent.MouseButtonPressed.Pre e) {
        if (e.getScreen() instanceof AbstractContainerScreen<?> c)
            c.getMenu().setCarried(HandlerSlotClicker.dragPreviewCursor(dragCarried, dragButton, dragSlots.size()));
    }

    private void updateDragPreview(ScreenEvent.MouseDragged.Pre e) {
        if (e.getScreen() instanceof AbstractContainerScreen<?> c)
            c.getMenu().setCarried(HandlerSlotClicker.dragPreviewCursor(dragCarried, dragButton, dragSlots.size()));
    }

    public boolean panelInteractiveContains(double mx, double my, int sw, int sh) {
        return visible && mx >= x - 4 && mx < x + layout.panelWidth - 4
                && my >= y - 4 && my < y + layout.panelHeight + 4;
    }
}
