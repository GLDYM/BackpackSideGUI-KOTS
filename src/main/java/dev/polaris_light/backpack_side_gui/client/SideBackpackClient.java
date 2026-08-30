package dev.polaris_light.backpack_side_gui.client;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiConfig;
import dev.polaris_light.backpack_side_gui.client.gui.BackpackOverlayScreenPolicy;
import dev.polaris_light.backpack_side_gui.client.gui.OverlayWidget;
import dev.polaris_light.backpack_side_gui.compat.JeiReflectionCompat;
import dev.polaris_light.backpack_side_gui.network.ClientPacketSender;
import dev.polaris_light.backpack_side_gui.network.payload.AnvilSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackAvailabilityPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.FurnaceSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SmithingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.StonecutterSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.UtilityFlagsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import top.theillusivec4.curios.api.CuriosApi;

public final class SideBackpackClient {
    private static final OverlayWidget OVERLAY = new OverlayWidget();
    private static int refreshTicks;
    private static boolean hasBackpack;
    private static double lastMouseX, lastMouseY;
    private static List<ItemStack> syncedBackpackItems = List.of();
    private static boolean backpackAvailabilitySynced;

    public static boolean canFillFromBackpacks(List<List<ItemStack>> groups) {
        if (Minecraft.getInstance().player == null || groups == null || groups.isEmpty())
            return false;
        List<ItemStack> items = new java.util.ArrayList<>();
        if (backpackAvailabilitySynced)
            for (ItemStack stack : syncedBackpackItems)
                items.add(stack.copy());
        else {
            for (ItemStack s : Minecraft.getInstance().player.getInventory().items)
                collect(s, items);
            CuriosApi.getCuriosInventory(Minecraft.getInstance().player)
                    .ifPresent(curiosInventory -> curiosInventory.getCurios().values().forEach(curioHandler -> {
                        IItemHandler handler = curioHandler.getStacks();
                        for (int i = 0; i < handler.getSlots(); i++)
                            collect(handler.getStackInSlot(i), items);
                    }));
        }
        for (List<ItemStack> options : groups) {
            if (options == null || options.isEmpty())
                continue;
            int found = -1;
            for (int i = 0; i < items.size() && found < 0; i++)
                for (ItemStack o : options)
                    if (o != null && !o.isEmpty() && ItemStack.isSameItemSameComponents(items.get(i), o)
                            && items.get(i).getCount() >= Math.max(1, o.getCount())) {
                        found = i;
                        break;
                    }
            if (found < 0)
                return false;
            items.get(found).shrink(1);
        }
        return true;
    }

    public static void receiveBackpackAvailability(BackpackAvailabilityPayload payload) {
        syncedBackpackItems = payload.items().stream().map(ItemStack::copy).toList();
        backpackAvailabilitySynced = true;
    }

    private static void collect(ItemStack backpack, List<ItemStack> out) {
        if (backpack == null || backpack.isEmpty() || !(backpack.getItem() instanceof BackpackItem))
            return;
        IItemHandler h = backpack.getCapability(Capabilities.ItemHandler.ITEM);
        if (h == null)
            return;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty())
                out.add(s.copy());
        }
    }

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

    public static boolean isCraftingUtilityVisible() {
        return hasBackpack && OVERLAY.isCraftingVisible();
    }

    public static boolean isSmithingUtilityVisible() {
        return hasBackpack && OVERLAY.isSmithingVisible();
    }

    public static void receiveAnvil(AnvilSyncPayload payload) {
        OVERLAY.receiveAnvil(payload);
    }

    public static void receiveFurnace(FurnaceSyncPayload payload) {
        OVERLAY.receiveFurnace(payload);
    }

    public static void receiveStonecutter(StonecutterSyncPayload payload) {
        OVERLAY.receiveStonecutter(payload);
    }

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
        if (minecraft.player != null && ++refreshTicks >= 5) {
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
        if (hasBackpack)
            OVERLAY.setContents(name, stacks, limits);
    }

    public static void receiveCarried(ItemStack carried) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen<?> screen)
            screen.getMenu().setCarried(carried == null ? ItemStack.EMPTY : carried.copy());
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        lastMouseX = event.getMouseX();
        lastMouseY = event.getMouseY();
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
        if (hasBackpack && BackpackOverlayScreenPolicy.allows(event.getScreen())) {
            ItemStack hovered = OVERLAY.stackAt(lastMouseX, lastMouseY);
            if (!hovered.isEmpty() && event.getKeyCode() == InputConstants.KEY_R) {
                if (JeiReflectionCompat.showItemRecipes(hovered)) {
                    event.setCanceled(true);
                    return;
                }
            }
            if (!hovered.isEmpty() && event.getKeyCode() == InputConstants.KEY_U) {
                if (JeiReflectionCompat.showItemUses(hovered)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
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
