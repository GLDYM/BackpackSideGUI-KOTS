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
        private int INPUT_BOX_X = 0, INPUT_BOX_Y = 15, INPUT_BOX_WIDTH = 90;
        private int FIRST_SLOT_X = 0, SECOND_SLOT_X = 36, RESULT_SLOT_X = 72;
        private int SLOTS_Y = 35, SLOT_SIZE = 18, PANEL_WIDTH = 98, PANEL_HEIGHT = 68;
    }

    public final Layout layout = new Layout();
    private final OverlayTextInput nameInput = new OverlayTextInput(layout.INPUT_BOX_WIDTH);
    private final ItemStack[] stacks = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    private final BackpackOverlaySlot[] slots = { new BackpackOverlaySlot(0, ItemStack.EMPTY),
            new BackpackOverlaySlot(1, ItemStack.EMPTY), new BackpackOverlaySlot(2, ItemStack.EMPTY) };
    private int cost;
    private long lastLeftClickTime;
    private int lastLeftClickSlot = -1;

    public void sync(ItemStack first, ItemStack second, ItemStack result, int cost, String name) {
        stacks[0] = first == null ? ItemStack.EMPTY : first.copy();
        stacks[1] = second == null ? ItemStack.EMPTY : second.copy();
        stacks[2] = result == null ? ItemStack.EMPTY : result.copy();
        for (int i = 0; i < slots.length; i++)
            slots[i] = new BackpackOverlaySlot(i, stacks[i]);
        nameInput.setValue(name);
        this.cost = Math.max(0, cost);
        width = layout.PANEL_WIDTH;
        height = layout.PANEL_HEIGHT;
        visible = true;
        nameInput.setVisible(true);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        nameInput.setVisible(visible);
    }

    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft) {
        if (!visible)
            return;
        int right = x + layout.PANEL_WIDTH - 4, bottom = y + layout.PANEL_HEIGHT + 4;
        graphics.fill(x - 4, y - 4, right, bottom, -871362544);
        graphics.fill(x - 4, y - 4, right, y - 3, -11184811);
        graphics.drawString(minecraft.font, Component.translatable("text.backpack_side_gui.utility.anvil"), x + 4,
                y + 3, 16777215,
                true);
        nameInput.setBounds(x + layout.INPUT_BOX_X, y + layout.INPUT_BOX_Y);
        nameInput.render(graphics, minecraft, Component.translatable("text.backpack_side_gui.anvil.rename_hint"));
        int[] xs = { layout.FIRST_SLOT_X, layout.SECOND_SLOT_X, layout.RESULT_SLOT_X };
        for (int i = 0; i < slots.length; i++)
            slots[i].renderAt(graphics, minecraft, x + xs[i], y + layout.SLOTS_Y);
        if (cost > 0)
            graphics.drawString(minecraft.font, Component.translatable("text.backpack_side_gui.anvil.cost", cost),
                    x + 2,
                    y + layout.SLOTS_Y + layout.SLOT_SIZE + 4, 8454016, true);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible)
            return false;
        if (nameInput.mousePressed(event.getMouseX(), event.getMouseY()))
            return true;
        int[] xs = { layout.FIRST_SLOT_X, layout.SECOND_SLOT_X, layout.RESULT_SLOT_X };
        for (int i = 0; i < xs.length; i++)
            if (event.getMouseX() >= x + xs[i] && event.getMouseX() < x + xs[i] + layout.SLOT_SIZE
                    && event.getMouseY() >= y + layout.SLOTS_Y
                    && event.getMouseY() < y + layout.SLOTS_Y + layout.SLOT_SIZE) {
                ItemStack carried = event.getScreen() instanceof AbstractContainerScreen<?> c ? c.getMenu().getCarried()
                        : ItemStack.EMPTY;
                long now = net.minecraft.Util.getMillis();
                boolean dbl = i < 2 && event.getButton() == 0 && i == lastLeftClickSlot
                        && now - lastLeftClickTime < 250;
                if (event.getButton() == 0) {
                    lastLeftClickSlot = i;
                    lastLeftClickTime = now;
                }
                ClientPacketSender.anvilSlot(i, dbl ? 6 : event.getButton(), carried);
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

    public boolean charTyped(char character) {
        String before = nameInput.value();
        boolean handled = visible && nameInput.charTyped(character);
        if (handled && !before.equals(nameInput.value()))
            ClientPacketSender.anvilRename(nameInput.value());
        return handled;
    }

    public void renderTooltip(GuiGraphics graphics, double mouseX, double mouseY) {
        if (!visible)
            return;
        int[] xs = { layout.FIRST_SLOT_X, layout.SECOND_SLOT_X, layout.RESULT_SLOT_X };
        for (int i = 0; i < slots.length; i++)
            slots[i].renderHighlightAt(graphics, x + xs[i], y + layout.SLOTS_Y, mouseX, mouseY);
        for (int i = 0; i < slots.length; i++)
            slots[i].renderTooltip(graphics, Minecraft.getInstance(), x + xs[i], y + layout.SLOTS_Y, mouseX, mouseY);
    }

    public boolean panelInteractiveContains(double mouseX, double mouseY, int sw, int sh) {
        return visible && mouseX >= x - 4 && mouseX < x + layout.PANEL_WIDTH - 4 && mouseY >= y - 4
                && mouseY < y + layout.PANEL_HEIGHT + 4;
    }
}

