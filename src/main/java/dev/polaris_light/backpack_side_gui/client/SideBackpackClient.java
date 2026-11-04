package dev.polaris_light.backpack_side_gui.client;

import java.util.ArrayList;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import top.theillusivec4.curios.api.CuriosApi;

public final class SideBackpackClient {
    private static OverlayWidget OVERLAY = new OverlayWidget();
    private static int refreshTicks;
    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static boolean hasBackpack;
    private static double lastMouseX, lastMouseY;
    private static List<ItemStack> syncedBackpackItems = List.of();
    private static boolean backpackAvailabilitySynced;
    private static AbstractContainerScreen<?> lastScreen;
    private static LocalPlayer trackedPlayer;
    private static ClientLevel trackedLevel;
    private static boolean playerContextInitialized;

    private static void ensurePlayerContext() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (playerContextInitialized && player == trackedPlayer && level == trackedLevel)
            return;

        boolean replaced = playerContextInitialized && (player != trackedPlayer || level != trackedLevel);
        trackedPlayer = player;
        trackedLevel = level;
        playerContextInitialized = true;

        OVERLAY = new OverlayWidget();
        hasBackpack = false;
        syncedBackpackItems = List.of();
        backpackAvailabilitySynced = false;
        refreshTicks = 0;
        lastMouseX = 0;
        lastMouseY = 0;
        lastScreen = null;

        if (replaced && minecraft.screen instanceof AbstractContainerScreen<?>)
            minecraft.setScreen(null);
    }

    public static boolean canFillFromBackpacks(List<List<ItemStack>> groups) {
        ensurePlayerContext();
        if (Minecraft.getInstance().player == null || groups == null || groups.isEmpty())
            return false;
        return missingIngredientIndexes(groups).isEmpty();
    }

    /** Returns recipe input indexes that cannot be supplied by the player or backpacks. */
    public static List<Integer> missingIngredientIndexes(List<List<ItemStack>> groups) {
        ensurePlayerContext();
        if (Minecraft.getInstance().player == null || groups == null || groups.isEmpty())
            return List.of();
        List<ItemStack> items = availableItems();
        List<Integer> missing = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            List<ItemStack> options = groups.get(groupIndex);
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
                missing.add(groupIndex);
            else
                items.get(found).shrink(requiredCount(options, items.get(found)));
        }
        return missing;
    }

    private static List<ItemStack> availableItems() {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : Minecraft.getInstance().player.getInventory().items)
            if (!stack.isEmpty())
                items.add(stack.copy());
        if (backpackAvailabilitySynced)
            for (ItemStack stack : syncedBackpackItems)
                items.add(stack.copy());
        else {
            CuriosApi.getCuriosInventory(Minecraft.getInstance().player)
                    .ifPresent(curiosInventory -> curiosInventory.getCurios().values().forEach(curioHandler -> {
                        IItemHandler handler = curioHandler.getStacks();
                        for (int i = 0; i < handler.getSlots(); i++)
                            collect(handler.getStackInSlot(i), items);
                    }));
        }
        return items;
    }

    private static int requiredCount(List<ItemStack> options, ItemStack available) {
        return options.stream()
                .filter(option -> option != null && !option.isEmpty()
                        && ItemStack.isSameItemSameComponents(available, option))
                .findFirst()
                .map(option -> Math.max(1, option.getCount()))
                .orElse(1);
    }

    public static void receiveBackpackAvailability(BackpackAvailabilityPayload payload) {
        ensurePlayerContext();
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
        ensurePlayerContext();
        OVERLAY.setUtilityFlags(new boolean[] { payload.crafting(), payload.furnace(), payload.anvil(),
                payload.smithing(), payload.stonecutter() });
    }

    public static void receiveSmithing(SmithingSyncPayload payload) {
        ensurePlayerContext();
        OVERLAY.receiveSmithing(payload);
    }

    public static void receiveCrafting(CraftingSyncPayload payload) {
        ensurePlayerContext();
        OVERLAY.receiveCrafting(payload);
    }

    public static boolean isCraftingUtilityVisible() {
        ensurePlayerContext();
        return hasBackpack && OVERLAY.isCraftingVisible();
    }

    public static boolean isSmithingUtilityVisible() {
        ensurePlayerContext();
        return hasBackpack && OVERLAY.isSmithingVisible();
    }

    public static void receiveAnvil(AnvilSyncPayload payload) {
        ensurePlayerContext();
        OVERLAY.receiveAnvil(payload);
    }

    public static void receiveFurnace(FurnaceSyncPayload payload) {
        ensurePlayerContext();
        OVERLAY.receiveFurnace(payload);
    }

    public static void receiveStonecutter(StonecutterSyncPayload payload) {
        ensurePlayerContext();
        OVERLAY.receiveStonecutter(payload);
    }

    public static boolean shouldBlockContainerInput(
            AbstractContainerScreen<?> screen, double mouseX,
            double mouseY) {
        ensurePlayerContext();
        return hasBackpack && BackpackOverlayScreenPolicy.allows(screen)
                && OVERLAY.panelInteractiveContains(screen, mouseX, mouseY);
    }

    public static boolean shouldBlockContainerTooltip(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        ensurePlayerContext();
        return hasBackpack && BackpackOverlayScreenPolicy.allows(screen)
                && OVERLAY.panelInteractiveContains(screen, mouseX, mouseY);
    }

    private SideBackpackClient() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ensurePlayerContext();
        AbstractContainerScreen<?> current = minecraft.screen instanceof AbstractContainerScreen<?> s ? s : null;
        if (current != lastScreen) {
            lastScreen = current;
            refreshTicks = REFRESH_INTERVAL_TICKS;
            backpackAvailabilitySynced = false;
        }
        if (minecraft.player == null) {
            hasBackpack = false;
            syncedBackpackItems = List.of();
            backpackAvailabilitySynced = false;
            refreshTicks = 0;
            return;
        }
        if (current != null && ++refreshTicks >= REFRESH_INTERVAL_TICKS) {
            refreshTicks = 0;
            ClientPacketSender.open();
        }
    }

    public static void receive(String name, List<ItemStack> stacks) {
        ensurePlayerContext();
        hasBackpack = name != null && !name.isBlank();
        if (!hasBackpack)
            return;
        OVERLAY.setContents(name, stacks);
    }

    public static void receive(String name, List<ItemStack> stacks, List<Integer> limits) {
        receive(name, stacks, limits, List.of());
    }

    public static void receive(String name, List<ItemStack> stacks, List<Integer> limits,
            List<Boolean> infiniteSlots) {
        ensurePlayerContext();
        hasBackpack = name != null && !name.isBlank();
        if (hasBackpack)
            OVERLAY.setContents(name, stacks, limits, infiniteSlots);
    }

    public static void receiveCarried(ItemStack carried) {
        ensurePlayerContext();
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen<?> screen)
            screen.getMenu().setCarried(carried == null ? ItemStack.EMPTY : carried.copy());
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        ensurePlayerContext();
        lastMouseX = event.getMouseX();
        lastMouseY = event.getMouseY();
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (!hasBackpack)
            return;
        GuiGraphics graphics = event.getGuiGraphics();
        RenderSystem.disableDepthTest();
        graphics.pose().pushPose();
        // Why 330.0F? Ask Mojang.
        graphics.pose().translate(0.0F, 0.0F, 330.0F);
        try {
            Minecraft minecraft = Minecraft.getInstance();
            OVERLAY.setAnchorPosition(event.getScreen().width / 2 + BackpackSideGuiConfig.OVERLAY_X.get(),
                    event.getScreen().height / 2 + BackpackSideGuiConfig.OVERLAY_Y.get());
            OVERLAY.render(event.getScreen(), graphics, minecraft);
            OVERLAY.renderSlotFeedback(graphics, event.getMouseX(), event.getMouseY());
            OVERLAY.renderTooltip(graphics, minecraft, event.getMouseX(), event.getMouseY());
        } finally {
            graphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    public static void onMousePressedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        ensurePlayerContext();
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
        ensurePlayerContext();
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (!hasBackpack)
            return;
        if (OVERLAY.mouseDragged(event))
            event.setCanceled(true);
    }

    public static void onMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        ensurePlayerContext();
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (!hasBackpack)
            return;
        if (OVERLAY.mouseReleased(event))
            event.setCanceled(true);
    }

    public static void onMouseScrolledPre(ScreenEvent.MouseScrolled.Pre event) {
        ensurePlayerContext();
        if (!BackpackOverlayScreenPolicy.allows(event.getScreen()))
            return;
        if (OVERLAY.mouseScrolled(event))
            event.setCanceled(true);
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        ensurePlayerContext();
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
        ensurePlayerContext();
        if (hasBackpack && BackpackOverlayScreenPolicy.allows(event.getScreen())
                && OVERLAY.charTyped(event.getCodePoint()))
            event.setCanceled(true);
    }
}
