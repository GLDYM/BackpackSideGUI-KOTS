package dev.polaris_light.backpack_side_gui.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The view model of the panel.  All interactive controls are represented by
 * objects here; the client no longer has to duplicate their rectangles in
 * every mouse event handler.
 */
final class PanelWidgetLayer {
    private static final ResourceLocation MOVE_ICON = icon("move");
    private static final ResourceLocation SHOW_ICON = icon("show");
    private static final ResourceLocation HIDE_ICON = icon("hide");
    private static final ResourceLocation SEARCH_ICON = icon("search");
    private static final ResourceLocation SORT_ICON = icon("sort");
    private static final ResourceLocation[] UTILITY_ICONS = {
            icon("utility_crafting"), icon("utility_furnace"), icon("utility_anvil"), icon("utility_smithing")
    };
    private final List<PanelWidget> widgets = new ArrayList<>();

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath("backpack_side_gui", "textures/gui/" + name + ".png");
    }

    void rebuild(BackpackPanelLayout.PanelRect rect, boolean hidden, int[] utilityTypes, int activeUtility) {
        widgets.clear();
        int buttonY = rect.y() + (rect.visibleRows() * BackpackPanelLayout.SLOT_SIZE) + 5;
        widgets.add(new PanelWidget.Icon("move", MOVE_ICON, rect.x(), buttonY, false));
        widgets.add(new PanelWidget.Icon("toggle", hidden ? SHOW_ICON : HIDE_ICON, rect.x() + 17, buttonY, false));
        if (!hidden) {
            for (int index = 0; index < utilityTypes.length; index++) {
                int type = utilityTypes[index];
                widgets.add(new PanelWidget.Icon("utility:" + type, UTILITY_ICONS[type],
                        rect.x() + (17 * (2 + index)), buttonY, activeUtility == type));
            }
        }

        int topY = rect.y() - 16;
        int categoryX = rect.x() + 162 - 14;
        int sortX = categoryX - 16;
        int searchX = sortX - 16;
        widgets.add(new PanelWidget.Icon("search", SEARCH_ICON, searchX, topY, false));
        widgets.add(new PanelWidget.Icon("sort", SORT_ICON, sortX, topY, false));
        widgets.add(new PanelWidget("category", categoryX, topY, 14, 14) { });
    }

    static ResourceLocation utilityIcon(int type) {
        return type >= 0 && type < UTILITY_ICONS.length ? UTILITY_ICONS[type] : UTILITY_ICONS[0];
    }

    void add(PanelWidget widget) {
        widgets.add(widget);
    }

    void render(GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
        for (PanelWidget widget : widgets) {
            widget.render(graphics, minecraft, mouseX, mouseY);
        }
    }

    void renderGroup(String prefix, GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
        for (PanelWidget widget : widgets) {
            if (widget.id().equals(prefix) || widget.id().startsWith(prefix)) {
                widget.render(graphics, minecraft, mouseX, mouseY);
            }
        }
    }

    PanelWidget find(double mouseX, double mouseY) {
        for (PanelWidget widget : widgets) {
            if (widget.contains(mouseX, mouseY)) return widget;
        }
        return null;
    }

    boolean contains(String id, double mouseX, double mouseY) {
        for (PanelWidget widget : widgets) {
            if (widget.id().equals(id) && widget.contains(mouseX, mouseY)) return true;
        }
        return false;
    }

    int topButton(double mouseX, double mouseY) {
        PanelWidget widget = find(mouseX, mouseY);
        if (widget == null) return -1;
        return switch (widget.id()) {
            case "search" -> 0;
            case "sort" -> 1;
            case "category" -> 2;
            default -> -1;
        };
    }

    int utilityButton(double mouseX, double mouseY) {
        PanelWidget widget = find(mouseX, mouseY);
        if (widget == null || !widget.id().startsWith("utility:")) return -1;
        return Integer.parseInt(widget.id().substring("utility:".length()));
    }
}
