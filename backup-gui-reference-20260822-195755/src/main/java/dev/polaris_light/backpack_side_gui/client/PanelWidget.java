package dev.polaris_light.backpack_side_gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A small, state-free view object used by the side panel widget tree. */
abstract class PanelWidget {
    private final String id;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    PanelWidget(String id, int x, int y, int width, int height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    final String id() { return id; }

    final boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    final int x() { return x; }
    final int y() { return y; }

    void render(GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
    }

    Component tooltip(Minecraft minecraft) {
        return null;
    }

    /** Generic leaf used for controls whose painting is supplied by the view. */
    static final class Surface extends PanelWidget {
        interface Painter {
            void paint(GuiGraphics graphics, Minecraft minecraft, Surface surface,
                    double mouseX, double mouseY);
        }

        private final Painter painter;

        Surface(String id, int x, int y, int width, int height, Painter painter) {
            super(id, x, y, width, height);
            this.painter = painter;
        }

        @Override
        void render(GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
            painter.paint(graphics, minecraft, this, mouseX, mouseY);
        }
    }

    static final class Label extends PanelWidget {
        private final Component text;
        private final int color;
        private final boolean shadow;

        Label(String id, int x, int y, Component text, int color, boolean shadow) {
            super(id, x, y, 0, 0);
            this.text = text;
            this.color = color;
            this.shadow = shadow;
        }

        @Override
        void render(GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
            graphics.drawString(minecraft.font, text, x(), y(), color, shadow);
        }
    }

    static final class Icon extends PanelWidget {
        private final ResourceLocation texture;
        private final boolean selected;

        Icon(String id, ResourceLocation texture, int x, int y, boolean selected) {
            super(id, x, y, 14, 14);
            this.texture = texture;
            this.selected = selected;
        }

        @Override
        void render(GuiGraphics graphics, Minecraft minecraft, double mouseX, double mouseY) {
            PanelRenderer.renderIconButton(graphics, texture, x(), y(), selected);
        }
    }

    static final class Slot extends PanelWidget {
        private final Minecraft minecraft;
        private final ItemStack stack;
        private final boolean hovered;
        private final boolean selected;

        Slot(Minecraft minecraft, int x, int y, ItemStack stack, boolean hovered, boolean selected) {
            super("slot", x, y, 18, 18);
            this.minecraft = minecraft;
            this.stack = stack == null ? ItemStack.EMPTY : stack;
            this.hovered = hovered;
            this.selected = selected;
        }

        @Override
        void render(GuiGraphics graphics, Minecraft ignored, double mouseX, double mouseY) {
            graphics.fill(x(), y(), x() + 17, y() + 17,
                    selected ? -10053172 : hovered ? -8947849 : -12961222);
            graphics.fill(x() + 1, y() + 1, x() + 16, y() + 16, -14671840);
            if (!stack.isEmpty()) {
                PanelRenderer.renderItemWithLargeCount(graphics, minecraft, stack, x() + 1, y() + 1);
            }
        }
    }
}
