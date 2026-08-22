package dev.polaris_light.backpack_side_gui.client;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiConfig;
import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import dev.polaris_light.backpack_side_gui.compat.JeiReflectionCompat;
import dev.polaris_light.backpack_side_gui.network.JeiRetryTransferPayload;
import dev.polaris_light.backpack_side_gui.network.ModNetwork;
import dev.polaris_light.backpack_side_gui.network.PanelSyncPayload;
import dev.polaris_light.backpack_side_gui.network.UtilitySyncPayload;
import dev.polaris_light.backpack_side_gui.server.ServerBackpackAccess;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class SideBackpackClient {
    private static final int SLOT_SIZE = 18;
    private static final int COLUMNS = 9;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int BUTTON_SIZE = 14;
    private static final int BUTTON_GAP = 3;
    private static final long DOUBLE_CLICK_MS = 300;
    private static final int LARGE_STACK_PREVIEW_LIMIT = 4096;
    private static final int TOP_BUTTON_SIZE = 14;
    private static final int SEARCH_BUTTON = 0;
    private static final int SORT_BUTTON = 1;
    private static final int CATEGORY_BUTTON = 2;
    private static final int UTILITY_CRAFTING = 0;
    private static final int UTILITY_FURNACE = 1;
    private static final int UTILITY_ANVIL = 2;
    private static final int UTILITY_SMITHING = 3;
    private static final int UTILITY_PANEL_HEIGHT = 66;
    private static final ResourceLocation MOVE_ICON = ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "textures/gui/move.png");
    private static final ResourceLocation SHOW_ICON = ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "textures/gui/show.png");
    private static final ResourceLocation HIDE_ICON = ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "textures/gui/hide.png");
    private static final ResourceLocation SEARCH_ICON = ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "textures/gui/search.png");
    private static final ResourceLocation SORT_ICON = ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "textures/gui/sort.png");
    private static final ResourceLocation UTILITY_CRAFTING_ICON = ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "textures/gui/utility_crafting.png");
    private static final ResourceLocation UTILITY_FURNACE_ICON = ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "textures/gui/utility_furnace.png");
    private static final ResourceLocation UTILITY_ANVIL_ICON = ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "textures/gui/utility_anvil.png");
    private static final ResourceLocation UTILITY_SMITHING_ICON = ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "textures/gui/utility_smithing.png");
    private static final String[] SORT_MODE_LABELS = {"1", "T", "A", "M"};
    public static final KeyMapping QUICK_TRANSFER_KEY = new KeyMapping("key.backpack_side_gui.quick_transfer", InputConstants.Type.KEYSYM, 71, "key.categories.backpack_side_gui");
    private static boolean hoveredSlotFieldResolved = false;
    private static boolean available = false;
    private static boolean requestedOnce = false;
    private static int backpackSlot = -1;
    private static int slotCount = 0;
    private static String displayName = "";
    private static final UtilityPanelState utilityState = new UtilityPanelState();
    private static boolean hasCraftingUpgrade = false;
    private static boolean hasFurnaceUpgrade = false;
    private static boolean hasAnvilUpgrade = false;
    private static boolean hasSmithingUpgrade = false;
    private static int activeUtilityPanel = -1;
    private static final List<ItemStack> items = new ArrayList();
    private static int scrollRow = 0;
    private static int tickCounter = 0;
    private static int clickLockedTicks = 0;
    private static boolean searchOpen = false;
    private static String searchText = "";
    private static int sortMode = 0;
    private static boolean jeiTransferAssistWaiting = false;
    private static int utilitySyncTick = 0;
    private static int utilitySyncType = -1;
    private static final List<ItemStack> utilityItems = new ArrayList();
    private static int furnaceLitTime = 0;
    private static int furnaceLitDuration = 0;
    private static int furnaceCookProgress = 0;
    private static int furnaceCookTotal = 200;
    private static int anvilCost = 0;
    private static String anvilName = "";
    private static boolean anvilNameFocused = false;
    private static double pendingJeiTransferMouseX = 0.0d;
    private static double pendingJeiTransferMouseY = 0.0d;
    private static int pendingJeiTransferButton = 0;
    private static long lastClickMs = 0;
    private static int lastClickSlot = -1;
    private static int lastClickButton = -1;
    private static boolean draggingStack = false;
    private static int dragButton = 0;
    private static final Set<Integer> dragSlots = new LinkedHashSet();
    private static boolean dragMovedAcrossSlots = false;
    private static boolean draggingUtilityStack = false;
    private static int dragUtilityType = -1;
    private static int dragUtilityButton = 0;
    private static final Set<Integer> dragUtilitySlots = new LinkedHashSet();
    private static boolean utilityDragMovedAcrossSlots = false;
    private static long lastUtilityClickMs = 0;
    private static int lastUtilityClickSlot = -1;
    private static int lastUtilityClickButton = -1;
    private static int lastUtilityClickType = -1;
    private static boolean scrollbarDragging = false;
    private static boolean movingPanel = false;
    private static int moveStartMouseX = 0;
    private static int moveStartMouseY = 0;
    private static int moveStartXOffset = 0;
    private static int moveStartYOffset = 0;
    private static int panelDragXOffset = 0;
    private static int panelDragYOffset = 0;
    private static boolean carriedRenderOverrideActive = false;
    private static ItemStack carriedRenderOriginal = ItemStack.EMPTY;

    private SideBackpackClient() {
    }

    public static void receiveSync(PanelSyncPayload packet) {
        clickLockedTicks = 0;
        available = packet.available();
        backpackSlot = packet.backpackSlot();
        slotCount = packet.slotCount();
        displayName = packet.displayName();
        hasCraftingUpgrade = packet.craftingUpgrade();
        hasFurnaceUpgrade = packet.furnaceUpgrade();
        hasAnvilUpgrade = packet.anvilUpgrade();
        hasSmithingUpgrade = packet.smithingUpgrade();
        utilityState.hasCraftingUpgrade = hasCraftingUpgrade;
        utilityState.hasFurnaceUpgrade = hasFurnaceUpgrade;
        utilityState.hasAnvilUpgrade = hasAnvilUpgrade;
        utilityState.hasSmithingUpgrade = hasSmithingUpgrade;
        utilityState.activePanel = activeUtilityPanel;
        if (!isUtilityTypeVisible(activeUtilityPanel)) {
            activeUtilityPanel = -1;
            utilityState.activePanel = -1;
        }
        items.clear();
        items.addAll(packet.items());
        clampScroll();
    }

    public static void receiveUtilitySync(UtilitySyncPayload packet) {
        utilityState.applySync(packet.utilityType(), packet.items(), packet.furnaceLitTime(), packet.furnaceLitDuration(),
                packet.furnaceCookProgress(), packet.furnaceCookTotal(), packet.anvilCost(), packet.anvilName());
        utilitySyncType = packet.utilityType();
        utilityItems.clear();
        utilityItems.addAll(packet.items());
        furnaceLitTime = packet.furnaceLitTime();
        furnaceLitDuration = packet.furnaceLitDuration();
        furnaceCookProgress = packet.furnaceCookProgress();
        furnaceCookTotal = packet.furnaceCookTotal();
        anvilCost = packet.anvilCost();
        anvilName = packet.anvilName() == null ? "" : packet.anvilName();
    }

    public static void receiveCursorSync(ItemStack carried) {
        Minecraft mc = Minecraft.getInstance();
        restoreCarriedRenderOverride(mc);
        if (mc.player != null && mc.player.containerMenu != null) {
            mc.player.containerMenu.setCarried(carried == null ? ItemStack.EMPTY : carried.copy());
        }
    }

    public static void receiveJeiRetryTransfer(JeiRetryTransferPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        boolean wasWaiting = jeiTransferAssistWaiting;
        jeiTransferAssistWaiting = false;
        if (payload != null && payload.utilityToOpen() == 0) {
            activeUtilityPanel = 0;
            utilitySyncType = 0;
            ModNetwork.sendUtilityRequest(backpackSlot, 0);
        } else if (wasWaiting && payload != null && payload.retryOriginalClick() && mc.screen != null && JeiReflectionCompat.isJeiRecipesScreen(mc.screen)) {
            try {
                mc.screen.mouseClicked(pendingJeiTransferMouseX, pendingJeiTransferMouseY, pendingJeiTransferButton);
                mc.screen.mouseReleased(pendingJeiTransferMouseX, pendingJeiTransferMouseY, pendingJeiTransferButton);
            } catch (Throwable th) {
            }
        }
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(QUICK_TRANSFER_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        restoreCarriedRenderOverride(mc);
        if (clickLockedTicks > 0) {
            clickLockedTicks--;
        }
        Screen screen = mc.screen;
        if (JeiReflectionCompat.isJeiRecipesScreen(screen)) {
            tickCounter++;
            if (!requestedOnce || tickCounter % 10 == 0) {
                requestedOnce = true;
                ModNetwork.sendRequest();
                return;
            }
            return;
        }
        if (!(screen instanceof AbstractContainerScreen)) {
            requestedOnce = false;
            available = false;
            hasCraftingUpgrade = false;
            hasFurnaceUpgrade = false;
            hasAnvilUpgrade = false;
            hasSmithingUpgrade = false;
            jeiTransferAssistWaiting = false;
            cancelDragState();
            return;
        }
        if (isBackpackScreen(screen)) {
            requestedOnce = false;
            return;
        }
        tickCounter++;
        if (!requestedOnce || tickCounter % 10 == 0) {
            requestedOnce = true;
            ModNetwork.sendRequest();
        }
        if (activeUtilityPanel >= 0 && isUtilityTypeVisible(activeUtilityPanel) && !((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
            utilitySyncTick++;
            if (utilitySyncTick % 5 == 0) {
                ModNetwork.sendUtilityRequest(backpackSlot, activeUtilityPanel);
            }
        }
        handleQuickTransferKey(mc);
    }

    private static void handleQuickTransferKey(Minecraft mc) {
        if (!available || searchOpen || mc == null) {
            return;
        }
        Screen currentScreen = mc.screen;
        AbstractContainerScreen<?> abstractContainerScreen = currentScreen instanceof AbstractContainerScreen<?> c ? c : null;
        if (abstractContainerScreen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> screen = abstractContainerScreen;
            if (isBackpackScreen(screen)) {
                return;
            }
            while (QUICK_TRANSFER_KEY.consumeClick()) {
                tryQuickTransferAtMouse(mc, screen);
            }
        }
    }

    private static boolean tryQuickTransferAtMouse(Minecraft mc, AbstractContainerScreen<?> screen) {
        if (!available || searchOpen || mc == null || screen == null || isBackpackScreen(screen)) {
            return false;
        }
        double mouseX = (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()) / mc.getWindow().getScreenWidth();
        double mouseY = (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()) / mc.getWindow().getScreenHeight();
        BackpackPanelLayout.PanelRect rect = getBackpackPanelLayoutPanelRect(screen.width, screen.height);
        int panelSlot = !((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue() ? getHoveredSlot(rect, mouseX, mouseY) : -1;
        if (!((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue() && panelSlot < 0 && panelInteractiveContains(rect, mouseX, mouseY)) {
            return false;
        }
        int menuSlot = -1;
        if (panelSlot < 0) {
            menuSlot = getHoveredMenuSlot(screen);
        }
        if (panelSlot >= 0 || menuSlot >= 0) {
            clickLockedTicks = 10;
            ModNetwork.sendQuickTransfer(backpackSlot, panelSlot, menuSlot);
            return true;
        }
        return false;
    }

    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        restoreCarriedRenderOverride(mc);
        Screen currentScreen = mc.screen;
        AbstractContainerScreen<?> abstractContainerScreen = currentScreen instanceof AbstractContainerScreen<?> c ? c : null;
        if (abstractContainerScreen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> screen = abstractContainerScreen;
            if (!available || isBackpackScreen(screen)) {
                return;
            }
            if ((!draggingStack && !draggingUtilityStack) || mc.player == null || mc.player.containerMenu == null) {
                return;
            }
            ItemStack carried = mc.player.containerMenu.getCarried();
            if (carried.isEmpty()) {
                return;
            }
            Map<Integer, ItemStack> dragPreview = draggingUtilityStack ? buildUtilityDragPreview(mc) : buildDragPreview(mc);
            int assigned = draggingUtilityStack ? getAssignedUtilityDragCount(dragPreview) : getAssignedDragCount(dragPreview);
            if (assigned <= 0) {
                return;
            }
            int remaining = Math.max(0, carried.getCount() - assigned);
            carriedRenderOriginal = carried.copy();
            carriedRenderOverrideActive = true;
            if (remaining <= 0) {
                mc.player.containerMenu.setCarried(ItemStack.EMPTY);
                return;
            }
            ItemStack shown = carried.copy();
            shown.setCount(remaining);
            mc.player.containerMenu.setCarried(shown);
        }
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        restoreCarriedRenderOverride(mc);
        Screen currentScreen = mc.screen;
        AbstractContainerScreen<?> abstractContainerScreen = currentScreen instanceof AbstractContainerScreen<?> c ? c : null;
        if (!(abstractContainerScreen instanceof AbstractContainerScreen)) {
            return;
        }
        AbstractContainerScreen<?> screen = abstractContainerScreen;
        if (isBackpackScreen(screen)) {
            return;
        }
        BackpackPanelLayout.PanelRect rect = getBackpackPanelLayoutPanelRect(screen.width, screen.height);
        GuiGraphics graphics = event.getGuiGraphics();
        if (!available) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 500.0f);
        if (((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
            renderPanelButtons(graphics, rect);
            renderPanelButtonTooltips(graphics, mc, rect, event.getMouseX(), event.getMouseY());
            graphics.pose().popPose();
        } else {
            renderPanel(graphics, mc, rect, event.getMouseX(), event.getMouseY());
            if (panelInteractiveContains(rect, event.getMouseX(), event.getMouseY())) {
                PanelRenderer.renderCarriedStack(graphics, mc, getCarriedStackForRender(mc), event.getMouseX(), event.getMouseY());
            }
            graphics.pose().popPose();
        }
    }

    public static void onMousePressedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (tryHandleJeiTransferAssistClick(mc, event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        Screen currentScreen = mc.screen;
        AbstractContainerScreen<?> abstractContainerScreen = currentScreen instanceof AbstractContainerScreen<?> c ? c : null;
        if (abstractContainerScreen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> screen = abstractContainerScreen;
            if (isBackpackScreen(screen)) {
                return;
            }
            BackpackPanelLayout.PanelRect rect = getBackpackPanelLayoutPanelRect(screen.width, screen.height);
            if (!available) {
                return;
            }
            if (moveButtonContains(rect, event.getMouseX(), event.getMouseY())) {
                movingPanel = true;
                moveStartMouseX = (int) event.getMouseX();
                moveStartMouseY = (int) event.getMouseY();
                moveStartXOffset = ((Integer) BackpackSideGuiConfig.PANEL_X_OFFSET.get()).intValue();
                moveStartYOffset = ((Integer) BackpackSideGuiConfig.PANEL_Y_OFFSET.get()).intValue();
                panelDragXOffset = moveStartXOffset;
                panelDragYOffset = moveStartYOffset;
                event.setCanceled(true);
                return;
            }
            if (showHideButtonContains(rect, event.getMouseX(), event.getMouseY())) {
                setPanelHidden(!((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue());
                event.setCanceled(true);
                return;
            }
            if (!((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
                if (activeUtilityPanel == 2 && anvilNameBoxContains(rect, event.getMouseX(), event.getMouseY())) {
                    anvilNameFocused = true;
                    event.setCanceled(true);
                    return;
                }
                if (activeUtilityPanel == 2 && !anvilNameBoxContains(rect, event.getMouseX(), event.getMouseY())) {
                    anvilNameFocused = false;
                }
                int utility = getUtilityButtonType(rect, event.getMouseX(), event.getMouseY());
                if (utility >= 0) {
                    activeUtilityPanel = activeUtilityPanel == utility ? -1 : utility;
                    utilityItems.clear();
                    utilitySyncType = activeUtilityPanel;
                if (activeUtilityPanel >= 0) {
                    ModNetwork.sendUtilityRequest(backpackSlot, activeUtilityPanel);
                }
                utilityState.activePanel = activeUtilityPanel;
                    event.setCanceled(true);
                    return;
                }
                if (utilityPanelContains(rect, event.getMouseX(), event.getMouseY())) {
                    int utilitySlot = getUtilitySlotAt(rect, event.getMouseX(), event.getMouseY());
                    if (utilitySlot >= 0) {
                        int button = event.getButton() == 1 ? 1 : 0;
                        long now = System.currentTimeMillis();
                        if (button == 0 && lastUtilityClickButton == 0 && lastUtilityClickType == activeUtilityPanel && lastUtilityClickSlot == utilitySlot && now - lastUtilityClickMs <= DOUBLE_CLICK_MS) {
                            clickLockedTicks = 8;
                            ModNetwork.sendUtilityDoubleCollect(backpackSlot, activeUtilityPanel, utilitySlot, getClientCarriedSnapshot(mc));
                            lastUtilityClickMs = 0L;
                            lastUtilityClickSlot = -1;
                            lastUtilityClickButton = -1;
                            lastUtilityClickType = -1;
                        } else {
                            ItemStack carried = (mc.player == null || mc.player.containerMenu == null) ? ItemStack.EMPTY : mc.player.containerMenu.getCarried();
                            if (!carried.isEmpty() && carried.getCount() > 1 && isUtilityDragInputSlot(activeUtilityPanel, utilitySlot)) {
                                draggingUtilityStack = true;
                                dragUtilityType = activeUtilityPanel;
                                dragUtilityButton = button;
                                dragUtilitySlots.clear();
                                dragUtilitySlots.add(Integer.valueOf(utilitySlot));
                                utilityDragMovedAcrossSlots = false;
                            } else {
                                clickLockedTicks = SCROLLBAR_WIDTH;
                                ModNetwork.sendUtilityClick(backpackSlot, activeUtilityPanel, utilitySlot, button, getClientCarriedSnapshot(mc));
                                rememberUtilityClick(activeUtilityPanel, utilitySlot, button);
                            }
                        }
                    }
                    event.setCanceled(true);
                    return;
                }
            }
            if (!available || ((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
                return;
            }
            if (panelInteractiveContains(rect, event.getMouseX(), event.getMouseY())) {
                event.setCanceled(true);
            }
            int topButton = getTopButtonIndex(rect, event.getMouseX(), event.getMouseY());
            if (topButton == 0) {
                searchOpen = !searchOpen;
                event.setCanceled(true);
                return;
            }
            if (topButton == 1) {
                clickLockedTicks = 10;
                ModNetwork.sendSort(backpackSlot, sortMode);
                event.setCanceled(true);
                return;
            }
            if (topButton == 2) {
                sortMode = (sortMode + 1) % SORT_MODE_LABELS.length;
                event.setCanceled(true);
                return;
            }
            if (searchOpen && searchBarContains(rect, event.getMouseX(), event.getMouseY())) {
                event.setCanceled(true);
                return;
            }
            if (clickLockedTicks > 0) {
                event.setCanceled(true);
                return;
            }
            if (rect.scrollbarContains(event.getMouseX(), event.getMouseY())) {
                scrollbarDragging = true;
                updateScrollFromMouse(rect, event.getMouseY());
                event.setCanceled(true);
                return;
            }
            int slot = getHoveredSlot(rect, event.getMouseX(), event.getMouseY());
            if (slot < 0 || slot >= slotCount) {
                return;
            }
            int button2 = event.getButton() == 1 ? 1 : 0;
            long now2 = System.currentTimeMillis();
            if (button2 == 0 && lastClickButton == 0 && lastClickSlot == slot && now2 - lastClickMs <= DOUBLE_CLICK_MS) {
                clickLockedTicks = 10;
                ModNetwork.sendDoubleCollect(backpackSlot, slot, getClientCarriedSnapshot(mc));
                lastClickMs = 0L;
                lastClickSlot = -1;
                lastClickButton = -1;
                event.setCanceled(true);
                return;
            }
            ItemStack carried2 = (mc.player == null || mc.player.containerMenu == null) ? ItemStack.EMPTY : mc.player.containerMenu.getCarried();
            if (!carried2.isEmpty()) {
                if (carried2.getCount() <= 1) {
                    clickLockedTicks = 10;
                    ModNetwork.sendClick(backpackSlot, slot, button2, getClientCarriedSnapshot(mc));
                    rememberClick(slot, button2);
                    event.setCanceled(true);
                    return;
                }
                draggingStack = true;
                dragButton = button2;
                dragSlots.clear();
                dragSlots.add(Integer.valueOf(slot));
                dragMovedAcrossSlots = false;
                event.setCanceled(true);
                return;
            }
            clickLockedTicks = 10;
            ModNetwork.sendClick(backpackSlot, slot, button2, getClientCarriedSnapshot(mc));
            rememberClick(slot, button2);
            event.setCanceled(true);
        }
    }

    public static void onMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        AbstractContainerScreen<?> abstractContainerScreen = currentScreen instanceof AbstractContainerScreen<?> c ? c : null;
        if (abstractContainerScreen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> screen = abstractContainerScreen;
            if (!isBackpackScreen(screen)) {
                BackpackPanelLayout.PanelRect rect = getBackpackPanelLayoutPanelRect(screen.width, screen.height);
                if (movingPanel) {
                    commitPanelDragOffsets();
                    movingPanel = false;
                    saveClientConfig();
                    event.setCanceled(true);
                    return;
                }
                if (((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
                    if (moveButtonContains(rect, event.getMouseX(), event.getMouseY()) || showHideButtonContains(rect, event.getMouseX(), event.getMouseY())) {
                        event.setCanceled(true);
                        return;
                    }
                    return;
                }
                if (scrollbarDragging) {
                    scrollbarDragging = false;
                    event.setCanceled(true);
                    return;
                }
                if (draggingUtilityStack) {
                    int hovered = getUtilitySlotAt(rect, event.getMouseX(), event.getMouseY());
                    if (hovered >= 0 && isUtilityDragInputSlot(dragUtilityType, hovered)) {
                        if (!dragUtilitySlots.contains(Integer.valueOf(hovered))) {
                            utilityDragMovedAcrossSlots = true;
                        }
                        dragUtilitySlots.add(Integer.valueOf(hovered));
                    }
                    clickLockedTicks = 8;
                    if (dragUtilitySlots.size() > 1 || utilityDragMovedAcrossSlots) {
                        ModNetwork.sendUtilityDragDistribute(backpackSlot, dragUtilityType, new ArrayList(dragUtilitySlots), dragUtilityButton, getClientCarriedSnapshot(mc));
                    } else if (!dragUtilitySlots.isEmpty()) {
                        int only = dragUtilitySlots.iterator().next().intValue();
                        ModNetwork.sendUtilityClick(backpackSlot, dragUtilityType, only, dragUtilityButton, getClientCarriedSnapshot(mc));
                        rememberUtilityClick(dragUtilityType, only, dragUtilityButton);
                    }
                    cancelUtilityDragState();
                    event.setCanceled(true);
                    return;
                }
                if (draggingStack) {
                    int hovered2 = getHoveredSlot(rect, event.getMouseX(), event.getMouseY());
                    if (hovered2 >= 0 && hovered2 < slotCount) {
                        if (!dragSlots.contains(Integer.valueOf(hovered2))) {
                            dragMovedAcrossSlots = true;
                        }
                        dragSlots.add(Integer.valueOf(hovered2));
                    }
                    clickLockedTicks = 10;
                    if (dragSlots.size() > 1 || dragMovedAcrossSlots) {
                        ModNetwork.sendDragDistribute(backpackSlot, new ArrayList(dragSlots), dragButton, getClientCarriedSnapshot(mc));
                    } else if (!dragSlots.isEmpty()) {
                        int only2 = dragSlots.iterator().next().intValue();
                        ModNetwork.sendClick(backpackSlot, only2, dragButton, getClientCarriedSnapshot(mc));
                        rememberClick(only2, dragButton);
                    }
                    cancelDragState();
                    event.setCanceled(true);
                    return;
                }
                if (clickLockedTicks > 0 || panelInteractiveContains(rect, event.getMouseX(), event.getMouseY())) {
                    event.setCanceled(true);
                    return;
                }
                return;
            }
        }
        cancelDragState();
        movingPanel = false;
    }

    public static void onMouseDraggedPre(ScreenEvent.MouseDragged.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        AbstractContainerScreen<?> abstractContainerScreen = currentScreen instanceof AbstractContainerScreen<?> c ? c : null;
        if (abstractContainerScreen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> screen = abstractContainerScreen;
            if (isBackpackScreen(screen)) {
                return;
            }
            BackpackPanelLayout.PanelRect rect = getBackpackPanelLayoutPanelRect(screen.width, screen.height);
            if (movingPanel) {
                int dx = ((int) event.getMouseX()) - moveStartMouseX;
                int dy = ((int) event.getMouseY()) - moveStartMouseY;
                setPanelDragOffsets(moveStartXOffset + dx, moveStartYOffset + dy);
                event.setCanceled(true);
                return;
            }
            if (!available || ((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
                return;
            }
            if (scrollbarDragging) {
                updateScrollFromMouse(rect, event.getMouseY());
                event.setCanceled(true);
                return;
            }
            if (draggingUtilityStack) {
                int hovered = getUtilitySlotAt(rect, event.getMouseX(), event.getMouseY());
                if (hovered >= 0 && isUtilityDragInputSlot(dragUtilityType, hovered)) {
                    if (!dragUtilitySlots.contains(Integer.valueOf(hovered))) {
                        utilityDragMovedAcrossSlots = true;
                    }
                    dragUtilitySlots.add(Integer.valueOf(hovered));
                }
                event.setCanceled(true);
                return;
            }
            if (draggingStack) {
                int hovered2 = getHoveredSlot(rect, event.getMouseX(), event.getMouseY());
                if (hovered2 >= 0 && hovered2 < slotCount) {
                    if (!dragSlots.contains(Integer.valueOf(hovered2))) {
                        dragMovedAcrossSlots = true;
                    }
                    dragSlots.add(Integer.valueOf(hovered2));
                }
                event.setCanceled(true);
                return;
            }
            if (clickLockedTicks > 0 || panelInteractiveContains(rect, event.getMouseX(), event.getMouseY())) {
                event.setCanceled(true);
            }
        }
    }

    public static void onMouseScrolledPre(ScreenEvent.MouseScrolled.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        AbstractContainerScreen<?> abstractContainerScreen = currentScreen instanceof AbstractContainerScreen<?> c ? c : null;
        if (abstractContainerScreen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> screen = abstractContainerScreen;
            if (!available || isBackpackScreen(screen)) {
                return;
            }
            if (clickLockedTicks > 0) {
                event.setCanceled(true);
                return;
            }
            BackpackPanelLayout.PanelRect rect = getBackpackPanelLayoutPanelRect(screen.width, screen.height);
            if (((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue() || !panelInteractiveContains(rect, event.getMouseX(), event.getMouseY())) {
                return;
            }
            if (event.getScrollDeltaY() < 0.0d) {
                scrollRow++;
            } else if (event.getScrollDeltaY() > 0.0d) {
                scrollRow--;
            }
            clampScroll();
            event.setCanceled(true);
        }
    }

    public static void onKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        AbstractContainerScreen<?> abstractContainerScreen = currentScreen instanceof AbstractContainerScreen<?> c ? c : null;
        if (abstractContainerScreen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> screen = abstractContainerScreen;
            if (isBackpackScreen(screen)) {
                return;
            }
            if (!searchOpen && available && !((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
                ItemStack hoveredPanelStack = getHoveredPanelStackAtMouse(mc, screen);
                if (!hoveredPanelStack.isEmpty()) {
                    if (event.getKeyCode() == 82 && JeiReflectionCompat.showItemRecipes(hoveredPanelStack)) {
                        event.setCanceled(true);
                        return;
                    } else if (event.getKeyCode() == 85 && JeiReflectionCompat.showItemUses(hoveredPanelStack)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
            if (!searchOpen && available && QUICK_TRANSFER_KEY.matches(event.getKeyCode(), event.getScanCode()) && tryQuickTransferAtMouse(mc, screen)) {
                event.setCanceled(true);
                return;
            }
            if (anvilNameFocused && available && activeUtilityPanel == 2 && !((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
                if (event.getKeyCode() == 256 || event.getKeyCode() == 257 || event.getKeyCode() == 335) {
                    anvilNameFocused = false;
                    event.setCanceled(true);
                    return;
                } else if (event.getKeyCode() == 259) {
                    if (!anvilName.isEmpty()) {
                        anvilName = anvilName.substring(0, anvilName.length() - 1);
                        ModNetwork.sendUtilityRename(anvilName);
                    }
                    event.setCanceled(true);
                    return;
                }
            }
            if (searchOpen && available && !((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
                if (event.getKeyCode() == 256 || event.getKeyCode() == 257 || event.getKeyCode() == 335) {
                    searchOpen = false;
                    event.setCanceled(true);
                } else if (event.getKeyCode() == 259) {
                    if (!searchText.isEmpty()) {
                        searchText = searchText.substring(0, searchText.length() - 1);
                        scrollRow = 0;
                        clampScroll();
                    }
                    event.setCanceled(true);
                }
            }
        }
    }

    public static void onCharacterTypedPre(ScreenEvent.CharacterTyped.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen) || !available || ((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
            return;
        }
        char c = event.getCodePoint();
        if (anvilNameFocused && activeUtilityPanel == 2) {
            if (c >= ' ' && c != 127 && anvilName.length() < 50) {
                anvilName += c;
                ModNetwork.sendUtilityRename(anvilName);
                event.setCanceled(true);
                return;
            }
            return;
        }
        if (searchOpen && c >= ' ' && c != 127 && searchText.length() < 64) {
            searchText += c;
            scrollRow = 0;
            clampScroll();
            event.setCanceled(true);
        }
    }

    public static void onScreenClosed(ScreenEvent.Closing event) {
        restoreCarriedRenderOverride(Minecraft.getInstance());
        requestedOnce = false;
        available = false;
        hasCraftingUpgrade = false;
        hasFurnaceUpgrade = false;
        hasAnvilUpgrade = false;
        hasSmithingUpgrade = false;
        utilitySyncType = activeUtilityPanel;
        items.clear();
        scrollRow = 0;
        clickLockedTicks = 0;
        cancelDragState();
        scrollbarDragging = false;
        movingPanel = false;
        jeiTransferAssistWaiting = false;
        panelDragXOffset = ((Integer) BackpackSideGuiConfig.PANEL_X_OFFSET.get()).intValue();
        panelDragYOffset = ((Integer) BackpackSideGuiConfig.PANEL_Y_OFFSET.get()).intValue();
    }

    private static boolean tryHandleJeiTransferAssistClick(Minecraft mc, double mouseX, double mouseY, int button) {
        return false;
    }

    private static boolean hasAnyBackpackMatch(List<List<ItemStack>> ingredientGroups) {
        if (ingredientGroups == null || ingredientGroups.isEmpty()) {
            return false;
        }
        for (List<ItemStack> group : ingredientGroups) {
            if (group != null && !group.isEmpty()) {
                for (ItemStack stack : items) {
                    if (stack != null && !stack.isEmpty()) {
                        for (ItemStack option : group) {
                            if (option != null && !option.isEmpty() && stack.getItem() == option.getItem()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void renderPanel(GuiGraphics graphics, Minecraft mc, BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        int logicalSlot;
        int panelWidth = rect.width() + 8;
        int panelHeight = (rect.visibleRows() * SLOT_SIZE) + 22;
        graphics.fill(rect.x() - 4, rect.y() - SLOT_SIZE, (rect.x() - 4) + panelWidth, (rect.y() - SLOT_SIZE) + panelHeight, -871362544);
        graphics.fill(rect.x() - 4, rect.y() - SLOT_SIZE, (rect.x() - 4) + panelWidth, rect.y() - 17, -11184811);
        if (!searchOpen) {
            graphics.drawString(mc.font, (displayName == null || displayName.isEmpty()) ? Component.translatable("text.backpack_side_gui.title") : Component.literal(displayName), rect.x(), rect.y() - 14, 16777215, true);
        }
        renderTopButtonsAndSearch(graphics, mc, rect);
        List<Integer> visibleSlots = getVisibleSlots();
        int hovered = -1;
        Map<Integer, ItemStack> dragPreview = buildDragPreview(mc);
        for (int row = 0; row < rect.visibleRows(); row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int visibleIndex = ((scrollRow + row) * COLUMNS) + col;
                if (visibleIndex < visibleSlots.size() && (logicalSlot = visibleSlots.get(visibleIndex).intValue()) < slotCount) {
                    int x = rect.x() + (col * SLOT_SIZE);
                    int y = rect.y() + (row * SLOT_SIZE);
                    boolean isHovered = mouseX >= ((double) x) && mouseX < ((double) (x + SLOT_SIZE)) && mouseY >= ((double) y) && mouseY < ((double) (y + SLOT_SIZE));
                    boolean isDragSelected = draggingStack && dragSlots.contains(Integer.valueOf(logicalSlot));
                    graphics.fill(x, y, x + 17, y + 17, isDragSelected ? -10053172 : isHovered ? -8947849 : -12961222);
                    graphics.fill(x + 1, y + 1, x + 16, y + 16, -14671840);
                    ItemStack previewStack = dragPreview.get(Integer.valueOf(logicalSlot));
                    if (previewStack != null && !previewStack.isEmpty()) {
                        PanelRenderer.renderItemWithLargeCount(graphics, mc, previewStack, x + 1, y + 1);
                    } else if (logicalSlot < items.size()) {
                        ItemStack stack = items.get(logicalSlot);
                        if (!stack.isEmpty()) {
                            PanelRenderer.renderItemWithLargeCount(graphics, mc, stack, x + 1, y + 1);
                        }
                    }
                    if (isHovered) {
                        hovered = logicalSlot;
                    }
                }
            }
        }
        PanelRenderer.renderScrollbar(graphics, rect, getTotalRows(), getMaxScrollRows(), scrollRow);
        renderPanelButtons(graphics, rect);
        renderUtilityPanel(graphics, mc, rect, mouseX, mouseY);
        boolean showedButtonTooltip = renderTopButtonTooltips(graphics, mc, rect, mouseX, mouseY);
        if (!showedButtonTooltip) {
            renderPanelButtonTooltips(graphics, mc, rect, mouseX, mouseY);
        }
        ItemStack carried = (mc.player == null || mc.player.containerMenu == null) ? ItemStack.EMPTY : mc.player.containerMenu.getCarried();
        if (!showedButtonTooltip && carried.isEmpty() && hovered >= 0 && hovered < items.size() && !items.get(hovered).isEmpty()) {
            graphics.renderTooltip(mc.font, items.get(hovered), (int) mouseX, (int) mouseY);
        }
    }

    private static void renderTopButtonsAndSearch(GuiGraphics graphics, Minecraft mc, BackpackPanelLayout.PanelRect rect) {
        int y = rect.y() - 16;
        int categoryX = (rect.x() + 162) - 14;
        int sortX = (categoryX - 14) - 2;
        int searchX = (sortX - 14) - 2;
        renderSmallIconButton(graphics, SEARCH_ICON, searchX, y);
        renderSmallIconButton(graphics, SORT_ICON, sortX, y);
        renderTextButton(graphics, mc, categoryX, y, SORT_MODE_LABELS[sortMode]);
        if (searchOpen) {
            int barRight = searchX - 3;
            int barX = rect.x() + 2;
            int barW = Math.max(40, barRight - barX);
            graphics.fill(barX, y, barX + barW, y + 14, -300871407);
            graphics.fill(barX, y, barX + barW, y + 1, -8947849);
        String shown = (anvilName == null || anvilName.isEmpty()) ? "Rename" : anvilName;
            int color = (searchText == null || searchText.isEmpty()) ? 7829367 : 16777215;
            graphics.drawString(mc.font, shown, barX + 4, y + 3, color, false);
        }
    }

    private static void renderSmallIconButton(GuiGraphics graphics, ResourceLocation icon, int x, int y) {
        graphics.blit(icon, x + 1, y + 1, 0.0f, 0.0f, 12, 12, 12, 12);
    }

    private static void renderTextButton(GuiGraphics graphics, Minecraft mc, int x, int y, String text) {
        int tx = x + ((14 - mc.font.width(text)) / 2);
        graphics.drawString(mc.font, text, tx, y + 3, 16777215, true);
    }

    private static int getTopButtonIndex(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        int y = rect.y() - 16;
        int categoryX = (rect.x() + 162) - 14;
        int sortX = (categoryX - 14) - 2;
        int searchX = (sortX - 14) - 2;
        if (contains(mouseX, mouseY, searchX, y, 14, 14)) {
            return 0;
        }
        if (contains(mouseX, mouseY, sortX, y, 14, 14)) {
            return 1;
        }
        return contains(mouseX, mouseY, categoryX, y, 14, 14) ? 2 : -1;
    }

    private static boolean searchBarContains(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        if (!searchOpen) {
            return false;
        }
        int y = rect.y() - 16;
        int categoryX = (rect.x() + 162) - 14;
        int sortX = (categoryX - 14) - 2;
        int searchX = (sortX - 14) - 2;
        int barRight = searchX - 3;
        int barX = rect.x() + 2;
        int barW = Math.max(40, barRight - barX);
        return contains(mouseX, mouseY, barX, y, barW, 14);
    }

    private static boolean panelInteractiveContains(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        int panelWidth = rect.width() + 8;
        int panelHeight = (rect.visibleRows() * SLOT_SIZE) + 22;
        if (contains(mouseX, mouseY, rect.x() - 4, rect.y() - SLOT_SIZE, panelWidth, panelHeight) || searchBarContains(rect, mouseX, mouseY) || moveButtonContains(rect, mouseX, mouseY) || showHideButtonContains(rect, mouseX, mouseY)) {
            return true;
        }
        if (((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue() || getUtilityButtonType(rect, mouseX, mouseY) < 0) {
            return (!((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue() && utilityPanelContains(rect, mouseX, mouseY)) || getTopButtonIndex(rect, mouseX, mouseY) >= 0;
        }
        return true;
    }

    private static boolean renderTopButtonTooltips(GuiGraphics graphics, Minecraft mc, BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        MutableComponent mutableComponentTranslatable;
        int topButton = getTopButtonIndex(rect, mouseX, mouseY);
        if (topButton < 0) {
            return false;
        }
        if (topButton == 0) {
            mutableComponentTranslatable = Component.translatable("text.backpack_side_gui.tooltip.search");
        } else if (topButton == 1) {
            mutableComponentTranslatable = Component.translatable("text.backpack_side_gui.tooltip.sort");
        } else {
            mutableComponentTranslatable = Component.translatable("text.backpack_side_gui.tooltip.category", new Object[]{Component.translatable(getSortModeTranslationKey(sortMode))});
        }
        graphics.renderTooltip(mc.font, mutableComponentTranslatable, (int) mouseX, (int) mouseY);
        return true;
    }

    private static void renderPanelButtonTooltips(GuiGraphics graphics, Minecraft mc, BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        int us;
        if (moveButtonContains(rect, mouseX, mouseY)) {
            graphics.renderTooltip(mc.font, Component.translatable("text.backpack_side_gui.tooltip.move"), (int) mouseX, (int) mouseY);
            return;
        }
        if (showHideButtonContains(rect, mouseX, mouseY)) {
            graphics.renderTooltip(mc.font, Component.translatable(((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue() ? "text.backpack_side_gui.tooltip.show" : "text.backpack_side_gui.tooltip.hide"), (int) mouseX, (int) mouseY);
            return;
        }
        if (!((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
            int utility = getUtilityButtonType(rect, mouseX, mouseY);
            if (utility >= 0) {
                graphics.renderTooltip(mc.font, getUtilityTooltip(utility), (int) mouseX, (int) mouseY);
                return;
            }
            if (utilityPanelContains(rect, mouseX, mouseY) && (us = getUtilitySlotAt(rect, mouseX, mouseY)) >= 0 && us < utilityItems.size()) {
                ItemStack stack = utilityItems.get(us);
                if (!stack.isEmpty()) {
                    graphics.renderTooltip(mc.font, stack, (int) mouseX, (int) mouseY);
                }
            }
        }
    }

    private static String getSortModeTranslationKey(int mode) {
        switch (mode) {
            case 0:
                return "text.backpack_side_gui.sort.count";
            case 1:
                return "text.backpack_side_gui.sort.tag";
            case ServerBackpackAccess.UTILITY_ANVIL:
                return "text.backpack_side_gui.sort.name";
            case ServerBackpackAccess.UTILITY_SMITHING:
                return "text.backpack_side_gui.sort.mod";
            default:
                return "text.backpack_side_gui.sort.unknown";
        }
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= ((double) x) && mouseX < ((double) (x + w)) && mouseY >= ((double) y) && mouseY < ((double) (y + h));
    }

    private static void renderPanelButtons(GuiGraphics graphics, BackpackPanelLayout.PanelRect rect) {
        int y = getButtonY(rect);
        int moveX = rect.x();
        int toggleX = rect.x() + 14 + 3;
        PanelRenderer.renderIconButton(graphics, MOVE_ICON, moveX, y, false);
        PanelRenderer.renderIconButton(graphics, ((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue() ? SHOW_ICON : HIDE_ICON, toggleX, y, false);
        if (!((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
            int[] visibleTypes = getVisibleUtilityTypes();
            for (int i = 0; i < visibleTypes.length; i++) {
                int type = visibleTypes[i];
                int x = getUtilityButtonX(rect, i);
                PanelRenderer.renderIconButton(graphics, getUtilityIcon(type), x, y, activeUtilityPanel == type);
            }
        }
    }

    private static int getUtilityButtonX(BackpackPanelLayout.PanelRect rect, int visibleIndex) {
        return rect.x() + (17 * (2 + visibleIndex));
    }

    private static boolean isUtilityTypeVisible(int type) {
        switch (type) {
            case 0:
                return hasCraftingUpgrade;
            case 1:
                return hasFurnaceUpgrade;
            case ServerBackpackAccess.UTILITY_ANVIL:
                return hasAnvilUpgrade;
            case ServerBackpackAccess.UTILITY_SMITHING:
                return hasSmithingUpgrade;
            default:
                return false;
        }
    }

    private static void renderUtilityPanel(GuiGraphics graphics, Minecraft mc, BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        MutableComponent mutableComponentEmpty;
        if (activeUtilityPanel < 0 || !isUtilityTypeVisible(activeUtilityPanel)) {
            return;
        }
        int x = rect.x();
        int y = getUtilityPanelY(rect);
        graphics.fill(x - 4, y - 4, x + 162 + 4, y + UTILITY_PANEL_HEIGHT + 4, -871362544);
        graphics.fill(x - 4, y - 4, x + 162 + 4, y - 3, -11184811);
        switch (activeUtilityPanel) {
            case 0:
                mutableComponentEmpty = Component.translatable("text.backpack_side_gui.utility.crafting");
                break;
            case 1:
                mutableComponentEmpty = Component.translatable("text.backpack_side_gui.utility.furnace");
                break;
            case ServerBackpackAccess.UTILITY_ANVIL:
                mutableComponentEmpty = Component.translatable("text.backpack_side_gui.utility.anvil");
                break;
            case ServerBackpackAccess.UTILITY_SMITHING:
                mutableComponentEmpty = Component.translatable("text.backpack_side_gui.utility.smithing");
                break;
            default:
                mutableComponentEmpty = Component.empty();
                break;
        }
        graphics.drawString(mc.font, mutableComponentEmpty, x + 4, y + 3, 16777215, true);
        int sy = y + 17;
        if (activeUtilityPanel == 0) {
            int sx = x + 8;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    renderUtilitySlotWithItem(graphics, mc, sx + (col * SLOT_SIZE), sy + (row * SLOT_SIZE), (row * 3) + col, mouseX, mouseY);
                }
            }
            graphics.drawString(mc.font, "闂?", x + 70, sy + 20, 16777215, true);
            renderUtilitySlotWithItem(graphics, mc, x + 93, sy + SLOT_SIZE, COLUMNS, mouseX, mouseY);
            return;
        }
        if (activeUtilityPanel == 1) {
            renderUtilitySlotWithItem(graphics, mc, x + 22, sy + 2, 0, mouseX, mouseY);
            renderUtilitySlotWithItem(graphics, mc, x + 22, sy + 30, 1, mouseX, mouseY);
            PanelRenderer.renderFurnaceBars(graphics, x, sy, furnaceLitTime, furnaceLitDuration, furnaceCookProgress, furnaceCookTotal);
            graphics.drawString(mc.font, "闂?", x + 58, sy + SLOT_SIZE, 16777215, true);
            renderUtilitySlotWithItem(graphics, mc, x + 88, sy + 16, 2, mouseX, mouseY);
            return;
        }
        if (activeUtilityPanel == 2) {
            PanelRenderer.renderAnvilNameBox(graphics, mc, rect.x() + SCROLLBAR_WIDTH, getUtilityPanelY(rect) + 15, anvilNameFocused, anvilName);
            renderUtilitySlotWithItem(graphics, mc, x + 15, sy + 20, 0, mouseX, mouseY);
            graphics.drawString(mc.font, "+", x + 40, sy + 25, 16777215, true);
            renderUtilitySlotWithItem(graphics, mc, x + 55, sy + 20, 1, mouseX, mouseY);
            graphics.drawString(mc.font, "=", x + 82, sy + 25, 16777215, true);
            renderUtilitySlotWithItem(graphics, mc, x + 102, sy + 20, 2, mouseX, mouseY);
            if (anvilCost > 0) {
                boolean tooExpensive = anvilCost >= 40 && (mc.player == null || !mc.player.isCreative());
                boolean notEnoughXp = (tooExpensive || mc.player == null || mc.player.isCreative() || mc.player.experienceLevel >= anvilCost) ? false : true;
                String costText = tooExpensive ? "闂備礁鎼ˇ顐﹀疾濞戞◤娲晲閸ヮ亜小濡炪倖甯掔€氼剟宕ｆ繝鍥ㄧ厱閻忕偛澧界粻鎾绘煙?" : notEnoughXp ? "缂傚倸鍊搁崐椋庣矆娴ｇ儤宕叉慨妞诲亾闁诡喗婢橀埢搴ㄥ箛椤旇鐏冮柣搴＄畭閸庨亶鎮ч崱娑樼獥?" : "缂傚倸鍊烽悞锔剧矙閹次诲洦娼忛…鎴烆啍闂佹悶鍎滈埀顒€危閸儲鐓曟い鎰靛墰瀹€娑㈡煕? " + anvilCost;
                int costColor = tooExpensive ? 16733525 : notEnoughXp ? 16751001 : 8454016;
                graphics.drawString(mc.font, costText, x + 80, sy + 42, costColor, true);
                return;
            }
            return;
        }
        if (activeUtilityPanel == 3) {
            renderUtilitySlotWithItem(graphics, mc, x + 7, sy + 16, 0, mouseX, mouseY);
            renderUtilitySlotWithItem(graphics, mc, x + 31, sy + 16, 1, mouseX, mouseY);
            renderUtilitySlotWithItem(graphics, mc, x + 55, sy + 16, 2, mouseX, mouseY);
            graphics.drawString(mc.font, "闂?", x + 82, sy + 21, 16777215, true);
            renderUtilitySlotWithItem(graphics, mc, x + 106, sy + 16, 3, mouseX, mouseY);
        }
    }


    private static boolean anvilNameBoxContains(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        if (activeUtilityPanel != 2) {
            return false;
        }
        int x = rect.x() + SCROLLBAR_WIDTH;
        int y = getUtilityPanelY(rect) + 15;
        return contains(mouseX, mouseY, x, y, 92, 12);
    }


    private static void renderUtilitySlotWithItem(GuiGraphics graphics, Minecraft mc, int x, int y, int slot, double mouseX, double mouseY) {
        boolean hovered = contains(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);
        boolean dragSelected = draggingUtilityStack && dragUtilityType == activeUtilityPanel && dragUtilitySlots.contains(Integer.valueOf(slot));
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, dragSelected ? -10053172 : hovered ? -8947849 : -12961222);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, -14671840);
        ItemStack previewStack = buildUtilityDragPreview(mc).get(Integer.valueOf(slot));
        if (previewStack != null && !previewStack.isEmpty()) {
            renderItemWithLargeCount(graphics, mc, previewStack, x + 1, y + 1);
            return;
        }
        if (slot >= 0 && slot < utilityItems.size()) {
            ItemStack stack = utilityItems.get(slot);
            if (!stack.isEmpty()) {
                renderItemWithLargeCount(graphics, mc, stack, x + 1, y + 1);
            }
        }
    }


    private static int[] getVisibleUtilityTypes() {
        int[] tmp = new int[4];
        int size = 0;
        if (hasCraftingUpgrade) {
            size = 0 + 1;
            tmp[0] = 0;
        }
        if (hasFurnaceUpgrade) {
            int i = size;
            size++;
            tmp[i] = 1;
        }
        if (hasAnvilUpgrade) {
            int i2 = size;
            size++;
            tmp[i2] = 2;
        }
        if (hasSmithingUpgrade) {
            int i3 = size;
            size++;
            tmp[i3] = 3;
        }
        int[] result = new int[size];
        System.arraycopy(tmp, 0, result, 0, size);
        return result;
    }

    private static ResourceLocation getUtilityIcon(int type) {
        switch (type) {
        }
        return UTILITY_CRAFTING_ICON;
    }

    private static Component getUtilityTooltip(int type) {
        switch (type) {
            case 0:
                return Component.translatable("text.backpack_side_gui.tooltip.utility.crafting");
            case 1:
                return Component.translatable("text.backpack_side_gui.tooltip.utility.furnace");
            case ServerBackpackAccess.UTILITY_ANVIL:
                return Component.translatable("text.backpack_side_gui.tooltip.utility.anvil");
            case ServerBackpackAccess.UTILITY_SMITHING:
                return Component.translatable("text.backpack_side_gui.tooltip.utility.smithing");
            default:
                return Component.empty();
        }
    }

    private static boolean moveButtonContains(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        int y = getButtonY(rect);
        return mouseX >= ((double) rect.x()) && mouseX < ((double) (rect.x() + 14)) && mouseY >= ((double) y) && mouseY < ((double) (y + 14));
    }

    private static boolean showHideButtonContains(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        int x = rect.x() + 14 + 3;
        int y = getButtonY(rect);
        return mouseX >= ((double) x) && mouseX < ((double) (x + 14)) && mouseY >= ((double) y) && mouseY < ((double) (y + 14));
    }

    private static int getUtilityButtonType(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        if (((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
            return -1;
        }
        int y = getButtonY(rect);
        int[] visibleTypes = getVisibleUtilityTypes();
        for (int i = 0; i < visibleTypes.length; i++) {
            int x = getUtilityButtonX(rect, i);
            if (mouseX >= x && mouseX < x + 14 && mouseY >= y && mouseY < y + 14) {
                return visibleTypes[i];
            }
        }
        return -1;
    }

    private static int getUtilitySlotAt(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        if (activeUtilityPanel < 0 || !isUtilityTypeVisible(activeUtilityPanel)) {
            return -1;
        }
        int x = rect.x();
        int sy = getUtilityPanelY(rect) + 17;
        if (activeUtilityPanel == 0) {
            int sx = x + 8;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (contains(mouseX, mouseY, sx + (col * SLOT_SIZE), sy + (row * SLOT_SIZE), SLOT_SIZE, SLOT_SIZE)) {
                        return (row * 3) + col;
                    }
                }
            }
            if (contains(mouseX, mouseY, x + 93, sy + SLOT_SIZE, SLOT_SIZE, SLOT_SIZE)) {
                return COLUMNS;
            }
            return -1;
        }
        if (activeUtilityPanel == 1) {
            if (contains(mouseX, mouseY, x + 22, sy + 2, SLOT_SIZE, SLOT_SIZE)) {
                return 0;
            }
            if (contains(mouseX, mouseY, x + 22, sy + 30, SLOT_SIZE, SLOT_SIZE)) {
                return 1;
            }
            return contains(mouseX, mouseY, x + 88, sy + 16, SLOT_SIZE, SLOT_SIZE) ? 2 : -1;
        }
        if (activeUtilityPanel == 2) {
            if (contains(mouseX, mouseY, x + 15, sy + 16, SLOT_SIZE, SLOT_SIZE)) {
                return 0;
            }
            if (contains(mouseX, mouseY, x + 55, sy + 16, SLOT_SIZE, SLOT_SIZE)) {
                return 1;
            }
            return contains(mouseX, mouseY, x + 102, sy + 16, SLOT_SIZE, SLOT_SIZE) ? 2 : -1;
        }
        if (activeUtilityPanel == 3) {
            if (contains(mouseX, mouseY, x + 7, sy + 16, SLOT_SIZE, SLOT_SIZE)) {
                return 0;
            }
            if (contains(mouseX, mouseY, x + 31, sy + 16, SLOT_SIZE, SLOT_SIZE)) {
                return 1;
            }
            if (contains(mouseX, mouseY, x + 55, sy + 16, SLOT_SIZE, SLOT_SIZE)) {
                return 2;
            }
            return contains(mouseX, mouseY, x + 106, sy + 16, SLOT_SIZE, SLOT_SIZE) ? 3 : -1;
        }
        return -1;
    }

    private static boolean utilityPanelContains(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        if (activeUtilityPanel < 0 || !isUtilityTypeVisible(activeUtilityPanel)) {
            return false;
        }
        int x = rect.x() - 4;
        int y = getUtilityPanelY(rect) - 4;
        return contains(mouseX, mouseY, x, y, 170, 74);
    }

    private static int getUtilityPanelY(BackpackPanelLayout.PanelRect rect) {
        return getButtonY(rect) + 14 + 5;
    }

    private static int getButtonY(BackpackPanelLayout.PanelRect rect) {
        return rect.y() + (rect.visibleRows() * SLOT_SIZE) + 5;
    }

    private static void setPanelHidden(boolean hidden) {
        BackpackSideGuiConfig.PANEL_HIDDEN.set(Boolean.valueOf(hidden));
        if (hidden) {
            activeUtilityPanel = -1;
            utilityState.activePanel = -1;
        }
        saveClientConfig();
        cancelDragState();
        scrollbarDragging = false;
    }

    private static void setPanelDragOffsets(int xOffset, int yOffset) {
        panelDragXOffset = Math.max(-1000, Math.min(1000, xOffset));
        panelDragYOffset = Math.max(-1000, Math.min(1000, yOffset));
    }

    private static void commitPanelDragOffsets() {
        BackpackSideGuiConfig.PANEL_X_OFFSET.set(Integer.valueOf(Math.max(-1000, Math.min(1000, panelDragXOffset))));
        BackpackSideGuiConfig.PANEL_Y_OFFSET.set(Integer.valueOf(Math.max(-1000, Math.min(1000, panelDragYOffset))));
    }

    private static int getEffectiveXOffset() {
        return movingPanel ? panelDragXOffset : ((Integer) BackpackSideGuiConfig.PANEL_X_OFFSET.get()).intValue();
    }

    private static int getEffectiveYOffset() {
        return movingPanel ? panelDragYOffset : ((Integer) BackpackSideGuiConfig.PANEL_Y_OFFSET.get()).intValue();
    }

    private static void saveClientConfig() {
        try {
            BackpackSideGuiConfig.CLIENT_SPEC.save();
        } catch (Throwable th) {
        }
    }

    private static void restoreCarriedRenderOverride(Minecraft mc) {
        if (!carriedRenderOverrideActive) {
            return;
        }
        if (mc != null && mc.player != null && mc.player.containerMenu != null) {
            mc.player.containerMenu.setCarried(carriedRenderOriginal == null ? ItemStack.EMPTY : carriedRenderOriginal.copy());
        }
        carriedRenderOverrideActive = false;
        carriedRenderOriginal = ItemStack.EMPTY;
    }

    private static ItemStack getCarriedStackForRender(Minecraft mc) {
        if (mc == null || mc.player == null || mc.player.containerMenu == null) return ItemStack.EMPTY;
        ItemStack carried = mc.player.containerMenu.getCarried();
        return carried == null ? ItemStack.EMPTY : carried.copy();
    }

    private static ItemStack getHoveredPanelStackAtMouse(Minecraft mc, AbstractContainerScreen<?> screen) {
        if (mc == null || screen == null || ((Boolean) BackpackSideGuiConfig.PANEL_HIDDEN.get()).booleanValue()) {
            return ItemStack.EMPTY;
        }
        double mouseX = (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()) / mc.getWindow().getScreenWidth();
        double mouseY = (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()) / mc.getWindow().getScreenHeight();
        BackpackPanelLayout.PanelRect rect = getBackpackPanelLayoutPanelRect(screen.width, screen.height);
        int slot = getHoveredSlot(rect, mouseX, mouseY);
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(slot);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private static int getAssignedDragCount(Map<Integer, ItemStack> dragPreview) {
        int assigned = 0;
        if (dragPreview == null || dragPreview.isEmpty()) {
            return 0;
        }
        for (Map.Entry<Integer, ItemStack> entry : dragPreview.entrySet()) {
            int slot = entry.getKey().intValue();
            ItemStack preview = entry.getValue();
            if (preview != null && !preview.isEmpty()) {
                ItemStack base = (slot < 0 || slot >= items.size()) ? ItemStack.EMPTY : items.get(slot);
                int baseCount = base.isEmpty() ? 0 : base.getCount();
                int add = preview.getCount() - baseCount;
                if (add > 0) {
                    assigned += add;
                }
            }
        }
        return assigned;
    }

    private static Map<Integer, ItemStack> buildDragPreview(Minecraft mc) {
        int slot;
        Map<Integer, ItemStack> preview = new HashMap<>();
        if (!draggingStack || dragSlots.isEmpty() || mc.player == null || mc.player.containerMenu == null) {
            return preview;
        }
        ItemStack carried = mc.player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            return preview;
        }
        List<Integer> validSlots = new ArrayList<>();
        Map<Integer, Integer> baseCounts = new HashMap<>();
        for (Integer slotObj : dragSlots) {
            if (slotObj != null && (slot = slotObj.intValue()) >= 0 && slot < slotCount) {
                ItemStack current = slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
                if (canClientPreviewPlace(current, carried)) {
                    validSlots.add(Integer.valueOf(slot));
                    baseCounts.put(Integer.valueOf(slot), Integer.valueOf(current.isEmpty() ? 0 : current.getCount()));
                }
            }
        }
        if (validSlots.isEmpty()) {
            return preview;
        }
        Map<Integer, Integer> addCounts = new HashMap<>();
        int remaining = carried.getCount();
        if (dragButton == 1) {
            Iterator<Integer> it = validSlots.iterator();
            while (it.hasNext()) {
                int slot2 = it.next().intValue();
                if (remaining <= 0) {
                    break;
                }
                int base = baseCounts.getOrDefault(Integer.valueOf(slot2), 0).intValue();
                int limit = getClientPreviewLimit(slot2, carried);
                if (base + addCounts.getOrDefault(Integer.valueOf(slot2), 0).intValue() < limit) {
                    addCounts.put(Integer.valueOf(slot2), Integer.valueOf(addCounts.getOrDefault(Integer.valueOf(slot2), 0).intValue() + 1));
                    remaining--;
                }
            }
        } else {
            boolean movedAny = true;
            while (remaining > 0 && movedAny) {
                movedAny = false;
                Iterator<Integer> it2 = validSlots.iterator();
                while (it2.hasNext()) {
                    int slot3 = it2.next().intValue();
                    if (remaining <= 0) {
                        break;
                    }
                    int base2 = baseCounts.getOrDefault(Integer.valueOf(slot3), 0).intValue();
                    int add = addCounts.getOrDefault(Integer.valueOf(slot3), 0).intValue();
                    int limit2 = getClientPreviewLimit(slot3, carried);
                    if (base2 + add < limit2) {
                        addCounts.put(Integer.valueOf(slot3), Integer.valueOf(add + 1));
                        remaining--;
                        movedAny = true;
                    }
                }
            }
        }
        for (Map.Entry<Integer, Integer> entry : addCounts.entrySet()) {
            int slot4 = entry.getKey().intValue();
            int add2 = entry.getValue().intValue();
            if (add2 > 0) {
                ItemStack current2 = slot4 < items.size() ? items.get(slot4) : ItemStack.EMPTY;
                ItemStack shown = current2.isEmpty() ? carried.copy() : current2.copy();
                shown.setCount((current2.isEmpty() ? 0 : current2.getCount()) + add2);
                preview.put(Integer.valueOf(slot4), shown);
            }
        }
        return preview;
    }

    private static boolean canClientPreviewPlace(ItemStack current, ItemStack carried) {
        if (carried.isEmpty()) {
            return false;
        }
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, carried) && current.getCount() < getClientPreviewLimit(-1, carried);
    }

    private static int getClientPreviewLimit(int slot, ItemStack carried) {
        ItemStack current = (slot < 0 || slot >= items.size()) ? ItemStack.EMPTY : items.get(slot);
        if (isLargeStackMode(carried, current)) {
            return Math.max(LARGE_STACK_PREVIEW_LIMIT, (current.isEmpty() ? 0 : current.getCount()) + carried.getCount());
        }
        if (!current.isEmpty()) {
            return Math.min(current.getMaxStackSize(), carried.getMaxStackSize());
        }
        return carried.getMaxStackSize();
    }

    private static boolean isLargeStackMode(ItemStack carried, ItemStack current) {
        if (carried != null && !carried.isEmpty() && carried.getCount() > carried.getMaxStackSize()) {
            return true;
        }
        if (current != null && !current.isEmpty() && current.getCount() > current.getMaxStackSize()) {
            return true;
        }
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty() && stack.getCount() > stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private static Map<Integer, ItemStack> buildUtilityDragPreview(Minecraft mc) {
        Map<Integer, ItemStack> preview = new HashMap<>();
        if (!draggingUtilityStack || dragUtilitySlots.isEmpty() || mc.player == null || mc.player.containerMenu == null) {
            return preview;
        }
        ItemStack carried = mc.player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            return preview;
        }
        List<Integer> validSlots = new ArrayList<>();
        Map<Integer, Integer> baseCounts = new HashMap<>();
        for (Integer slotObj : dragUtilitySlots) {
            if (slotObj != null) {
                int slot = slotObj.intValue();
                if (isUtilityDragInputSlot(dragUtilityType, slot)) {
                    ItemStack current = (slot < 0 || slot >= utilityItems.size()) ? ItemStack.EMPTY : utilityItems.get(slot);
                    if (canClientPreviewPlace(current, carried)) {
                        validSlots.add(Integer.valueOf(slot));
                        baseCounts.put(Integer.valueOf(slot), Integer.valueOf(current.isEmpty() ? 0 : current.getCount()));
                    }
                }
            }
        }
        if (validSlots.isEmpty()) {
            return preview;
        }
        Map<Integer, Integer> addCounts = new HashMap<>();
        int remaining = carried.getCount();
        if (dragUtilityButton == 1) {
            Iterator<Integer> it = validSlots.iterator();
            while (it.hasNext()) {
                int slot2 = it.next().intValue();
                if (remaining <= 0) {
                    break;
                }
                int base = baseCounts.getOrDefault(Integer.valueOf(slot2), 0).intValue();
                int add = addCounts.getOrDefault(Integer.valueOf(slot2), 0).intValue();
                int limit = getClientPreviewLimit(-1, carried);
                if (base + add < limit) {
                    addCounts.put(Integer.valueOf(slot2), Integer.valueOf(add + 1));
                    remaining--;
                }
            }
        } else {
            boolean movedAny = true;
            while (remaining > 0 && movedAny) {
                movedAny = false;
                Iterator<Integer> it2 = validSlots.iterator();
                while (it2.hasNext()) {
                    int slot3 = it2.next().intValue();
                    if (remaining <= 0) {
                        break;
                    }
                    int base2 = baseCounts.getOrDefault(Integer.valueOf(slot3), 0).intValue();
                    int add2 = addCounts.getOrDefault(Integer.valueOf(slot3), 0).intValue();
                    int limit2 = getClientPreviewLimit(-1, carried);
                    if (base2 + add2 < limit2) {
                        addCounts.put(Integer.valueOf(slot3), Integer.valueOf(add2 + 1));
                        remaining--;
                        movedAny = true;
                    }
                }
            }
        }
        for (Map.Entry<Integer, Integer> entry : addCounts.entrySet()) {
            int slot4 = entry.getKey().intValue();
            int add3 = entry.getValue().intValue();
            if (add3 > 0) {
                ItemStack current2 = (slot4 < 0 || slot4 >= utilityItems.size()) ? ItemStack.EMPTY : utilityItems.get(slot4);
                ItemStack shown = current2.isEmpty() ? carried.copy() : current2.copy();
                shown.setCount((current2.isEmpty() ? 0 : current2.getCount()) + add3);
                preview.put(Integer.valueOf(slot4), shown);
            }
        }
        return preview;
    }

    private static int getAssignedUtilityDragCount(Map<Integer, ItemStack> dragPreview) {
        int assigned = 0;
        if (dragPreview == null || dragPreview.isEmpty()) {
            return 0;
        }
        for (Map.Entry<Integer, ItemStack> entry : dragPreview.entrySet()) {
            int slot = entry.getKey().intValue();
            ItemStack preview = entry.getValue();
            if (preview != null && !preview.isEmpty()) {
                ItemStack base = (slot < 0 || slot >= utilityItems.size()) ? ItemStack.EMPTY : utilityItems.get(slot);
                int baseCount = base.isEmpty() ? 0 : base.getCount();
                int add = preview.getCount() - baseCount;
                if (add > 0) {
                    assigned += add;
                }
            }
        }
        return assigned;
    }

    private static ItemStack getClientCarriedSnapshot(Minecraft mc) {
        if (mc == null || mc.player == null || mc.player.containerMenu == null) {
            return ItemStack.EMPTY;
        }
        ItemStack carried = mc.player.containerMenu.getCarried();
        return carried == null ? ItemStack.EMPTY : carried.copy();
    }


    private static int getHoveredSlot(BackpackPanelLayout.PanelRect rect, double mouseX, double mouseY) {
        if (!rect.slotsContains(mouseX, mouseY)) {
            return -1;
        }
        int col = (((int) mouseX) - rect.x()) / SLOT_SIZE;
        int row = (((int) mouseY) - rect.y()) / SLOT_SIZE;
        if (col < 0 || col >= COLUMNS || row < 0 || row >= rect.visibleRows()) {
            return -1;
        }
        int visibleIndex = ((scrollRow + row) * COLUMNS) + col;
        List<Integer> visibleSlots = getVisibleSlots();
        if (visibleIndex < 0 || visibleIndex >= visibleSlots.size()) {
            return -1;
        }
        return visibleSlots.get(visibleIndex).intValue();
    }

    private static BackpackPanelLayout.PanelRect getBackpackPanelLayoutPanelRect(int screenWidth, int screenHeight) {
        return BackpackPanelLayout.calculate(screenWidth, screenHeight, getTotalRows(),
                ((Integer) BackpackSideGuiConfig.VISIBLE_ROWS.get()).intValue(),
                ((Boolean) BackpackSideGuiConfig.PANEL_RIGHT_SIDE.get()).booleanValue(),
                getEffectiveXOffset(), getEffectiveYOffset());
    }

    private static void updateScrollFromMouse(BackpackPanelLayout.PanelRect rect, double mouseY) {
        int maxScroll = getMaxScrollRows();
        if (maxScroll <= 0) {
            scrollRow = 0;
            return;
        }
        int trackH = rect.visibleRows() * SLOT_SIZE;
        double ratio = (mouseY - rect.y()) / Math.max(1.0d, trackH);
        scrollRow = (int) Math.round(MthClamp(ratio, 0.0d, 1.0d) * maxScroll);
        clampScroll();
    }

    private static double MthClamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isUtilityDragInputSlot(int utilityType, int slot) {
        switch (utilityType) {
            case 0:
                return slot >= 0 && slot < COLUMNS;
            case 1:
                return slot == 0 || slot == 1;
            case ServerBackpackAccess.UTILITY_ANVIL:
                return slot == 0 || slot == 1;
            case ServerBackpackAccess.UTILITY_SMITHING:
                return slot >= 0 && slot < 3;
            default:
                return false;
        }
    }

    private static void rememberUtilityClick(int utilityType, int slot, int button) {
        lastUtilityClickMs = System.currentTimeMillis();
        lastUtilityClickType = utilityType;
        lastUtilityClickSlot = slot;
        lastUtilityClickButton = button;
    }

    private static void cancelUtilityDragState() {
        draggingUtilityStack = false;
        dragUtilityType = -1;
        dragUtilityButton = 0;
        dragUtilitySlots.clear();
        utilityDragMovedAcrossSlots = false;
    }

    private static void rememberClick(int slot, int button) {
        lastClickMs = System.currentTimeMillis();
        lastClickSlot = slot;
        lastClickButton = button;
    }

    private static void cancelDragState() {
        restoreCarriedRenderOverride(Minecraft.getInstance());
        draggingStack = false;
        dragButton = 0;
        dragSlots.clear();
        dragMovedAcrossSlots = false;
        cancelUtilityDragState();
    }

    private static void clampScroll() {
        int max = getMaxScrollRows();
        if (scrollRow < 0) {
            scrollRow = 0;
        }
        if (scrollRow > max) {
            scrollRow = max;
        }
    }

    private static List<Integer> getVisibleSlots() {
        List<Integer> result = new ArrayList<>();
        String filter = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < slotCount; i++) {
            if (filter.isEmpty() || matchesSearch(i, filter)) {
                result.add(Integer.valueOf(i));
            }
        }
        return result;
    }

    private static boolean matchesSearch(int slot, String filter) {
        ItemStack stack;
        if (slot < 0 || slot >= items.size() || (stack = items.get(slot)) == null || stack.isEmpty()) {
            return false;
        }
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (name.contains(filter)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && id.toString().toLowerCase(Locale.ROOT).contains(filter)) {
            return true;
        }
        return stack.getDescriptionId().toLowerCase(Locale.ROOT).contains(filter);
    }

    private static int getTotalRows() {
        return (int) Math.ceil(getVisibleSlots().size() / 9.0d);
    }

    private static int getMaxScrollRows() {
        int visibleRowsSetting = Math.max(2, Math.min(12, ((Integer) BackpackSideGuiConfig.VISIBLE_ROWS.get()).intValue()));
        return Math.max(0, getTotalRows() - visibleRowsSetting);
    }

    private static int getHoveredMenuSlot(AbstractContainerScreen<?> screen) {
        return -1;
    }

    private static boolean isBackpackScreen(Screen screen) {
        return screen != null && screen.getClass().getName().equals("net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen");
    }

}

