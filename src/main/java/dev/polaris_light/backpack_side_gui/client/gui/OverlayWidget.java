package dev.polaris_light.backpack_side_gui.client.gui;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiConfig;
import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayWidget;
import dev.polaris_light.backpack_side_gui.client.gui.area.BackpackOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.MoveOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.VisibilityOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.UtilityOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.UtilityType;
import dev.polaris_light.backpack_side_gui.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;
import dev.polaris_light.backpack_side_gui.network.SmithingSyncPayload;

public final class OverlayWidget extends IOverlayWidget {
    private static final int BUTTON_SIZE = 16, BUTTON_GAP = 3;

    private final BackpackOverlayArea area = new BackpackOverlayArea();
    private boolean dragging;
    private int dragOffsetX, dragOffsetY;
    private int x, y;

    private final MoveOverlayButton moveButton = new MoveOverlayButton(icon("move"));
    private final VisibilityOverlayButton visibilityButton = new VisibilityOverlayButton(area, icon("show"),
            icon("hide"));

    private final UtilityOverlayButton[] utilityButtons = new UtilityOverlayButton[5];
    private final boolean[] utilityFlags = new boolean[5];

    private UtilityType activeUtility;
    private final dev.polaris_light.backpack_side_gui.client.gui.area.SmithingOverlayArea smithing = new dev.polaris_light.backpack_side_gui.client.gui.area.SmithingOverlayArea();

    public void receiveSmithing(SmithingSyncPayload p) {
        smithing.sync(p.template(), p.base(), p.addition(), p.result());
    }

    public void setUtilityFlags(boolean[] flags) {
        java.util.Arrays.fill(utilityFlags, false);
        System.arraycopy(flags, 0, utilityFlags, 0, Math.min(flags.length, utilityFlags.length));
    }

    {
        UtilityType[] types = UtilityType.values();
        for (int i = 0; i < utilityButtons.length; i++) {
            final UtilityType type = types[i];
            utilityButtons[i] = new UtilityOverlayButton(type, icon(type.icon()), this::onUtilityPressed);
        }
    }

    private void onUtilityPressed(UtilityOverlayButton clicked) {
        activeUtility = activeUtility == clicked.utilityType() ? null : clicked.utilityType();
        smithing.setVisible(activeUtility == UtilityType.SMITHING && area.isVisible() && utilityButtons[3].isVisible());
        for (UtilityOverlayButton button : utilityButtons)
            button.setTargetVisible(button == clicked && activeUtility != null);
        if (activeUtility != null)
            ModNetwork.requestUtility(activeUtility.protocolId());
    }

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

        area.prepareLayout(s.width, s.height);
        area.setOverlayPosition(x, y - area.buttonOffsetY());
        area.render(s, g, mc);

        smithing.setVisible(activeUtility == UtilityType.SMITHING && area.isVisible() && utilityButtons[3].isVisible());
        if (smithing.isVisible()) {
            smithing.setOverlayPosition(area.overlayX(), area.overlayButtonY() + 22);
            smithing.render(s, g, mc);
        }

        moveButton.setBounds(x, y);
        moveButton.render(g, mc);

        visibilityButton.updateState();
        visibilityButton.setBounds(x + BUTTON_SIZE + BUTTON_GAP, y);
        visibilityButton.render(g, mc);


        int utilityIndex = 0;
        for (int i = 0; i < utilityButtons.length; i++) {
            UtilityOverlayButton b = utilityButtons[i];
            b.setVisible(area.isVisible() && utilityFlags[i]);
            if (utilityFlags[i])
                b.setBounds(x + (BUTTON_SIZE + BUTTON_GAP) * (2 + utilityIndex++), y);
            b.render(g, mc);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics g, Minecraft mc, double mx, double my) {
        area.renderTooltip(g, mx, my);
        smithing.renderTooltip(g, mx, my);
        moveButton.renderTooltip(g, mc, mx, my);
        visibilityButton.renderTooltip(g, mc, mx, my);
        for (UtilityOverlayButton button : utilityButtons)
            button.renderTooltip(g, mc, mx, my);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        if (area.mousePressed(e))
            return true;
        if (activeUtility == UtilityType.SMITHING && smithing.mousePressed(e))
            return true;
        int x = this.x, y = this.y;
        moveButton.setBounds(x, y);
        visibilityButton.setBounds(x + BUTTON_SIZE + BUTTON_GAP, y);
        if (moveButton.press(this, e.getMouseX(), e.getMouseY())
                || visibilityButton.press(e.getMouseX(), e.getMouseY()))
            return true;
        for (int i = 0; i < utilityButtons.length; i++)
            if (utilityButtons[i].isVisible() && utilityButtons[i].press(e.getMouseX(), e.getMouseY()))
                return true;
        return false;
    }

    public boolean panelInteractiveContains(ScreenEvent.MouseButtonPressed.Pre e) {
        return area.panelInteractiveContains(e.getMouseX(), e.getMouseY(), e.getScreen().width, e.getScreen().height)
                || smithing.panelInteractiveContains(e.getMouseX(), e.getMouseY(), e.getScreen().width, e.getScreen().height);
    }

    public boolean panelInteractiveContains(Screen screen, double mouseX, double mouseY) {
        return area.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height)
                || smithing.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height);
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

    public boolean keyPressed(int key) {
        return area.keyPressed(key);
    }

    public boolean charTyped(char c) {
        return area.charTyped(c);
    }

    private void saveAnchorPosition(Screen screen) {
        BackpackSideGuiConfig.OVERLAY_X.set(x - screen.width / 2);
        BackpackSideGuiConfig.OVERLAY_Y.set(y - screen.height / 2);
        BackpackSideGuiConfig.CLIENT_SPEC.save();
    }

    private static ResourceLocation icon(String n) {
        return ResourceLocation.fromNamespaceAndPath("backpack_side_gui", "textures/gui/" + n + ".png");
    }
}
