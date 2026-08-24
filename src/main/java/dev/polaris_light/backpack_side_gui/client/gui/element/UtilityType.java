package dev.polaris_light.backpack_side_gui.client.gui.element;

public enum UtilityType {
    CRAFTING(0, "utility_crafting"),
    FURNACE(1, "utility_furnace"),
    ANVIL(2, "utility_anvil"),
    SMITHING(3, "utility_smithing"),
    STONECUTTER(4, "utility_stonecutter");

    private final int protocolId;
    private final String icon;

    UtilityType(int protocolId, String icon) {
        this.protocolId = protocolId;
        this.icon = icon;
    }

    public int protocolId() {
        return protocolId;
    }

    public String icon() {
        return icon;
    }
}
