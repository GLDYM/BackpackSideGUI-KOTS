package dev.polaris_light.backpack_side_gui.client.gui.api;

/** Common visibility contract for overlay controls and overlay areas. */
public abstract class IOverlayElement {
    public boolean visible;

    public abstract void setVisible(boolean visible);

    public abstract boolean isVisible();

    public abstract void toggleVisible();
}
