package dev.polaris_light.backpack_side_gui.client.gui;

import java.util.List;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiConfig;
import dev.polaris_light.backpack_side_gui.client.gui.api.IOverlayWidget;
import dev.polaris_light.backpack_side_gui.client.gui.area.AnvilOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.area.BackpackOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.area.CraftingOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.area.FurnaceOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.area.SmithingOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.area.StonecutterOverlayArea;
import dev.polaris_light.backpack_side_gui.client.gui.element.MoveOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.UtilityOverlayButton;
import dev.polaris_light.backpack_side_gui.client.gui.element.UtilityType;
import dev.polaris_light.backpack_side_gui.client.gui.element.VisibilityOverlayButton;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import dev.polaris_light.backpack_side_gui.network.payload.AnvilSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.FurnaceSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SmithingSyncPayload;
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
    private int anchorX, anchorY;

    private final MoveOverlayButton moveButton = new MoveOverlayButton(icon("move"));
    private final VisibilityOverlayButton visibilityButton = new VisibilityOverlayButton(area, icon("show"),
            icon("hide"));

    private final UtilityOverlayButton[] utilityButtons = new UtilityOverlayButton[5];
    private final boolean[] utilityFlags = new boolean[5];

    private UtilityType activeUtility;
    private final CraftingOverlayArea crafting = new CraftingOverlayArea();
    private final FurnaceOverlayArea furnace = new FurnaceOverlayArea();
    private final AnvilOverlayArea anvil = new AnvilOverlayArea();
    private final SmithingOverlayArea smithing = new SmithingOverlayArea();
    private final StonecutterOverlayArea stonecutter = new StonecutterOverlayArea();

    {
        UtilityType[] types = UtilityType.values();
        for (int i = 0; i < utilityButtons.length; i++) {
            final UtilityType type = types[i];
            utilityButtons[i] = new UtilityOverlayButton(type, icon(type.icon()), this::onUtilityPressed);
        }
    }

    public void receiveCrafting(CraftingSyncPayload payload) {
        crafting.sync(java.util.Arrays.copyOf(payload.items(), 9), payload.items()[9]);
    }

    public void receiveFurnace(FurnaceSyncPayload payload) {
        furnace.sync(payload.input(), payload.fuel(), payload.output(), payload.burnFinish(), payload.burnTotal(),
                payload.cookFinish(), payload.cookTotal(),
                payload.cooking());
    }

    public void receiveAnvil(AnvilSyncPayload payload) {
        anvil.sync(payload.first(), payload.second(), payload.result(), payload.cost(), payload.name());
    }

    public void receiveSmithing(SmithingSyncPayload payload) {
        smithing.sync(payload.template(), payload.base(), payload.addition(), payload.result());
    }

    public void receiveStonecutter(dev.polaris_light.backpack_side_gui.network.payload.StonecutterSyncPayload payload) {
        stonecutter.sync(payload.input(), payload.output(), payload.recipes(), payload.selected());
    }

    public void setUtilityFlags(boolean[] flags) {
        java.util.Arrays.fill(utilityFlags, false);
        System.arraycopy(flags, 0, utilityFlags, 0, Math.min(flags.length, utilityFlags.length));
    }

    private void onUtilityPressed(UtilityOverlayButton clicked) {
        activeUtility = activeUtility == clicked.utilityType() ? null : clicked.utilityType();
        crafting.setVisible(activeUtility == UtilityType.CRAFTING && area.isVisible() && utilityButtons[0].isVisible());
        furnace.setVisible(activeUtility == UtilityType.FURNACE && area.isVisible() && utilityButtons[1].isVisible());
        anvil.setVisible(activeUtility == UtilityType.ANVIL && area.isVisible() && utilityButtons[2].isVisible());
        smithing.setVisible(activeUtility == UtilityType.SMITHING && area.isVisible() && utilityButtons[3].isVisible());
        stonecutter.setVisible(activeUtility == UtilityType.STONECUTTER && area.isVisible() && utilityButtons[4].isVisible());

        for (UtilityOverlayButton button : utilityButtons)
            button.setTargetVisible(button == clicked && activeUtility != null);
        if (activeUtility != null)
            ClientPacketSender.utility(activeUtility.protocolId());
    }

    @Override
    public void beginDragging(double mouseX, double mouseY) {
        dragging = true;
        dragOffsetX = (int) mouseX - anchorX;
        dragOffsetY = (int) mouseY - anchorY;
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

    public void setAnchorPosition(int anchorX, int anchorY) {
        if (!dragging) {
            this.anchorX = anchorX;
            this.anchorY = anchorY;
        }
    }

    @Override
    public void render(Screen screen, GuiGraphics graphics, Minecraft minecraft) {
        clampAnchor(screen.width, screen.height);

        area.prepareLayout(screen.width, screen.height);
        area.setOverlayPosition(anchorX, anchorY - area.buttonOffsetY());
        area.render(screen, graphics, minecraft);

        crafting.setVisible(activeUtility == UtilityType.CRAFTING && area.isVisible() && utilityButtons[0].isVisible());
        furnace.setVisible(activeUtility == UtilityType.FURNACE && area.isVisible() && utilityButtons[1].isVisible());
        anvil.setVisible(activeUtility == UtilityType.ANVIL && area.isVisible() && utilityButtons[2].isVisible());
        smithing.setVisible(activeUtility == UtilityType.SMITHING && area.isVisible() && utilityButtons[3].isVisible());
        stonecutter.setVisible(activeUtility == UtilityType.STONECUTTER && area.isVisible() && utilityButtons[4].isVisible());

        if (crafting.isVisible()) {
            crafting.setOverlayPosition(area.overlayX(), area.overlayButtonY() + 22);
            crafting.render(screen, graphics, minecraft);
        }
        if (furnace.isVisible()) {
            furnace.setOverlayPosition(area.overlayX(), area.overlayButtonY() + 22);
            furnace.render(screen, graphics, minecraft);
        }
        if (anvil.isVisible()) {
            anvil.setOverlayPosition(area.overlayX(), area.overlayButtonY() + 22);
            anvil.render(screen, graphics, minecraft);
        }
        if (smithing.isVisible()) {
            smithing.setOverlayPosition(area.overlayX(), area.overlayButtonY() + 22);
            smithing.render(screen, graphics, minecraft);
        }
        if (stonecutter.isVisible()) {
            stonecutter.setOverlayPosition(area.overlayX(), area.overlayButtonY() + 22);
            stonecutter.render(screen, graphics, minecraft);
        }

        moveButton.setBounds(anchorX, anchorY);
        moveButton.render(graphics, minecraft);

        visibilityButton.updateState();
        visibilityButton.setBounds(anchorX + BUTTON_SIZE + BUTTON_GAP, anchorY);
        visibilityButton.render(graphics, minecraft);

        int utilityIndex = 0;
        for (int i = 0; i < utilityButtons.length; i++) {
            UtilityOverlayButton button = utilityButtons[i];
            button.setVisible(area.isVisible() && utilityFlags[i]);
            if (utilityFlags[i])
                button.setBounds(anchorX + (BUTTON_SIZE + BUTTON_GAP) * (2 + utilityIndex++), anchorY);
            button.render(graphics, minecraft);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
        area.renderTooltip(graphics, mouseX, mouseY);
        crafting.renderTooltip(graphics, mouseX, mouseY);
        furnace.renderTooltip(graphics, mouseX, mouseY);
        anvil.renderTooltip(graphics, mouseX, mouseY);
        smithing.renderTooltip(graphics, mouseX, mouseY);
        stonecutter.renderTooltip(graphics, mouseX, mouseY);
        moveButton.renderTooltip(graphics, minecraft, mouseX, mouseY);
        visibilityButton.renderTooltip(graphics, minecraft, mouseX, mouseY);
        for (UtilityOverlayButton button : utilityButtons)
            button.renderTooltip(graphics, minecraft, mouseX, mouseY);
    }

    public boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        clampAnchor(event.getScreen().width, event.getScreen().height);
        moveButton.setBounds(this.anchorX, this.anchorY);
        visibilityButton.setBounds(this.anchorX + BUTTON_SIZE + BUTTON_GAP, this.anchorY);
        if (moveButton.press(this, event.getMouseX(), event.getMouseY())
                || visibilityButton.press(event.getMouseX(), event.getMouseY()))
            return true;

        int utilityIndex = 0;
        for (int i = 0; i < utilityButtons.length; i++)
            if (utilityButtons[i].isVisible()) {
                utilityButtons[i].setBounds(this.anchorX + (BUTTON_SIZE + BUTTON_GAP) * (2 + utilityIndex++),
                        this.anchorY);
                if (utilityButtons[i].press(event.getMouseX(), event.getMouseY()))
                    return true;
            }

        if (area.mousePressed(event))
            return true;
        if (activeUtility == UtilityType.CRAFTING
                && crafting.panelInteractiveContains(event.getMouseX(), event.getMouseY(),
                        event.getScreen().width, event.getScreen().height))
            return crafting.mousePressed(event) || true;
        if (activeUtility == UtilityType.FURNACE
                && furnace.panelInteractiveContains(event.getMouseX(), event.getMouseY(),
                        event.getScreen().width, event.getScreen().height))
            return furnace.mousePressed(event) || true;
        if (activeUtility == UtilityType.ANVIL && anvil.panelInteractiveContains(event.getMouseX(), event.getMouseY(),
                event.getScreen().width, event.getScreen().height))
            return anvil.mousePressed(event) || true;
        if (activeUtility == UtilityType.SMITHING && smithing.mousePressed(event))
            return true;
        if (activeUtility == UtilityType.STONECUTTER && stonecutter.panelInteractiveContains(event.getMouseX(),
                event.getMouseY(), event.getScreen().width, event.getScreen().height))
            return stonecutter.mousePressed(event) || true;
        return false;
    }

    public boolean panelInteractiveContains(ScreenEvent.MouseButtonPressed.Pre event) {
        return area.panelInteractiveContains(event.getMouseX(), event.getMouseY(), event.getScreen().width,
                event.getScreen().height)
                || crafting.panelInteractiveContains(event.getMouseX(), event.getMouseY(), event.getScreen().width,
                        event.getScreen().height)
                || furnace.panelInteractiveContains(event.getMouseX(), event.getMouseY(), event.getScreen().width,
                        event.getScreen().height)
                || anvil.panelInteractiveContains(event.getMouseX(), event.getMouseY(), event.getScreen().width,
                        event.getScreen().height)
                || smithing.panelInteractiveContains(event.getMouseX(), event.getMouseY(), event.getScreen().width,
                        event.getScreen().height)
                || stonecutter.panelInteractiveContains(event.getMouseX(), event.getMouseY(), event.getScreen().width,
                        event.getScreen().height);
    }

    public boolean panelInteractiveContains(Screen screen, double mouseX, double mouseY) {
        return area.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height)
                || crafting.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height)
                || furnace.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height)
                || anvil.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height)
                || smithing.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height)
                || stonecutter.panelInteractiveContains(mouseX, mouseY, screen.width, screen.height);
    }

    public boolean mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        // Move
        if (dragging && event.getMouseButton() == 0) {
            anchorX = (int) event.getMouseX() - dragOffsetX;
            anchorY = (int) event.getMouseY() - dragOffsetY;
            clampAnchor(event.getScreen().width, event.getScreen().height);
            area.setOverlayPosition(anchorX, anchorY - area.buttonOffsetY());
            saveAnchorPosition(event.getScreen());
            return true;
        }
        // Scroller
        if (activeUtility == UtilityType.CRAFTING && crafting.mouseDragged(event))
            return true;
        if (activeUtility == UtilityType.STONECUTTER && stonecutter.mouseDragged(event))
            return true;
        return area.mouseDragged(event);
    }

    public boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == 0 && dragging) {
            dragging = false;
            return true;
        }
        if (activeUtility == UtilityType.CRAFTING && crafting.mouseReleased(event))
            return true;
        if (activeUtility == UtilityType.STONECUTTER && stonecutter.mouseReleased(event))
            return true;
        return area.mouseReleased(event);
    }

    public boolean mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (activeUtility == UtilityType.STONECUTTER && stonecutter.mouseScrolled(event))
            return true;
        return area.mouseScrolled(event);
    }

    public boolean keyPressed(int key) {
        return area.keyPressed(key) || anvil.keyPressed(key);
    }

    public boolean charTyped(char character) {
        return area.charTyped(character) || anvil.charTyped(character);
    }

    private void saveAnchorPosition(Screen screen) {
        clampAnchor(screen.width, screen.height);
        BackpackSideGuiConfig.OVERLAY_X.set(anchorX - screen.width / 2);
        BackpackSideGuiConfig.OVERLAY_Y.set(anchorY - screen.height / 2);
        BackpackSideGuiConfig.CLIENT_SPEC.save();
    }

    private void clampAnchor(int screenWidth, int screenHeight) {
        int maxX = Math.max(ANCHOR_MARGIN, screenWidth - MoveOverlayButton.SIZE);
        int maxY = Math.max(ANCHOR_MARGIN, screenHeight - MoveOverlayButton.SIZE);
        anchorX = Math.max(ANCHOR_MARGIN, Math.min(maxX, anchorX));
        anchorY = Math.max(ANCHOR_MARGIN, Math.min(maxY, anchorY));
    }

    private static ResourceLocation icon(String iconName) {
        return ResourceLocation.fromNamespaceAndPath("backpack_side_gui", "textures/gui/" + iconName + ".png");
    }
}
