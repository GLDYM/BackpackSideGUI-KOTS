package dev.polaris_light.backpack_side_gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/** Shared rendering primitives for side backpack panels. */
final class PanelRenderer {
    private static final int COUNT_OFFSET = 6;

    private PanelRenderer() {
    }

    static void renderItemWithLargeCount(GuiGraphics graphics, Minecraft mc, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, x, y);
        if (stack.getCount() != 1) {
            String count = String.valueOf(stack.getCount());
            graphics.pose().pushPose();
            graphics.pose().translate(0.0f, 0.0f, 200.0f);
            int tx = x + 17 - mc.font.width(count);
            graphics.drawString(mc.font, count, tx, y + COUNT_OFFSET + 3, 16777215, true);
            graphics.pose().popPose();
        } else {
            graphics.renderItemDecorations(mc.font, stack, x, y);
        }
    }

    static void renderCarriedStack(GuiGraphics graphics, Minecraft mc, ItemStack stack, double mouseX, double mouseY) {
        if (stack == null || stack.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 900.0f);
        renderItemWithLargeCount(graphics, mc, stack, (int) mouseX - 8, (int) mouseY - 8);
        graphics.pose().popPose();
    }

    static void renderIconButton(GuiGraphics graphics, ResourceLocation icon, int x, int y, boolean selected) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 20.0f);
        graphics.fill(x - 1, y - 1, x + 15, y + 15, selected ? -2047904 : -872415232);
        graphics.fill(x, y, x + 14, y + 14, selected ? -11187676 : -14013910);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        graphics.pose().translate(0.0f, 0.0f, 2.0f);
        graphics.blit(icon, x + 1, y + 1, 0.0f, 0.0f, 12, 12, 12, 12);
        graphics.pose().popPose();
    }

    static void renderScrollbar(GuiGraphics graphics, BackpackPanelLayout.PanelRect rect, int totalRows,
            int maxScroll, int scrollRow) {
        if (totalRows <= rect.visibleRows() || maxScroll <= 0) return;
        int trackX = rect.x() + 165;
        int trackY = rect.y();
        int trackH = rect.visibleRows() * BackpackPanelLayout.SLOT_SIZE;
        int thumbH = Math.max(12, (trackH * rect.visibleRows()) / Math.max(1, totalRows));
        int thumbTravel = Math.max(1, trackH - thumbH);
        int thumbY = trackY + (int) Math.round(thumbTravel * ((double) scrollRow / maxScroll));
        graphics.fill(trackX, trackY, trackX + 6, trackY + trackH, -14671840);
        graphics.fill(trackX + 1, thumbY, trackX + 5, thumbY + thumbH, -6645094);
    }

    static void renderSmallHint(GuiGraphics graphics, Minecraft mc, int x, int y, Component message) {
        graphics.fill(x - 4, y - 18, x + 120, y + 4, -1728053248);
        graphics.drawString(mc.font, message, x, y - 13, 11184810, true);
    }

    static void renderUtilitySlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, -12961222);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, -14671840);
    }

    static void renderFurnaceBars(GuiGraphics graphics, int x, int sy, int litTime, int litDuration,
            int cookProgress, int cookTotal) {
        if (litDuration > 0 && litTime > 0) {
            int fire = Math.max(1, Math.min(13, (litTime * 13) / Math.max(1, litDuration)));
            graphics.fill(x + 45, sy + 31 + (13 - fire), x + 51, sy + 44, -29696);
        }
        if (cookTotal > 0 && cookProgress > 0) {
            int cook = Math.max(1, Math.min(22, (cookProgress * 22) / Math.max(1, cookTotal)));
            graphics.fill(x + 54, sy + 25, x + 54 + cook, sy + 29, -5592406);
        }
    }

    static void renderAnvilNameBox(GuiGraphics graphics, Minecraft mc, int x, int y, boolean focused, String name) {
        graphics.fill(x, y, x + 92, y + 12, focused ? -13421773 : -870309856);
        graphics.fill(x, y, x + 92, y + 1, focused ? -8064 : -8947849);
        String shown = name == null || name.isEmpty() ? "Rename" : name;
        graphics.drawString(mc.font, shown, x + 3, y + 2, name == null || name.isEmpty() ? 7829367 : 16777215, false);
    }
}
