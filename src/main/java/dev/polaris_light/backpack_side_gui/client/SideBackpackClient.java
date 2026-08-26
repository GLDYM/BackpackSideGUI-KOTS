package dev.polaris_light.backpack_side_gui.client;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiConfig;
import dev.polaris_light.backpack_side_gui.client.gui.BackpackOverlayScreenPolicy;
import dev.polaris_light.backpack_side_gui.client.gui.OverlayWidget;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SmithingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.UtilityFlagsPayload;
import dev.polaris_light.backpack_side_gui.network.payload.AnvilSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class SideBackpackClient {
    private static final OverlayWidget OVERLAY = new OverlayWidget();
    private static int refreshTicks;
    private static boolean hasBackpack;

    public static void receiveUtilityFlags(UtilityFlagsPayload payload) {
        OVERLAY.setUtilityFlags(new boolean[] { payload.crafting(), payload.furnace(), payload.anvil(),
                payload.smithing(), payload.stonecutter() });
    }

    public static void receiveSmithing(SmithingSyncPayload payload) {
        OVERLAY.receiveSmithing(payload);
    }

    public static void receiveCrafting(CraftingSyncPayload payload) {
        OVERLAY.receiveCrafting(payload);
    }

    public static void receiveAnvil(AnvilSyncPayload payload) { OVERLAY.receiveAnvil(payload); }

    public static boolean shouldBlockContainerInput(
            AbstractContainerScreen<?> screen, double mouseX,
            double mouseY) {
        return hasBackpack && BackpackOverlayScreenPolicy.allows(screen)
                && OVERLAY.panelInteractiveContains(screen, mouseX, mouseY);
    }

    public static boolean shouldBlockContainerTooltip(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        return hasBackpack && BackpackOverlayScreenPolicy.allows(screen)
                && OVERLAY.panelInteractiveContains(screen, mouseX, mouseY);
    }

    private SideBackpackClient() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && ++refreshTicks >= 4) {
            refreshTicks = 0;
            ClientPacketSender.open();
        }
    }

    public static void receive(String name, List<ItemStack> stacks) {
        hasBackpack = name != null && !name.isBlank();
        if (!hasBackpack)
            return;
        OVERLAY.setContents(name, stacks);
    }

    public static void receive(String name, List<ItemStack> stacks, List<Integer> limits) {
        hasBackpack = name != null && !name.isBlank();
        if (hasBackpack) OVERLAY.setContents(name, stacks, limits);
    }

    public static void receiveCarried(ItemStack carried) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen<?> screen)
            screen.getMenu().setCarried(carried == null ? ItemStack.EMPTY : carried.copy());
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (!hasBackpack)
            return;
        RenderSystem.disableDepthTest();
        event.getGuiGraphics().pose().pushPose();
        // Why 330.0F? Ask Mojang.
        event.getGuiGraphics().pose().translate(0.0F, 0.0F, 330.0F);
        try {
            OVERLAY.setAnchorPosition(event.getScreen().width / 2 + BackpackSideGuiConfig.OVERLAY_X.get(),
                    event.getScreen().height / 2 + BackpackSideGuiConfig.OVERLAY_Y.get());
            Minecraft minecraft = Minecraft.getInstance();
            OVERLAY.render(event.getScreen(), event.getGuiGraphics(), minecraft);
            OVERLAY.renderTooltip(event.getGuiGraphics(), minecraft, event.getMouseX(), event.getMouseY());
        } finally {
            event.getGuiGraphics().pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    public static void onMousePressedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (!hasBackpack)
            return;
        boolean panelHit = OVERLAY.panelInteractiveContains(event);
        if (panelHit)
            event.setCanceled(true);
        if (OVERLAY.mousePressed(event))
            event.setCanceled(true);
    }

    public static void onMouseDraggedPre(ScreenEvent.MouseDragged.Pre event) {
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (!hasBackpack)
            return;
        if (OVERLAY.mouseDragged(event))
            event.setCanceled(true);
    }

    public static void onMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (!hasBackpack)
            return;
        if (OVERLAY.mouseReleased(event))
            event.setCanceled(true);
    }

    public static void onMouseScrolledPre(ScreenEvent.MouseScrolled.Pre event) {
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (OVERLAY.mouseScrolled(event))
            event.setCanceled(true);
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (hasBackpack && BackpackOverlayScreenPolicy.allows(event.getScreen())
                && OVERLAY.keyPressed(event.getKeyCode()))
            event.setCanceled(true);
    }

    public static void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (hasBackpack && BackpackOverlayScreenPolicy.allows(event.getScreen())
                && OVERLAY.charTyped(event.getCodePoint()))
            event.setCanceled(true);
    }
}
