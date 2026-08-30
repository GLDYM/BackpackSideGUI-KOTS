package dev.polaris_light.backpack_side_gui.client.gui.area;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.GuiConstants;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlaySlot;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import dev.polaris_light.backpack_side_gui.server.inventory.HandlerSlotClicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Crafting utility layout; slot data remains server-owned. */
public final class CraftingOverlayArea extends IOverlayArea {
    public static final class Layout {
        private static final int INPUT_X = 0, INPUT_Y = 15;
        private static final int RESULT_X = 72, RESULT_Y = 33;
        private static final int SLOT_SIZE = GuiConstants.SLOT_SIZE, PANEL_WIDTH = 98, PANEL_HEIGHT = 71;
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
        width = layout.PANEL_WIDTH;
        height = layout.PANEL_HEIGHT;
        visible = true;
    }

    @Override
    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft) {
        if (!visible)
            return;
        int right = x + layout.PANEL_WIDTH - 4, bottom = y + layout.PANEL_HEIGHT + 4;
        graphics.fill(x - 4, y - 4, right, bottom, -871362544);
        graphics.fill(x - 4, y - 4, right, y - 3, -11184811);
        graphics.drawString(minecraft.font, Component.translatable("text.backpack_side_gui.utility.crafting"), x + 4, y + 3,
                16777215, true);
        for (int i = 0; i < 9; i++)
            slots[i].renderAt(graphics, minecraft, x + layout.INPUT_X + (i % 3) * layout.SLOT_SIZE,
                    y + layout.INPUT_Y + (i / 3) * layout.SLOT_SIZE);
        // graphics.drawString(minecraft.font, "=", x + 82, y + 33, 16777215, true);
        slots[9].renderAt(graphics, minecraft, x + layout.RESULT_X, y + layout.RESULT_Y);
        if (itemDragging)
            for (int i : dragSlots) {
                int amount = HandlerSlotClicker.dragAmount(dragCarried, dragButton, dragSlots.size());
                ItemStack base = stacks[i];
                if (base.isEmpty() || ItemStack.isSameItemSameComponents(base, dragCarried)) {
                    slots[i].renderPreview(graphics, minecraft, x + layout.INPUT_X + (i % 3) * 18,
                            y + layout.INPUT_Y + (i / 3) * 18,
                            (base.isEmpty() ? dragCarried : base).copyWithCount(base.getCount() + amount));
                    slots[i].renderDragHighlight(graphics, x + layout.INPUT_X + (i % 3) * 18,
                            y + layout.INPUT_Y + (i / 3) * 18);
                }
            }
    }

    public void renderTooltip(GuiGraphics graphics, double mouseX, double mouseY) {
        if (!visible)
            return;
        for (int i = 0; i < 9; i++)
            slots[i].renderHighlightAt(graphics, x + layout.INPUT_X + (i % 3) * layout.SLOT_SIZE,
                    y + layout.INPUT_Y + (i / 3) * layout.SLOT_SIZE, mouseX, mouseY);
        slots[9].renderHighlightAt(graphics, x + layout.RESULT_X, y + layout.RESULT_Y, mouseX, mouseY);
        for (int i = 0; i < 9; i++)
            slots[i].renderTooltip(graphics, Minecraft.getInstance(), x + layout.INPUT_X + (i % 3) * layout.SLOT_SIZE,
                    y + layout.INPUT_Y + (i / 3) * layout.SLOT_SIZE, mouseX, mouseY);
        slots[9].renderTooltip(graphics, Minecraft.getInstance(), x + layout.RESULT_X, y + layout.RESULT_Y, mouseX,
                mouseY);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible)
            return false;
        for (int i = 0; i < 10; i++) {
            int sx = i == 9 ? x + layout.RESULT_X : x + layout.INPUT_X + (i % 3) * layout.SLOT_SIZE;
            int sy = i == 9 ? y + layout.RESULT_Y : y + layout.INPUT_Y + (i / 3) * layout.SLOT_SIZE;
            if (event.getMouseX() >= sx && event.getMouseX() < sx + layout.SLOT_SIZE
                    && event.getMouseY() >= sy && event.getMouseY() < sy + layout.SLOT_SIZE) {
                ItemStack carried = event.getScreen() instanceof AbstractContainerScreen<?> c ? c.getMenu().getCarried()
                        : ItemStack.EMPTY;
                long now = net.minecraft.Util.getMillis();
                boolean dbl = i < 9 && event.getButton() == 0 && i == lastLeftClickSlot
                        && now - lastLeftClickTime < 250;
                if (event.getButton() == 0) {
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
                    dragButton = event.getButton();
                    dragCarried = carried.copy();
                    dragSlots.clear();
                    dragSlots.add(i);
                    updateDragPreview(event);
                } else
                    ClientPacketSender.craftingSlot(i, event.getButton(),
                            net.minecraft.client.gui.screens.Screen.hasShiftDown(), carried);
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!itemDragging)
            return false;
        int col = (int) ((event.getMouseX() - x - layout.INPUT_X) / GuiConstants.SLOT_SIZE),
                row = (int) ((event.getMouseY() - y - layout.INPUT_Y) / GuiConstants.SLOT_SIZE);
        if (col >= 0 && col < 3 && row >= 0 && row < 3) {
            int i = row * 3 + col;
            if (!dragSlots.contains(i))
                dragSlots.add(i);
            updateDragPreview(event);
        }
        return true;
    }

    public boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
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

    private void updateDragPreview(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> c)
            c.getMenu().setCarried(HandlerSlotClicker.dragPreviewCursor(dragCarried, dragButton, dragSlots.size()));
    }

    private void updateDragPreview(ScreenEvent.MouseDragged.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> c)
            c.getMenu().setCarried(HandlerSlotClicker.dragPreviewCursor(dragCarried, dragButton, dragSlots.size()));
    }

    public boolean panelInteractiveContains(double mouseX, double mouseY, int sw, int sh) {
        return visible && mouseX >= x - 4 && mouseX < x + layout.PANEL_WIDTH - 4
                && mouseY >= y - 4 && mouseY < y + layout.PANEL_HEIGHT + 4;
    }
}


