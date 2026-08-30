package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.client.SideBackpackClient;
import dev.polaris_light.backpack_side_gui.network.payload.*;

/** Client-only payload dispatch boundary; keeps ModNetwork side-neutral. */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {}
    public static void backpack(BackpackSyncPayload p) { SideBackpackClient.receive(p.title(), p.items(), p.slotLimits()); }
    public static void flags(UtilityFlagsPayload p) { SideBackpackClient.receiveUtilityFlags(p); }
    public static void smithing(SmithingSyncPayload p) { SideBackpackClient.receiveSmithing(p); }
    public static void crafting(CraftingSyncPayload p) { SideBackpackClient.receiveCrafting(p); }
    public static void anvil(AnvilSyncPayload p) { SideBackpackClient.receiveAnvil(p); }
    public static void furnace(FurnaceSyncPayload p) { SideBackpackClient.receiveFurnace(p); }
    public static void stonecutter(StonecutterSyncPayload p) { SideBackpackClient.receiveStonecutter(p); }
    public static void carried(BackpackCarriedPayload p) { SideBackpackClient.receiveCarried(p.carried()); }
    public static void availability(BackpackAvailabilityPayload p) { SideBackpackClient.receiveBackpackAvailability(p); }
}
