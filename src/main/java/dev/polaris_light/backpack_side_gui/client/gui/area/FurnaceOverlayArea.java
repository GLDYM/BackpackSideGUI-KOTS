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
        public int inputX = 0, inputY = 15, fuelX = 0, fuelY = 43, outputX = 54, outputY = 29;
        public int progressX = 24, progressY = 36, progressWidth = 24, burnX = 1, burnY = 36, burnWidth = 16;
        public int slotSize = 18, panelWidth = 80, panelHeight = 68;
    }
    public final Layout layout = new Layout();
    private final ItemStack[] stacks = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    private final BackpackOverlaySlot[] slots = { new BackpackOverlaySlot(0, ItemStack.EMPTY),
            new BackpackOverlaySlot(1, ItemStack.EMPTY), new BackpackOverlaySlot(2, ItemStack.EMPTY) };
    private long burnFinish, cookFinish;
    private int burnTotal, cookTotal;
    private boolean cooking;

    public void sync(ItemStack input, ItemStack fuel, ItemStack output, long bf, int bt, long cf, int ct, boolean active) {
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
        width = layout.panelWidth;
        height = layout.panelHeight;
        visible = true;
    }

    @Override
    public void render(Screen s, GuiGraphics g, Minecraft mc) {
        if (!visible)
            return;
        g.fill(x - 4, y - 4, x + layout.panelWidth - 4, y + layout.panelHeight + 4, -871362544);
        g.fill(x - 4, y - 4, x + layout.panelWidth - 4, y - 3, -11184811);
        g.drawString(mc.font, "Furnace", x + 4, y + 3, 16777215, true);
        slots[0].renderAt(g, mc, x + layout.inputX, y + layout.inputY);
        slots[1].renderAt(g, mc, x + layout.fuelX, y + layout.fuelY);
        slots[2].renderAt(g, mc, x + layout.outputX, y + layout.outputY);
        int cook = !cooking || cookFinish <= 0 || cookTotal <= 0 ? 0 : Math.min(layout.progressWidth,
                Math.max(0, (int) (cookTotal - Math.max(0, cookFinish)) * layout.progressWidth / cookTotal));
        int burn = burnTotal <= 0 ? 0 : Math.min(layout.burnWidth,
                Math.max(0, (int) Math.max(0, burnFinish) * layout.burnWidth / burnTotal));
        // Burning and cooking are independent: cooking progress can remain
        // visible (and decrease) after fuel runs out, while the flame hides.
        if (cook > 0) {
            g.fill(x + layout.progressX, y + layout.progressY,
                    x + layout.progressX + cook, y + layout.progressY + 4, -256);
        }
        if (burn > 0) {
            g.fill(x + layout.burnX, y + layout.burnY,
                    x + layout.burnX + burn, y + layout.burnY + 4, -65536);
        }
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!visible)
            return false;
        for (int i = 0; i < 3; i++) {
            int sx = x + (i == 0 ? layout.inputX : i == 1 ? layout.fuelX : layout.outputX), sy = y + (i == 0 ? layout.inputY : i == 1 ? layout.fuelY : layout.outputY);
            if (e.getMouseX() >= sx && e.getMouseX() < sx + 18 && e.getMouseY() >= sy && e.getMouseY() < sy + 18) {
                ItemStack c = e.getScreen() instanceof AbstractContainerScreen<?> a ? a.getMenu().getCarried()
                        : ItemStack.EMPTY;
                ClientPacketSender.furnaceSlot(i, e.getButton(), c);
                return true;
            }
        }
        return false;
    }

    public void renderTooltip(GuiGraphics g, double mx, double my) {
        if (!visible)
            return;
        for (int i = 0; i < 3; i++) {
            int sx = x + (i == 0 ? layout.inputX : i == 1 ? layout.fuelX : layout.outputX), sy = y + (i == 0 ? layout.inputY : i == 1 ? layout.fuelY : layout.outputY);
            slots[i].renderHighlightAt(g, sx, sy, mx, my);
            slots[i].renderTooltip(g, Minecraft.getInstance(), sx, sy, mx, my);
        }
    }

    public boolean panelInteractiveContains(double mx, double my, int sw, int sh) {
        return visible && mx >= x - 4 && mx < x + layout.panelWidth - 4 && my >= y - 4 && my < y + layout.panelHeight + 4;
    }
}
