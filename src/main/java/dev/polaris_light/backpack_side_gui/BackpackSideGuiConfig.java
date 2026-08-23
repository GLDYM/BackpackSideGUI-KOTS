package dev.polaris_light.backpack_side_gui;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BackpackSideGuiConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.IntValue OVERLAY_X;
    public static final ModConfigSpec.IntValue OVERLAY_Y;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("Backpack Side GUI");
        OVERLAY_X = b.defineInRange("overlayX", 94, -10000, 10000);
        OVERLAY_Y = b.defineInRange("overlayY", 0, -10000, 10000);
        b.pop();
        CLIENT_SPEC = b.build();
    }

    private BackpackSideGuiConfig() {
    }
}
