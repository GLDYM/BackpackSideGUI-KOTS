package dev.polaris_light.backpack_side_gui.client.gui;

import java.util.List;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiConfig;
import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayWidget;
import dev.polaris_light.backpack_side_gui.client.gui.area.BackpackOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.area.CraftingOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.area.AnvilOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.area.SmithingOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.MoveOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.UtilityOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.UtilityType;
import dev.polaris_light.backpack_side_gui.client.gui.element.VisibilityOverlayButton;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SmithingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.AnvilSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class OverlayWidget extends IOverlayWidget {
    private static final int BUTTON_SIZE = 16, BUTTON_GAP = 3;
    // Keep the anchor usable even when the panel is dragged near a screen edge.
    private static final int ANCHOR_MARGIN = 4;

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
    private final CraftingOverlayArea crafting = new CraftingOverlayArea();
    private final SmithingOverlayArea smithing = new SmithingOverlayArea();
    private final AnvilOverlayArea anvil = new AnvilOverlayArea();

    public void receiveSmithing(SmithingSyncPayload p) {
        smithing.sync(p.template(), p.base(), p.addition(), p.result());
    }

    public void receiveCrafting(CraftingSyncPayload p) {
        crafting.sync(java.util.Arrays.copyOf(p.items(), 9), p.items()[9]);
    }

    public void receiveAnvil(AnvilSyncPayload p) { anvil.sync(p.first(), p.second(), p.result(), p.cost(), p.name()); }

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
        crafting.setVisible(activeUtility == UtilityType.CRAFTING && area.isVisible() && utilityButtons[0].isVisible());
        anvil.setVisible(activeUtility == UtilityType.ANVIL && area.isVisible() && utilityButtons[2].isVisible());
        for (UtilityOverlayButton button : utilityButtons)
            button.setTargetVisible(button == clicked && activeUtility != null);
        if (activeUtility != null)
            ClientPacketSender.utility(activeUtility.protocolId());
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

    public void setContents(String title, List<ItemStack> items, int stackLimit) {
        area.setContents(title, items, stackLimit);
    }
    public void setContents(String title, List<ItemStack> items, List<Integer> limits) {
        area.setContents(title, items, limits);
    }

    public void setAnchorPosition(int x, int y) {
        if (!dragging) {
            this.x = x;
            this.y = y;
        }
    }

    @Override
    public void render(Screen s, GuiGraphics g, Minecraft mc) {
        clampAnchor(s.width, s.height);

        area.prepareLayout(s.width, s.height);
        area.setOverlayPosition(x, y - area.buttonOffsetY());
        area.render(s, g, mc);

        smithing.setVisible(activeUtility == UtilityType.SMITHING && area.isVisible() && utilityButtons[3].isVisible());
        crafting.setVisible(activeUtility == UtilityType.CRAFTING && area.isVisible() && utilityButtons[0].isVisible());
        anvil.setVisible(activeUtility == UtilityType.ANVIL && area.isVisible() && utilityButtons[2].isVisible());
        if (smithing.isVisible()) {
            smithing.setOverlayPosition(area.overlayX(), area.overlayButtonY() + 22);
            smithing.render(s, g, mc);
        }
        if (crafting.isVisible()) {
            crafting.setOverlayPosition(area.overlayX(), area.overlayButtonY() + 22);
            crafting.render(s, g, mc);
        }
        if (anvil.isVisible()) {
            anvil.setOverlayPosition(area.overlayX(), area.overlayButtonY() + 22);
            anvil.render(s, g, mc);
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
        crafting.renderTooltip(g, mx, my);
        anvil.renderTooltip(g, mx, my);
        moveButton.renderTooltip(g, mc, mx, my);
        visibilityButton.renderTooltip(g, mc, mx, my);
        for (UtilityOverlayButton button : utilityButtons)
            button.renderTooltip(g, mc, mx, my);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        // Buttons own their pixels even when a panel extends behind them.
        clampAnchor(e.getScreen().width, e.getScreen().height);
        moveButton.setBounds(this.x, this.y);
        visibilityButton.setBounds(this.x + BUTTON_SIZE + BUTTON_GAP, this.y);
        if (moveButton.press(this, e.getMouseX(), e.getMouseY())
                || visibilityButton.press(e.getMouseX(), e.getMouseY()))
            return true;

        int utilityIndex = 0;
        for (int i = 0; i < utilityButtons.length; i++)
            if (utilityButtons[i].isVisible()) {
                utilityButtons[i].setBounds(this.x + (BUTTON_SIZE + BUTTON_GAP) * (2 + utilityIndex++), this.y);
                if (utilityButtons[i].press(e.getMouseX(), e.getMouseY()))
                    return true;
            }

        if (area.mousePressed(e))
            return true;
        if (activeUtility == UtilityType.SMITHING && smithing.mousePressed(e))
            return true;
        if (activeUtility == UtilityType.CRAFTING && crafting.panelInteractiveContains(e.getMouseX(), e.getMouseY(),
                e.getScreen().width, e.getScreen().height))
            return crafting.mousePressed(e) || true;
        if (activeUtility == UtilityType.ANVIL && anvil.panelInteractiveContains(e.getMouseX(), e.getMouseY(),
                e.getScreen().width, e.getScreen().height))
            return anvil.mousePressed(e) || true;
        return false;
    }

    public boolean panelInteractiveContains(ScreenEvent.MouseButtonPressed.Pre e) {
        return area.panelInteractiveContains(e.getMouseX(), e.getMouseY(), e.getScreen().width, e.getScreen().height)
                || smithing.panelInteractiveContains(e.getMouseX(), e.getMouseY(), e.getScreen().width,
                        e.getScreen().height)
                || crafting.panelInteractiveContains(e.getMouseX(), e.getMouseY(), e.getScreen().width,
                        e.getScreen().height)
                || anvil.panelInteractiveContains(e.getMouseX(), e.getMouseY(), e.getScreen().width,
                        e.getScreen().height);
    }

    public boolean panelInteractiveContains(Screen screen, double mouseX, double mouseY) {
        return area.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height)
                || smithing.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height)
                || crafting.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height)
                || anvil.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height);
    }

    public boolean mouseDragged(ScreenEvent.MouseDragged.Pre e) {
        // Move
        if (dragging && e.getMouseButton() == 0) {
            x = (int) e.getMouseX() - dragOffsetX;
            y = (int) e.getMouseY() - dragOffsetY;
            clampAnchor(e.getScreen().width, e.getScreen().height);
            area.setOverlayPosition(x, y - area.buttonOffsetY());
            saveAnchorPosition(e.getScreen());
            return true;
        }
        // Scroller
        if (activeUtility == UtilityType.CRAFTING && crafting.mouseDragged(e))
            return true;
        return area.mouseDragged(e);
    }

    public boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre e) {
        if (e.getButton() == 0 && dragging) {
            dragging = false;
            return true;
        }
        if (activeUtility == UtilityType.CRAFTING && crafting.mouseReleased(e))
            return true;
        return area.mouseReleased(e);
    }

    public boolean mouseScrolled(ScreenEvent.MouseScrolled.Pre e) {
        return area.mouseScrolled(e);
    }

    public boolean keyPressed(int key) {
        return area.keyPressed(key) || anvil.keyPressed(key);
    }

    public boolean charTyped(char c) {
        return area.charTyped(c) || anvil.charTyped(c);
    }

    private void saveAnchorPosition(Screen screen) {
        clampAnchor(screen.width, screen.height);
        BackpackSideGuiConfig.OVERLAY_X.set(x - screen.width / 2);
        BackpackSideGuiConfig.OVERLAY_Y.set(y - screen.height / 2);
        BackpackSideGuiConfig.CLIENT_SPEC.save();
    }

    private void clampAnchor(int screenWidth, int screenHeight) {
        int maxX = Math.max(ANCHOR_MARGIN, screenWidth - MoveOverlayButton.SIZE);
        int maxY = Math.max(ANCHOR_MARGIN, screenHeight - MoveOverlayButton.SIZE);
        x = Math.max(ANCHOR_MARGIN, Math.min(maxX, x));
        y = Math.max(ANCHOR_MARGIN, Math.min(maxY, y));
    }

    private static ResourceLocation icon(String n) {
        return ResourceLocation.fromNamespaceAndPath("backpack_side_gui", "textures/gui/" + n + ".png");
    }
}
