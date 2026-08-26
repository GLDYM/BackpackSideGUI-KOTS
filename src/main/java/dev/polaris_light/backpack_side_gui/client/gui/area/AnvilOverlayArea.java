package dev.polaris_light.backpack_side_gui.client.gui.area;

import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.BackpackOverlaySlot;
import dev.polaris_light.backpack_side_gui.client.gui.element.OverlayTextInput;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Anvil utility area: name input above, two inputs and one result below. */
public final class AnvilOverlayArea extends IOverlayArea {
    public static final class Layout {
        public int inputBoxX = 0, inputBoxY = 15, inputBoxWidth = 90;
        public int firstSlotX = 0, secondSlotX = 36, resultSlotX = 72;
        public int slotsY = 35, slotSize = 18, panelWidth = 98, panelHeight = 68;
    }

    public final Layout layout = new Layout();
    private final OverlayTextInput nameInput = new OverlayTextInput(layout.inputBoxWidth);
    private final ItemStack[] stacks = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    private final BackpackOverlaySlot[] slots = { new BackpackOverlaySlot(0, ItemStack.EMPTY),
            new BackpackOverlaySlot(1, ItemStack.EMPTY), new BackpackOverlaySlot(2, ItemStack.EMPTY) };
    private int cost;

    public void sync(ItemStack first, ItemStack second, ItemStack result, int cost, String name) {
        stacks[0] = first == null ? ItemStack.EMPTY : first.copy();
        stacks[1] = second == null ? ItemStack.EMPTY : second.copy();
        stacks[2] = result == null ? ItemStack.EMPTY : result.copy();
        for (int i = 0; i < slots.length; i++)
            slots[i] = new BackpackOverlaySlot(i, stacks[i]);
        nameInput.setValue(name);
        this.cost = Math.max(0, cost);
        width = layout.panelWidth;
        height = layout.panelHeight;
        visible = true;
        nameInput.setVisible(true);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        nameInput.setVisible(visible);
    }

    public void render(Screen screen, GuiGraphics g, Minecraft mc) {
        if (!visible)
            return;
        int right = x + layout.panelWidth - 4, bottom = y + layout.panelHeight + 4;
        g.fill(x - 4, y - 4, right, bottom, -871362544);
        g.fill(x - 4, y - 4, right, y - 3, -11184811);
        g.drawString(mc.font, Component.translatable("text.backpack_side_gui.utility.anvil"), x + 4, y + 3, 16777215,
                true);
        nameInput.setBounds(x + layout.inputBoxX, y + layout.inputBoxY);
        nameInput.render(g, mc, Component.translatable("text.backpack_side_gui.anvil.rename_hint"));
        int[] xs = { layout.firstSlotX, layout.secondSlotX, layout.resultSlotX };
        for (int i = 0; i < slots.length; i++)
            slots[i].renderAt(g, mc, x + xs[i], y + layout.slotsY);
        if (cost > 0)
            g.drawString(mc.font, Component.translatable("text.backpack_side_gui.anvil.cost", cost), x + 2,
                    y + layout.slotsY + layout.slotSize + 4, 8454016, true);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!visible)
            return false;
        if (nameInput.mousePressed(e.getMouseX(), e.getMouseY()))
            return true;
        int[] xs = { layout.firstSlotX, layout.secondSlotX, layout.resultSlotX };
        for (int i = 0; i < xs.length; i++)
            if (e.getMouseX() >= x + xs[i] && e.getMouseX() < x + xs[i] + layout.slotSize
                    && e.getMouseY() >= y + layout.slotsY && e.getMouseY() < y + layout.slotsY + layout.slotSize) {
                ItemStack carried = e.getScreen() instanceof AbstractContainerScreen<?> c ? c.getMenu().getCarried()
                        : ItemStack.EMPTY;
                ClientPacketSender.anvilSlot(i, e.getButton(), carried);
                return true;
            }
        return false;
    }

    public boolean keyPressed(int key) {
        String before = nameInput.value();
        boolean handled = visible && nameInput.keyPressed(key);
        if (handled && !before.equals(nameInput.value()))
            ClientPacketSender.anvilRename(nameInput.value());
        return handled;
    }

    public boolean charTyped(char c) {
        String before = nameInput.value();
        boolean handled = visible && nameInput.charTyped(c);
        if (handled && !before.equals(nameInput.value()))
            ClientPacketSender.anvilRename(nameInput.value());
        return handled;
    }

    public void renderTooltip(GuiGraphics g, double mx, double my) {
        if (!visible)
            return;
        int[] xs = { layout.firstSlotX, layout.secondSlotX, layout.resultSlotX };
        for (int i = 0; i < slots.length; i++)
            slots[i].renderHighlightAt(g, x + xs[i], y + layout.slotsY, mx, my);
    }

    public boolean panelInteractiveContains(double mx, double my, int sw, int sh) {
        return visible && mx >= x - 4 && mx < x + layout.panelWidth - 4 && my >= y - 4
                && my < y + layout.panelHeight + 4;
    }
}
