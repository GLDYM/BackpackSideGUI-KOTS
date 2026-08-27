package dev.polaris_light.backpack_side_gui.client.gui.area;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlaySlot;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class SmithingOverlayArea extends IOverlayArea {
    public static final class Layout {
        public int FIRST_SLOT_X = 0, SECOND_SLOT_X = 18, THIRD_SLOT_X = 36, RESULT_SLOT_X = 72;
        public int SLOTS_Y = 15, SLOT_SIZE = 18, PANEL_WIDTH = 98, PANEL_HEIGHT = 35;
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
        width = layout.PANEL_WIDTH;
        height = layout.PANEL_HEIGHT;
    }

    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft) {
        if (!visible)
            return;
        // The panel can become visible before the first sync packet arrives.
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null)
                slots[i] = new BackpackOverlaySlot(i, stacks[i]);
        }
        int panelRight = x + layout.PANEL_WIDTH - 4;
        int panelBottom = y + layout.PANEL_HEIGHT + 4;
        graphics.fill(x - 4, y - 4, panelRight, panelBottom, -871362544);
        graphics.fill(x - 4, y - 4, panelRight, y - 3, -11184811);
        graphics.drawString(minecraft.font, "Smithing", x + 4, y + 3, 16777215, true);
        slots[0].renderAt(graphics, minecraft, x + layout.FIRST_SLOT_X, y + layout.SLOTS_Y);
        slots[1].renderAt(graphics, minecraft, x + layout.SECOND_SLOT_X, y + layout.SLOTS_Y);
        slots[2].renderAt(graphics, minecraft, x + layout.THIRD_SLOT_X, y + layout.SLOTS_Y);
        // graphics.drawString(minecraft.font, "=", x + 82, y + 38, 16777215, true);
        slots[3].renderAt(graphics, minecraft, x + layout.RESULT_SLOT_X, y + layout.SLOTS_Y);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible || event.getMouseY() < y + layout.SLOTS_Y
                || event.getMouseY() >= y + layout.SLOTS_Y + layout.SLOT_SIZE)
            return false;
        int[] slotX = { x + layout.FIRST_SLOT_X, x + layout.SECOND_SLOT_X, x + layout.THIRD_SLOT_X,
                x + layout.RESULT_SLOT_X };
        for (int i = 0; i < slotX.length; i++) {
            if (event.getMouseX() >= slotX[i] && event.getMouseX() < slotX[i] + layout.SLOT_SIZE) {
                ItemStack carried = event.getScreen() instanceof AbstractContainerScreen<?> c ? c.getMenu().getCarried()
                        : ItemStack.EMPTY;
                ClientPacketSender.smithingSlot(i, event.getButton(), carried);
                return true;
            }
        }
        // Being on the slot row is not enough: gaps between slots must remain
        // available to the rest of the overlay and underlying screen policy.
        return false;
    }

    public void renderTooltip(GuiGraphics graphics, double mouseX, double mouseY) {
        if (!visible)
            return;
        slots[0].renderHighlightAt(graphics, x + layout.FIRST_SLOT_X, y + layout.SLOTS_Y, mouseX, mouseY);
        slots[1].renderHighlightAt(graphics, x + layout.SECOND_SLOT_X, y + layout.SLOTS_Y, mouseX, mouseY);
        slots[2].renderHighlightAt(graphics, x + layout.THIRD_SLOT_X, y + layout.SLOTS_Y, mouseX, mouseY);
        slots[3].renderHighlightAt(graphics, x + layout.RESULT_SLOT_X, y + layout.SLOTS_Y, mouseX, mouseY);
        int[] tooltipX = { layout.FIRST_SLOT_X, layout.SECOND_SLOT_X, layout.THIRD_SLOT_X, layout.RESULT_SLOT_X };
        for (int i = 0; i < slots.length; i++)
            slots[i].renderTooltip(graphics, Minecraft.getInstance(), x + tooltipX[i], y + layout.SLOTS_Y, mouseX,
                    mouseY);
    }

    public boolean panelInteractiveContains(double mouseX, double mouseY, int sw, int sh) {
        int panelRight = x + layout.PANEL_WIDTH - 4;
        int panelBottom = y + layout.PANEL_HEIGHT + 4;
        return visible && mouseX >= x - 4 && mouseX < panelRight
                && mouseY >= y - 4 && mouseY < panelBottom;
    }
}
