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
        public static final int INPUT_X = 0, INPUT_Y = 15, FUEL_X = 0, FUEL_Y = 43, OUTPUT_X = 54, OUTPUT_Y = 29;
        public static final int PROGRESS_X = 24, PROGRESS_Y = 36, PROGRESS_WIDTH = 24, BURN_X = 1, BURN_Y = 36, BURN_WIDTH = 16;
        public static final int SLOT_SIZE = 18, PANEL_WIDTH = 80, PANEL_HEIGHT = 68;
    }

    private final ItemStack[] stacks = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    private final BackpackOverlaySlot[] slots = { new BackpackOverlaySlot(0, ItemStack.EMPTY),
            new BackpackOverlaySlot(1, ItemStack.EMPTY), new BackpackOverlaySlot(2, ItemStack.EMPTY) };
    private long burnFinish, cookFinish;
    private int burnTotal, cookTotal;
    private boolean cooking;
    private long lastLeftClickTime;
    private int lastLeftClickSlot = -1;

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
        width = Layout.PANEL_WIDTH;
        height = Layout.PANEL_HEIGHT;
        visible = true;
    }

    @Override
    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft) {
        if (!visible)
            return;
        graphics.fill(x - 4, y - 4, x + Layout.PANEL_WIDTH - 4, y + Layout.PANEL_HEIGHT + 4, -871362544);
        graphics.fill(x - 4, y - 4, x + Layout.PANEL_WIDTH - 4, y - 3, -11184811);
        graphics.drawString(minecraft.font, "Furnace", x + 4, y + 3, 16777215, true);
        slots[0].renderAt(graphics, minecraft, x + Layout.INPUT_X, y + Layout.INPUT_Y);
        slots[1].renderAt(graphics, minecraft, x + Layout.FUEL_X, y + Layout.FUEL_Y);
        slots[2].renderAt(graphics, minecraft, x + Layout.OUTPUT_X, y + Layout.OUTPUT_Y);
        int cook = !cooking || cookFinish <= 0 || cookTotal <= 0 ? 0
                : Math.min(Layout.PROGRESS_WIDTH,
                        Math.max(0, (int) (cookTotal - Math.max(0, cookFinish)) * Layout.PROGRESS_WIDTH / cookTotal));
        int burn = burnTotal <= 0 ? 0
                : Math.min(Layout.BURN_WIDTH,
                        Math.max(0, (int) Math.max(0, burnFinish) * Layout.BURN_WIDTH / burnTotal));
        // Burning and cooking are independent: cooking progress can remain
        // visible (and decrease) after fuel runs out, while the flame hides.
        if (cook > 0) {
            graphics.fill(x + Layout.PROGRESS_X, y + Layout.PROGRESS_Y,
                    x + Layout.PROGRESS_X + cook, y + Layout.PROGRESS_Y + 4, -256);
        }
        if (burn > 0) {
            graphics.fill(x + Layout.BURN_X, y + Layout.BURN_Y,
                    x + Layout.BURN_X + burn, y + Layout.BURN_Y + 4, -65536);
        }
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible)
            return false;
        for (int i = 0; i < 3; i++) {
            int sx = x + (i == 0 ? Layout.INPUT_X : i == 1 ? Layout.FUEL_X : Layout.OUTPUT_X),
                    sy = y + (i == 0 ? Layout.INPUT_Y : i == 1 ? Layout.FUEL_Y : Layout.OUTPUT_Y);
            if (event.getMouseX() >= sx && event.getMouseX() < sx + 18 && event.getMouseY() >= sy
                    && event.getMouseY() < sy + 18) {
                ItemStack c = event.getScreen() instanceof AbstractContainerScreen<?> a ? a.getMenu().getCarried()
                        : ItemStack.EMPTY;
                long now = net.minecraft.Util.getMillis();
                boolean dbl = i < 2 && event.getButton() == 0 && i == lastLeftClickSlot
                        && now - lastLeftClickTime < 250;
                if (event.getButton() == 0) {
                    lastLeftClickSlot = i;
                    lastLeftClickTime = now;
                }
                ClientPacketSender.furnaceSlot(i, dbl ? 6 : event.getButton(), c);
                return true;
            }
        }
        return false;
    }

    public void renderTooltip(GuiGraphics graphics, double mouseX, double mouseY) {
        if (!visible)
            return;
        for (int i = 0; i < 3; i++) {
            int sx = x + (i == 0 ? Layout.INPUT_X : i == 1 ? Layout.FUEL_X : Layout.OUTPUT_X),
                    sy = y + (i == 0 ? Layout.INPUT_Y : i == 1 ? Layout.FUEL_Y : Layout.OUTPUT_Y);
            slots[i].renderHighlightAt(graphics, sx, sy, mouseX, mouseY);
            slots[i].renderTooltip(graphics, Minecraft.getInstance(), sx, sy, mouseX, mouseY);
        }
    }

    public boolean panelInteractiveContains(double mouseX, double mouseY, int sw, int sh) {
        return visible && mouseX >= x - 4 && mouseX < x + Layout.PANEL_WIDTH - 4 && mouseY >= y - 4
                && mouseY < y + Layout.PANEL_HEIGHT + 4;
    }
}
