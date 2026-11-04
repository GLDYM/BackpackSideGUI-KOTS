package dev.polaris_light.backpack_side_gui.client.gui.element;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;

public final class BackpackOverlaySlot extends Slot {
    private static final String INFINITY = "\u221E";
    private static final String STACK_COUNT_KEY = "gui.sophisticatedcore.tooltip.stack_count";
    private static final int TOOLTIP_WIDTH = 300;

    /*
     * Z layering inside the panel. The overlay pass itself is translated to
     * z=330 and GuiGraphics#renderItem adds another 150, while vanilla puts the
     * container's slot items at 250 (100 + 150) and the stack it draws for the
     * cursor at 382 (232 + 150). Panel items therefore have to be pulled down by
     * 130 so they still cover the container's items but stay underneath the
     * cursor stack; the count text sits just above them and under the cursor.
     */
    private static final float ITEM_Z = -130.0F;
    private static final float COUNT_Z = 30.0F;

    private final int index;
    private final ItemStack stack;
    private final int stackLimit;
    private final boolean infinite;

    public BackpackOverlaySlot(int index, ItemStack stack) {
        this(index, stack, 64);
    }

    public BackpackOverlaySlot(int index, ItemStack stack, int stackLimit) {
        this(index, stack, stackLimit, false);
    }

    public BackpackOverlaySlot(int index, ItemStack stack, int stackLimit, boolean infinite) {
        super(new SimpleContainer(1), 0, 0, 0);
        this.index = index;
        this.stack = stack == null ? ItemStack.EMPTY : stack;
        this.stackLimit = Math.max(1, stackLimit);
        this.infinite = infinite;
    }

    @Override
    public int getMaxStackSize() {
        return stackLimit;
    }

    public ItemStack stack() {
        return stack;
    }

    public boolean isInfinite() {
        return infinite;
    }

    public void renderAt(GuiGraphics graphics, Minecraft minecraft, int screenX, int screenY) {
        graphics.fill(screenX, screenY, screenX + 18, screenY + 18, -872415232);
        graphics.fill(screenX + 1, screenY + 1, screenX + 17, screenY + 17, -14671840);
        if (stack.isEmpty())
            return;
        renderItemIcon(graphics, stack, screenX + 1, screenY + 1);
        renderCount(graphics, minecraft, screenX, screenY, stack);
    }

    private void renderItemIcon(GuiGraphics graphics, ItemStack shown, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, ITEM_Z);
        graphics.renderItem(shown, x, y);
        graphics.pose().popPose();
    }

    public void renderPreview(GuiGraphics graphics, Minecraft minecraft, int sx, int sy, ItemStack preview) {
        if (preview == null || preview.isEmpty())
            return;
        renderItemIcon(graphics, preview, sx + 1, sy + 1);
        renderCount(graphics, minecraft, sx, sy, preview);
    }

    private void renderCount(GuiGraphics graphics, Minecraft minecraft, int sx, int sy, ItemStack shown) {
        String text = countText(shown);
        if (text == null)
            return;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, COUNT_Z);
        graphics.drawString(minecraft.font, text, sx + 20 - minecraft.font.width(text) - 2, sy + 10, 16777215,
                true);
        graphics.pose().popPose();
    }

    private String countText(ItemStack shown) {
        if (infinite)
            return INFINITY;
        return shown.getCount() > 1 ? CountAbbreviator.abbreviate(shown.getCount()) : null;
    }

    public void renderHighlightAt(GuiGraphics graphics, int sx, int sy, double mouseX, double mouseY) {
        if (mouseX < sx || mouseX >= sx + 18 || mouseY < sy || mouseY >= sy + 18)
            return;
        int highlight = 0x70FFF04A;
        graphics.fill(sx, sy, sx + 18, sy + 18, highlight);
        graphics.fill(sx, sy, sx + 18, sy + 1, 0xD0FFF04A);
        graphics.fill(sx, sy + 17, sx + 18, sy + 18, 0xD0FFF04A);
        graphics.fill(sx, sy, sx + 1, sy + 18, 0xD0FFF04A);
        graphics.fill(sx + 17, sy, sx + 18, sy + 18, 0xD0FFF04A);
    }

    public void renderDragHighlight(GuiGraphics graphics, int sx, int sy) {
        int color = 0xD0FFFF40;
        graphics.fill(sx, sy, sx + 18, sy + 1, color);
        graphics.fill(sx, sy + 17, sx + 18, sy + 18, color);
        graphics.fill(sx, sy, sx + 1, sy + 18, color);
        graphics.fill(sx + 17, sy, sx + 18, sy + 18, color);
    }

    public void renderTooltip(GuiGraphics graphics, Minecraft minecraft, int sx, int sy, double mouseX, double mouseY) {
        if (!stack.isEmpty() && mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
            List<Component> lines = new ArrayList<>(stack.getTooltipLines(
                    minecraft.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(minecraft.level),
                    minecraft.player,
                    TooltipFlag.NORMAL));
            Component value = stackCountValue();
            if (value != null)
                lines.add(Component.translatable(STACK_COUNT_KEY, value).withStyle(ChatFormatting.GRAY));
            List<FormattedCharSequence> wrapped = new ArrayList<>();
            for (Component line : lines)
                wrapped.addAll(minecraft.font.split(line, TOOLTIP_WIDTH));
            graphics.renderTooltip(minecraft.font, wrapped, (int) mouseX, (int) mouseY);
        }
    }

    private Component stackCountValue() {
        if (infinite)
            return Component.literal(INFINITY).withStyle(ChatFormatting.DARK_AQUA);
        if (stack.getCount() <= stack.getMaxStackSize() && stackLimit <= stack.getMaxStackSize())
            return null;
        MutableComponent count = Component.literal(NumberFormat.getNumberInstance().format(stack.getCount()))
                .withStyle(ChatFormatting.DARK_AQUA);
        Component max = Component.literal(NumberFormat.getNumberInstance().format(stackLimit))
                .withStyle(ChatFormatting.DARK_AQUA);
        return count.append(Component.literal(" /").withStyle(ChatFormatting.GRAY)).append(max);
    }

    public void render(GuiGraphics graphics, Minecraft minecraft, int ox, int oy, int scroll, int visible) {
        int row = index / 9;
        if (row < scroll || row >= scroll + visible)
            return;
        int sx = ox + (index % 9) * 18, sy = oy + (row - scroll) * 18;
        graphics.fill(sx, sy, sx + 18, sy + 18, -872415232);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, -14671840);
        // Do not read through the vanilla SimpleContainer-backed Slot here:
        // vanilla container paths normalize oversized stacks to 64. The sync
        // payload already carries the real Sophisticated count.
        ItemStack shown = stack;
        if (shown.isEmpty())
            return;
        renderItemIcon(graphics, shown, sx + 1, sy + 1);
        renderCount(graphics, minecraft, sx, sy, shown);
    }

    public void renderHighlight(GuiGraphics graphics, int ox, int oy, int scroll, int visible, double mouseX,
            double mouseY) {
        int row = index / 9;
        if (row < scroll || row >= scroll + visible)
            return;
        int sx = ox + (index % 9) * 18, sy = oy + (row - scroll) * 18;
        if (mouseX < sx || mouseX >= sx + 18 || mouseY < sy || mouseY >= sy + 18)
            return;
        renderHighlightAt(graphics, sx, sy, mouseX, mouseY);
    }

    public void renderTooltip(GuiGraphics graphics, Minecraft minecraft, int ox, int oy, int scroll, int visible,
            double mouseX, double mouseY) {
        int row = index / 9;
        if (row < scroll || row >= scroll + visible)
            return;
        renderTooltip(graphics, minecraft, ox + (index % 9) * 18, oy + (row - scroll) * 18, mouseX, mouseY);
    }
}
