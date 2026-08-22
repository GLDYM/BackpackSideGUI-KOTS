package dev.polaris_light.backpack_side_gui.server.record;

import dev.polaris_light.backpack_side_gui.server.ServerBackpackAccess;

public record UpgradeFlags(boolean crafting, boolean furnace, boolean anvil, boolean smithing) {

    public boolean allows(int type) {
        switch (type) {
            case ServerBackpackAccess.UTILITY_CRAFTING:
                return crafting;
            case ServerBackpackAccess.UTILITY_FURNACE:
                return furnace;
            case ServerBackpackAccess.UTILITY_ANVIL:
                return anvil;
            case ServerBackpackAccess.UTILITY_SMITHING:
                return smithing;
            default:
                return false;
        }
    }
}

