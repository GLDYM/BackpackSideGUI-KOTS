package dev.polaris_light.backpack_side_gui.client.gui;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiConfig;
import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayWidget;
import dev.polaris_light.backpack_side_gui.client.gui.area.BackpackOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.MoveOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.VisibilityOverlayButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

public final class OverlayWidget extends IOverlayWidget {

    private static final int BUTTON_SIZE = 14, BUTTON_GAP = 3;
    private final BackpackOverlayArea area = new BackpackOverlayArea();
    private boolean dragging;
    private int dragOffsetX, dragOffsetY;
    private int x, y;

    private final MoveOverlayButton moveButton = new MoveOverlayButton(icon("move"));
    private final VisibilityOverlayButton visibilityButton = new VisibilityOverlayButton(area, icon("show"), icon("hide"));

    @Override
    public void beginDragging(double mx, double my) {
        dragging = true;
        dragOffsetX = (int) mx - x;
        dragOffsetY = (int) my - y;
    }

    public void setContents(String title, List<ItemStack> items) {
        area.setContents(title, items);
    }

    public void setAnchorPosition(int x, int y) {
        if (!dragging) {
            this.x = x;
            this.y = y;
        }
    }

    @Override
    public void render(Screen s, GuiGraphics g, Minecraft mc) {
        moveButton.setBounds(x, y);
        moveButton.render(g, mc);

        visibilityButton.updateState();
        visibilityButton.setBounds(x + BUTTON_SIZE + BUTTON_GAP, y);
        visibilityButton.render(g, mc);

        area.prepareLayout(s.width, s.height);
        area.setOverlayPosition(x, y - area.buttonOffsetY());
        area.render(s, g, mc);
    }

    @Override
    public void renderTooltip(GuiGraphics g, Minecraft mc, double mx, double my) {
        moveButton.renderTooltip(g, mc, mx, my);
        visibilityButton.renderTooltip(g, mc, mx, my);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        if (area.mousePressed(e))
            return true;
        int x = this.x, y = this.y;
        moveButton.setBounds(x, y);
        visibilityButton.setBounds(x + BUTTON_SIZE + BUTTON_GAP, y);
        return moveButton.press(this, e.getMouseX(), e.getMouseY())
                || visibilityButton.press(e.getMouseX(), e.getMouseY());
    }

    public boolean mouseDragged(ScreenEvent.MouseDragged.Pre e) {
        // Move
        if (dragging && e.getMouseButton() == 0) {
            x = (int) e.getMouseX() - dragOffsetX;
            y = (int) e.getMouseY() - dragOffsetY;
            area.setOverlayPosition(x, y - area.buttonOffsetY());
            saveAnchorPosition(e.getScreen());
            return true;
        }
        // Scroller
        return area.mouseDragged(e);
    }

    public boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre e) {
        if (e.getButton() == 0 && dragging) {
            dragging = false;
            return true;
        }
        return area.mouseReleased(e);
    }

    public boolean mouseScrolled(ScreenEvent.MouseScrolled.Pre e) {
        return area.mouseScrolled(e);
    }
    public boolean keyPressed(int key) { return area.keyPressed(key); }
    public boolean charTyped(char c) { return area.charTyped(c); }

    private void saveAnchorPosition(Screen screen) {
        BackpackSideGuiConfig.OVERLAY_X.set(x - screen.width / 2);
        BackpackSideGuiConfig.OVERLAY_Y.set(y - screen.height / 2);
        BackpackSideGuiConfig.CLIENT_SPEC.save();
    }

    private static ResourceLocation icon(String n) {
        return ResourceLocation.fromNamespaceAndPath("backpack_side_gui", "textures/gui/" + n + ".png");
    }
}
