package dev.polaris_light.backpack_side_gui.client.gui.area;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlaySlot;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

public final class SmithingOverlayArea extends IOverlayArea {
    public static final class Layout {
        public int firstSlotX = 0, secondSlotX = 18, thirdSlotX = 36, resultSlotX = 72;
        public int slotsY = 15, slotSize = 18, panelWidth = 98, panelHeight = 35;
    }

    public final Layout layout = new Layout();
    private final ItemStack[] stacks = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    private final BackpackOverlaySlot[] slots = {
            new BackpackOverlaySlot(0, ItemStack.EMPTY),
            new BackpackOverlaySlot(1, ItemStack.EMPTY),
            new BackpackOverlaySlot(2, ItemStack.EMPTY),
            new BackpackOverlaySlot(3, ItemStack.EMPTY)
    };

    public void sync(ItemStack a, ItemStack b, ItemStack c, ItemStack result) {
        stacks[0] = a.copy();
        stacks[1] = b.copy();
        stacks[2] = c.copy();
        stacks[3] = result.copy();
        for (int i = 0; i < slots.length; i++)
            slots[i] = new BackpackOverlaySlot(i, stacks[i]);
        visible = true;
        // Match the legacy utility panel: 162 px content plus 4 px margins,
        // with the smithing slots placed on the same baseline as the backup.
        width = layout.panelWidth;
        height = layout.panelHeight;
    }

    public void render(Screen s, GuiGraphics g, Minecraft mc) {
        if (!visible)
            return;
        // The panel can become visible before the first sync packet arrives.
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null)
                slots[i] = new BackpackOverlaySlot(i, stacks[i]);
        }
        int panelRight = x + layout.panelWidth - 4;
        int panelBottom = y + layout.panelHeight + 4;
        g.fill(x - 4, y - 4, panelRight, panelBottom, -871362544);
        g.fill(x - 4, y - 4, panelRight, y - 3, -11184811);
        g.drawString(mc.font, "Smithing", x + 4, y + 3, 16777215, true);
        slots[0].renderAt(g, mc, x + layout.firstSlotX, y + layout.slotsY);
        slots[1].renderAt(g, mc, x + layout.secondSlotX, y + layout.slotsY);
        slots[2].renderAt(g, mc, x + layout.thirdSlotX, y + layout.slotsY);
        // g.drawString(mc.font, "=", x + 82, y + 38, 16777215, true);
        slots[3].renderAt(g, mc, x + layout.resultSlotX, y + layout.slotsY);
    }

    public boolean mousePressed(net.neoforged.neoforge.client.event.ScreenEvent.MouseButtonPressed.Pre e) {
        if (!visible || e.getMouseY() < y + layout.slotsY || e.getMouseY() >= y + layout.slotsY + layout.slotSize)
            return false;
        int[] slotX = { x + layout.firstSlotX, x + layout.secondSlotX, x + layout.thirdSlotX, x + layout.resultSlotX };
        for (int i = 0; i < slotX.length; i++) {
            if (e.getMouseX() >= slotX[i] && e.getMouseX() < slotX[i] + layout.slotSize) {
                ItemStack carried = e.getScreen() instanceof AbstractContainerScreen<?> c ? c.getMenu().getCarried()
                        : ItemStack.EMPTY;
                ClientPacketSender.smithingSlot(i, e.getButton(), carried);
                return true;
            }
        }
        // Being on the slot row is not enough: gaps between slots must remain
        // available to the rest of the overlay and underlying screen policy.
        return false;
    }

    public void renderTooltip(GuiGraphics g, double mouseX, double mouseY) {
        if (!visible)
            return;
        slots[0].renderHighlightAt(g, x + layout.firstSlotX, y + layout.slotsY, mouseX, mouseY);
        slots[1].renderHighlightAt(g, x + layout.secondSlotX, y + layout.slotsY, mouseX, mouseY);
        slots[2].renderHighlightAt(g, x + layout.thirdSlotX, y + layout.slotsY, mouseX, mouseY);
        slots[3].renderHighlightAt(g, x + layout.resultSlotX, y + layout.slotsY, mouseX, mouseY);
    }

    public boolean panelInteractiveContains(double mx, double my, int sw, int sh) {
        int panelRight = x + layout.panelWidth - 4;
        int panelBottom = y + layout.panelHeight + 4;
        return visible && mx >= x - 4 && mx < panelRight
                && my >= y - 4 && my < panelBottom;
    }
}
