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

public final class FurnaceOverlayArea extends IOverlayArea {
    public static final class Layout {
        public int INPUT_X = 0, INPUT_Y = 15, FUEL_X = 0, FUEL_Y = 43, OUTPUT_X = 54, OUTPUT_Y = 29;
        public int PROGRESS_X = 24, PROGRESS_Y = 36, PROGRESS_WIDTH = 24, BURN_X = 1, BURN_Y = 36, BURN_WIDTH = 16;
        public int SLOT_SIZE = 18, PANEL_WIDTH = 80, PANEL_HEIGHT = 68;
    }

    public final Layout layout = new Layout();
    private final ItemStack[] stacks = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    private final BackpackOverlaySlot[] slots = { new BackpackOverlaySlot(0, ItemStack.EMPTY),
            new BackpackOverlaySlot(1, ItemStack.EMPTY), new BackpackOverlaySlot(2, ItemStack.EMPTY) };
    private long burnFinish, cookFinish;
    private int burnTotal, cookTotal;
    private boolean cooking;

    public void sync(ItemStack input, ItemStack fuel, ItemStack output, long bf, int bt, long cf, int ct,
            boolean active) {
        ItemStack[] a = { input, fuel, output };
        for (int i = 0; i < 3; i++) {
            stacks[i] = a[i] == null ? ItemStack.EMPTY : a[i].copy();
            slots[i] = new BackpackOverlaySlot(i, stacks[i]);
        }
        burnFinish = bf;
        burnTotal = bt;
        cookFinish = cf;
        cookTotal = ct;
        cooking = active;
        width = layout.PANEL_WIDTH;
        height = layout.PANEL_HEIGHT;
        visible = true;
    }

    @Override
    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft) {
        if (!visible)
            return;
        graphics.fill(x - 4, y - 4, x + layout.PANEL_WIDTH - 4, y + layout.PANEL_HEIGHT + 4, -871362544);
        graphics.fill(x - 4, y - 4, x + layout.PANEL_WIDTH - 4, y - 3, -11184811);
        graphics.drawString(minecraft.font, "Furnace", x + 4, y + 3, 16777215, true);
        slots[0].renderAt(graphics, minecraft, x + layout.INPUT_X, y + layout.INPUT_Y);
        slots[1].renderAt(graphics, minecraft, x + layout.FUEL_X, y + layout.FUEL_Y);
        slots[2].renderAt(graphics, minecraft, x + layout.OUTPUT_X, y + layout.OUTPUT_Y);
        int cook = !cooking || cookFinish <= 0 || cookTotal <= 0 ? 0
                : Math.min(layout.PROGRESS_WIDTH,
                        Math.max(0, (int) (cookTotal - Math.max(0, cookFinish)) * layout.PROGRESS_WIDTH / cookTotal));
        int burn = burnTotal <= 0 ? 0
                : Math.min(layout.BURN_WIDTH,
                        Math.max(0, (int) Math.max(0, burnFinish) * layout.BURN_WIDTH / burnTotal));
        // Burning and cooking are independent: cooking progress can remain
        // visible (and decrease) after fuel runs out, while the flame hides.
        if (cook > 0) {
            graphics.fill(x + layout.PROGRESS_X, y + layout.PROGRESS_Y,
                    x + layout.PROGRESS_X + cook, y + layout.PROGRESS_Y + 4, -256);
        }
        if (burn > 0) {
            graphics.fill(x + layout.BURN_X, y + layout.BURN_Y,
                    x + layout.BURN_X + burn, y + layout.BURN_Y + 4, -65536);
        }
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible)
            return false;
        for (int i = 0; i < 3; i++) {
            int sx = x + (i == 0 ? layout.INPUT_X : i == 1 ? layout.FUEL_X : layout.OUTPUT_X),
                    sy = y + (i == 0 ? layout.INPUT_Y : i == 1 ? layout.FUEL_Y : layout.OUTPUT_Y);
            if (event.getMouseX() >= sx && event.getMouseX() < sx + 18 && event.getMouseY() >= sy
                    && event.getMouseY() < sy + 18) {
                ItemStack c = event.getScreen() instanceof AbstractContainerScreen<?> a ? a.getMenu().getCarried()
                        : ItemStack.EMPTY;
                ClientPacketSender.furnaceSlot(i, event.getButton(), c);
                return true;
            }
        }
        return false;
    }

    public void renderTooltip(GuiGraphics graphics, double mouseX, double mouseY) {
        if (!visible)
            return;
        for (int i = 0; i < 3; i++) {
            int sx = x + (i == 0 ? layout.INPUT_X : i == 1 ? layout.FUEL_X : layout.OUTPUT_X),
                    sy = y + (i == 0 ? layout.INPUT_Y : i == 1 ? layout.FUEL_Y : layout.OUTPUT_Y);
            slots[i].renderHighlightAt(graphics, sx, sy, mouseX, mouseY);
            slots[i].renderTooltip(graphics, Minecraft.getInstance(), sx, sy, mouseX, mouseY);
        }
    }

    public boolean panelInteractiveContains(double mouseX, double mouseY, int sw, int sh) {
        return visible && mouseX >= x - 4 && mouseX < x + layout.PANEL_WIDTH - 4 && mouseY >= y - 4
                && mouseY < y + layout.PANEL_HEIGHT + 4;
    }
}
