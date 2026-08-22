package dev.polaris_light.backpack_side_gui.client;

import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Complete object representation of one utility mini-screen. */
final class UtilityPanelWidget extends PanelWidget {
    private final int type, panelX, panelY;
    private final Minecraft minecraft;
    private final java.util.List<ItemStack> items;
    private final Map<Integer, ItemStack> preview;
    private final boolean dragging;
    private final int dragType;
    private final Set<Integer> dragSlots;
    private final int litTime, litDuration, cookProgress, cookTotal, anvilCost;
    private final String anvilName;
    private final boolean anvilFocused;

    UtilityPanelWidget(int type, int x, int y, Minecraft minecraft, java.util.List<ItemStack> items,
            Map<Integer, ItemStack> preview, boolean dragging, int dragType, Set<Integer> dragSlots,
            int litTime, int litDuration, int cookProgress, int cookTotal, int anvilCost,
            String anvilName, boolean anvilFocused) {
        super("utility-panel:" + type, x - 4, y - 4, 170, 74);
        this.type = type;
        this.panelX = x;
        this.panelY = y;
        this.minecraft = minecraft;
        this.items = items;
        this.preview = preview;
        this.dragging = dragging;
        this.dragType = dragType;
        this.dragSlots = dragSlots;
        this.litTime = litTime;
        this.litDuration = litDuration;
        this.cookProgress = cookProgress;
        this.cookTotal = cookTotal;
        this.anvilCost = anvilCost;
        this.anvilName = anvilName;
        this.anvilFocused = anvilFocused;
    }

    @Override
    void render(GuiGraphics g, Minecraft ignored, double mx, double my) {
        g.fill(panelX - 4, panelY - 4, panelX + 166, panelY + 70, -871362544);
        g.fill(panelX - 4, panelY - 4, panelX + 166, panelY - 3, -11184811);
        g.drawString(minecraft.font, Component.translatable(titleKey()), panelX + 4, panelY + 3, 16777215, true);
        int sy = panelY + 17;
        switch (type) {
            case 0 -> crafting(g, mx, my, sy);
            case 1 -> furnace(g, mx, my, sy);
            case 2 -> anvil(g, mx, my, sy);
            case 3 -> smithing(g, mx, my, sy);
            default -> {
            }
        }
    }

    private String titleKey() {
        return switch (type) {
            case 0 -> "text.backpack_side_gui.utility.crafting";
            case 1 -> "text.backpack_side_gui.utility.furnace";
            case 2 -> "text.backpack_side_gui.utility.anvil";
            case 3 -> "text.backpack_side_gui.utility.smithing";
            default -> "";
        };
    }

    private void crafting(GuiGraphics g, double mx, double my, int sy) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                slot(g, panelX + 8 + col * 18, sy + row * 18, row * 3 + col, mx, my);
        g.drawString(minecraft.font, "?", panelX + 70, sy + 20, 16777215, true);
        slot(g, panelX + 93, sy + 18, 9, mx, my);
    }

    private void furnace(GuiGraphics g, double mx, double my, int sy) {
        slot(g, panelX + 22, sy + 2, 0, mx, my);
        slot(g, panelX + 22, sy + 30, 1, mx, my);
        PanelRenderer.renderFurnaceBars(g, panelX, sy, litTime, litDuration, cookProgress, cookTotal);
        g.drawString(minecraft.font, "?", panelX + 58, sy + 18, 16777215, true);
        slot(g, panelX + 88, sy + 16, 2, mx, my);
    }

    private void anvil(GuiGraphics g, double mx, double my, int sy) {
        PanelRenderer.renderAnvilNameBox(g, minecraft, panelX + 6, panelY + 15, anvilFocused, anvilName);
        slot(g, panelX + 15, sy + 20, 0, mx, my);
        g.drawString(minecraft.font, "+", panelX + 40, sy + 25, 16777215, true);
        slot(g, panelX + 55, sy + 20, 1, mx, my);
        g.drawString(minecraft.font, "=", panelX + 82, sy + 25, 16777215, true);
        slot(g, panelX + 102, sy + 20, 2, mx, my);
        if (anvilCost > 0)
            g.drawString(minecraft.font, Integer.toString(anvilCost), panelX + 80, sy + 42, 8454016, true);
    }

    private void smithing(GuiGraphics g, double mx, double my, int sy) {
        slot(g, panelX + 7, sy + 16, 0, mx, my);
        slot(g, panelX + 31, sy + 16, 1, mx, my);
        slot(g, panelX + 55, sy + 16, 2, mx, my);
        g.drawString(minecraft.font, "?", panelX + 82, sy + 21, 16777215, true);
        slot(g, panelX + 106, sy + 16, 3, mx, my);
    }

    private void slot(GuiGraphics g, int x, int y, int index, double mx, double my) {
        ItemStack p = preview.get(index);
        ItemStack stack = p != null && !p.isEmpty() ? p : index < items.size() ? items.get(index) : ItemStack.EMPTY;
        boolean selected = dragging && dragType == type && dragSlots.contains(index);
        new PanelWidget.Slot(minecraft, x, y, stack, contains(mx, my, x, y, 18, 18), selected).render(g, minecraft, mx,
                my);
    }

    private static boolean contains(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
