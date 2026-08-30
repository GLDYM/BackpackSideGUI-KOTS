package dev.polaris_light.backpack_side_gui.server.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SlotActionTest {
    @Test void mapsVanillaClickCodes() {
        assertEquals(SlotAction.PICKUP, SlotAction.fromClickType(0));
        assertEquals(SlotAction.QUICK_MOVE, SlotAction.fromClickType(4));
        assertEquals(SlotAction.PICKUP_ALL, SlotAction.fromClickType(6));
        assertEquals(SlotAction.PLACE, SlotAction.fromClickType(10));
    }
}
