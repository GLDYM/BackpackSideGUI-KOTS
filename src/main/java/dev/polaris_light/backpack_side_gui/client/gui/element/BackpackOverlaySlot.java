package dev.polaris_light.backpack_side_gui.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;

public final class BackpackOverlaySlot extends Slot {
    private final int index;
    private final ItemStack stack;

    public BackpackOverlaySlot(int index, ItemStack stack) {
        super(new SimpleContainer(1), 0, 0, 0);
        this.index = index;
        this.stack = stack == null ? ItemStack.EMPTY : stack;
    }

    public ItemStack stack() {
        return stack;
    }

    public void renderAt(GuiGraphics g, Minecraft mc, int sx, int sy) {
        g.fill(sx, sy, sx + 18, sy + 18, -872415232);
        g.fill(sx + 1, sy + 1, sx + 17, sy + 17, -14671840);
        if (stack.isEmpty()) return;
        g.renderItem(stack, sx + 1, sy + 1);
        if (stack.getCount() <= 1) return;
        String c = CountAbbreviator.abbreviate(stack.getCount());
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F);
        g.drawString(mc.font, c, sx + 20 - mc.font.width(c) - 2, sy + 10, 16777215, true);
        g.pose().popPose();
    }

    public void renderHighlightAt(GuiGraphics g, int sx, int sy, double mouseX, double mouseY) {
        if (mouseX < sx || mouseX >= sx + 18 || mouseY < sy || mouseY >= sy + 18) return;
        int highlight = 0x70FFF04A;
        g.fill(sx, sy, sx + 18, sy + 18, highlight);
        g.fill(sx, sy, sx + 18, sy + 1, 0xD0FFF04A);
        g.fill(sx, sy + 17, sx + 18, sy + 18, 0xD0FFF04A);
        g.fill(sx, sy, sx + 1, sy + 18, 0xD0FFF04A);
        g.fill(sx + 17, sy, sx + 18, sy + 18, 0xD0FFF04A);
    }

    public void render(GuiGraphics g, Minecraft mc, int ox, int oy, int scroll, int visible) {
        int row = index / 9;
        if (row < scroll || row >= scroll + visible)
            return;
        int sx = ox + (index % 9) * 18, sy = oy + (row - scroll) * 18;
        g.fill(sx, sy, sx + 18, sy + 18, -872415232);
        g.fill(sx + 1, sy + 1, sx + 17, sy + 17, -14671840);
        // Do not read through the vanilla SimpleContainer-backed Slot here:
        // vanilla container paths normalize oversized stacks to 64. The sync
        // payload already carries the real Sophisticated count.
        ItemStack shown = stack;
        if (shown.isEmpty())
            return;
        g.renderItem(shown, sx + 1, sy + 1);
        if (shown.getCount() <= 1)
            return;
        String c = CountAbbreviator.abbreviate(shown.getCount());
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F);
        g.drawString(mc.font, c, sx + 20 - mc.font.width(c) - 2, sy + 10, 16777215, true);
        g.pose().popPose();
    }

    public void renderHighlight(GuiGraphics g, int ox, int oy, int scroll, int visible, double mouseX, double mouseY) {
        int row = index / 9;
        if (row < scroll || row >= scroll + visible)
            return;
        int sx = ox + (index % 9) * 18, sy = oy + (row - scroll) * 18;
        if (mouseX < sx || mouseX >= sx + 18 || mouseY < sy || mouseY >= sy + 18)
            return;
        renderHighlightAt(g, sx, sy, mouseX, mouseY);
    }
}
