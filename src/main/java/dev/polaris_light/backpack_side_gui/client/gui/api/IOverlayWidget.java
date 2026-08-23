package dev.polaris_light.backpack_side_gui.client.gui.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Contract for the complete floating overlay, which owns one or more areas. */
public abstract class IOverlayWidget extends IOverlay {
    public abstract void beginDragging(double mouseX, double mouseY);

    public abstract void render(Screen screen, GuiGraphics graphics, Minecraft minecraft);
}
