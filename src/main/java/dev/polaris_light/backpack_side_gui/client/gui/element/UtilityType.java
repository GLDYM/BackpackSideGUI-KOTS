package dev.polaris_light.backpack_side_gui.client.gui.element;

public enum UtilityType {
    CRAFTING(0, "utility_crafting", "text.backpack_side_gui.tooltip.utility.crafting"),
    FURNACE(1, "utility_furnace", "text.backpack_side_gui.tooltip.utility.furnace"),
    ANVIL(2, "utility_anvil", "text.backpack_side_gui.tooltip.utility.anvil"),
    SMITHING(3, "utility_smithing", "text.backpack_side_gui.tooltip.utility.smithing"),
    STONECUTTER(4, "utility_stonecutter", "text.backpack_side_gui.tooltip.utility.stonecutter");

    private final int protocolId;
    private final String icon;
    private final String tooltipKey;

    UtilityType(int protocolId, String icon, String tooltipKey) {
        this.protocolId = protocolId;
        this.icon = icon;
        this.tooltipKey = tooltipKey;
    }

    public int protocolId() {
        return protocolId;
    }

    public String icon() {
        return icon;
    }

    public String tooltipKey() {
        return tooltipKey;
    }
}
