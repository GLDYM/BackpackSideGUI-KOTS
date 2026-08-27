package dev.polaris_light.backpack_side_gui;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BackpackSideGuiConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.IntValue OVERLAY_X;
    public static final ModConfigSpec.IntValue OVERLAY_Y;
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("Backpack Side GUI");
        OVERLAY_X = builder.defineInRange("overlayX", 94, -10000, 10000);
        OVERLAY_Y = builder.defineInRange("overlayY", 0, -10000, 10000);
        builder.pop();
        CLIENT_SPEC = builder.build();
    }

    private BackpackSideGuiConfig() {
    }
}
