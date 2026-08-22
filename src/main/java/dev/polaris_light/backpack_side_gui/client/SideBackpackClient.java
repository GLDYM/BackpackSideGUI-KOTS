package dev.polaris_light.backpack_side_gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.polaris_light.backpack_side_gui.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class SideBackpackClient {
    private static final BackpackOverlayWidget OVERLAY = new BackpackOverlayWidget();
    private static int refreshTicks;

    private SideBackpackClient() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && ++refreshTicks >= 4) {
            refreshTicks = 0;
            ModNetwork.requestOpen();
        }
    }

    public static void receive(String name, List<ItemStack> stacks, ItemStack carried) {
        OVERLAY.setContents(name, stacks);
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu != null) {
            Minecraft.getInstance().player.containerMenu.setCarried(carried == null ? ItemStack.EMPTY : carried.copy());
        }
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        RenderSystem.disableDepthTest();
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(0.0F, 0.0F, 500.0F);
        try {
            OVERLAY.render(event.getScreen(), event.getGuiGraphics(), Minecraft.getInstance(), event.getMouseX(),
                    event.getMouseY());
        } finally {
            event.getGuiGraphics().pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    public static void onMousePressedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (OVERLAY.mousePressed(event))
            event.setCanceled(true);
    }

    public static void onMouseDraggedPre(ScreenEvent.MouseDragged.Pre event) {
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (OVERLAY.mouseDragged(event))
            event.setCanceled(true);
    }

    public static void onMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
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
}
