package dev.polaris_light.backpack_side_gui.server.action;

/** Normalized semantic actions used by inventory handlers. */
public enum SlotAction {
    PICKUP, QUICK_MOVE, THROW, CLONE, DRAG, PLACE;

    public static SlotAction fromClickType(int clickType) {
        return switch (clickType) {
            case 0, 1 -> PICKUP;
            case 2 -> PLACE;
            case 4, 5 -> QUICK_MOVE;
            case 6 -> THROW;
            default -> clickType >= 10 ? PLACE : DRAG;
        };
    }
}
