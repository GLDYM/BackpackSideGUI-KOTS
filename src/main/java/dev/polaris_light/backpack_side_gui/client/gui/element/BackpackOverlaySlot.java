package dev.polaris_light.backpack_side_gui.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;

public final class BackpackOverlaySlot extends Slot {
    private final int index;
    private final ItemStack stack;
    private final int stackLimit;

    public BackpackOverlaySlot(int index, ItemStack stack) {
        super(new SimpleContainer(1), 0, 0, 0);
        this.index = index;
        this.stack = stack == null ? ItemStack.EMPTY : stack;
        this.stackLimit = 64;
    }

    public BackpackOverlaySlot(int index, ItemStack stack, int stackLimit) {
        super(new SimpleContainer(1), 0, 0, 0);
        this.index = index;
        this.stack = stack == null ? ItemStack.EMPTY : stack;
        this.stackLimit = Math.max(1, stackLimit);
    }

    @Override
    public int getMaxStackSize() {
        return stackLimit;
    }

    public ItemStack stack() {
        return stack;
    }

    public void renderAt(GuiGraphics graphics, Minecraft minecraft, int screenX, int screenY) {
        graphics.fill(screenX, screenY, screenX + 18, screenY + 18, -872415232);
        graphics.fill(screenX + 1, screenY + 1, screenX + 17, screenY + 17, -14671840);
        if (stack.isEmpty())
            return;
        graphics.renderItem(stack, screenX + 1, screenY + 1);
        if (stack.getCount() <= 1)
            return;
        String c = CountAbbreviator.abbreviate(stack.getCount());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.drawString(minecraft.font, c, screenX + 20 - minecraft.font.width(c) - 2, screenY + 10, 16777215,
                true);
        graphics.pose().popPose();
    }

    public void renderPreview(GuiGraphics graphics, Minecraft minecraft, int sx, int sy, ItemStack preview) {
        if (preview == null || preview.isEmpty())
            return;
        graphics.renderItem(preview, sx + 1, sy + 1);
        if (preview.getCount() > 1) {
            String c = CountAbbreviator.abbreviate(preview.getCount());
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 300.0F);
            graphics.drawString(minecraft.font, c, sx + 20 - minecraft.font.width(c) - 2, sy + 10, 16777215, true);
            graphics.pose().popPose();
        }
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
            java.util.List<Component> lines = new java.util.ArrayList<>(stack.getTooltipLines(
                    minecraft.level == null ? net.minecraft.world.item.Item.TooltipContext.EMPTY
                            : net.minecraft.world.item.Item.TooltipContext.of(minecraft.level),
                    minecraft.player,
                    net.minecraft.world.item.TooltipFlag.NORMAL));
            // Stupid Codex!
            if (stack.getCount() > stack.getMaxStackSize() || stackLimit > stack.getMaxStackSize()) {
                net.minecraft.network.chat.MutableComponent count = Component
                        .literal(java.text.NumberFormat.getNumberInstance().format(stack.getCount()))
                        .withStyle(net.minecraft.ChatFormatting.DARK_AQUA);
                Component max = Component.literal(java.text.NumberFormat.getNumberInstance().format(stackLimit))
                        .withStyle(net.minecraft.ChatFormatting.DARK_AQUA);
                Component value = count.append(Component.literal(" /").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(max);
                lines.add(Component.translatable("gui.sophisticatedcore.tooltip.stack_count", value)
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            java.util.List<net.minecraft.util.FormattedCharSequence> wrapped = new java.util.ArrayList<>();
            for (Component line : lines)
                wrapped.addAll(minecraft.font.split(line, 300));
            graphics.renderTooltip(minecraft.font, wrapped, (int) mouseX, (int) mouseY);
        }
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
        graphics.renderItem(shown, sx + 1, sy + 1);
        if (shown.getCount() <= 1)
            return;
        String c = CountAbbreviator.abbreviate(shown.getCount());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.drawString(minecraft.font, c, sx + 20 - minecraft.font.width(c) - 2, sy + 10, 16777215, true);
        graphics.pose().popPose();
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
