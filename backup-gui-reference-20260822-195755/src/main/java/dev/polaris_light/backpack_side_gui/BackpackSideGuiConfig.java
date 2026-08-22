package dev.polaris_light.backpack_side_gui;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BackpackSideGuiConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.IntValue PANEL_X_OFFSET;
    public static final ModConfigSpec.IntValue PANEL_Y_OFFSET;
    public static final ModConfigSpec.BooleanValue PANEL_RIGHT_SIDE;
    public static final ModConfigSpec.BooleanValue PANEL_HIDDEN;
    public static final ModConfigSpec.IntValue VISIBLE_ROWS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("Backpack Side GUI");
        PANEL_RIGHT_SIDE = builder.comment("true = inventory/container right side, false = left side").define("panelRightSide", true);
        PANEL_X_OFFSET = builder.defineInRange("panelXOffset", 4, -1000, 1000);
        PANEL_Y_OFFSET = builder.defineInRange("panelYOffset", 0, -1000, 1000);
        PANEL_HIDDEN = builder.define("panelHidden", false);
        VISIBLE_ROWS = builder.defineInRange("visibleRows", 6, 1, 18);
        builder.pop();
        CLIENT_SPEC = builder.build();
    }

    private BackpackSideGuiConfig() {
    }
}
