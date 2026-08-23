package dev.polaris_light.backpack_side_gui.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;

public final class BackpackOverlaySlot {
    private final int index;
    private final ItemStack stack;
    public BackpackOverlaySlot(int index, ItemStack stack) { this.index = index; this.stack = stack == null ? ItemStack.EMPTY : stack; }

    public void render(GuiGraphics g, Minecraft mc, int ox, int oy, int scroll, int visible) {
        int row = index / 9; if (row < scroll || row >= scroll + visible) return;
        int sx = ox + (index % 9) * 18, sy = oy + (row - scroll) * 18;
        g.fill(sx, sy, sx + 18, sy + 18, -12961222); g.fill(sx + 1, sy + 1, sx + 17, sy + 17, -14671840);
        if (stack.isEmpty()) return; g.renderItem(stack, sx + 1, sy + 1);
        if (stack.getCount() <= 99) { g.renderItemDecorations(mc.font, stack, sx + 1, sy + 1); return; }
        String c = CountAbbreviator.abbreviate(stack.getCount()); g.drawString(mc.font, c, sx + 20 - mc.font.width(c) - 2, sy + 10, 16777215, true);
    }
}
