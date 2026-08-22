package dev.polaris_light.backpack_side_gui.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Defines which underlying Minecraft screens may receive the overlay. */
final class BackpackOverlayScreenPolicy {
    private static final String SOPHISTICATED_BACKPACK_SCREEN =
            "net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen";

    private BackpackOverlayScreenPolicy() {}

    static boolean allows(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) return false;
        return !SOPHISTICATED_BACKPACK_SCREEN.equals(screen.getClass().getName());
    }
}
