package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.client.SideBackpackClient;
import dev.polaris_light.backpack_side_gui.network.c2s.BackpackC2S;
import dev.polaris_light.backpack_side_gui.network.c2s.CraftingC2S;
import dev.polaris_light.backpack_side_gui.network.c2s.SmithingC2S;
import dev.polaris_light.backpack_side_gui.network.c2s.UtilityC2S;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackCarriedPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackDragPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackSlotPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingDragPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.OpenBackpackPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SmithingClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SmithingSyncPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SortPayload;
import dev.polaris_light.backpack_side_gui.network.payload.UtilityFlagsPayload;
import dev.polaris_light.backpack_side_gui.network.payload.UtilityRequestPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> server(context, () -> UtilityC2S.open((ServerPlayer) context.player()))));
        registrar.playToServer(BackpackSlotPayload.TYPE, BackpackSlotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> server(context, () -> BackpackC2S.handleSlot((ServerPlayer) context.player(), payload))));
        registrar.playToServer(BackpackDragPayload.TYPE, BackpackDragPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> server(context, () -> BackpackC2S.handleDrag((ServerPlayer) context.player(), payload))));
        registrar.playToServer(SortPayload.TYPE, SortPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> server(context, () -> BackpackC2S.handleSort((ServerPlayer) context.player(), payload))));
        registrar.playToServer(CraftingClickPayload.TYPE, CraftingClickPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> server(context, () -> CraftingC2S.handleClick((ServerPlayer) context.player(), payload))));
        registrar.playToServer(CraftingDragPayload.TYPE, CraftingDragPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> server(context, () -> CraftingC2S.handleDrag((ServerPlayer) context.player(), payload))));
        registrar.playToServer(SmithingClickPayload.TYPE, SmithingClickPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> server(context, () -> SmithingC2S.handleClick((ServerPlayer) context.player(), payload))));
        registrar.playToServer(UtilityRequestPayload.TYPE, UtilityRequestPayload.STREAM_CODEC, 
                (payload, context) -> context.enqueueWork(() -> server(context, () -> UtilityC2S.request((ServerPlayer) context.player(), payload.utilityType()))));
                
        registrar.playToClient(BackpackSyncPayload.TYPE, BackpackSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SideBackpackClient.receive(payload.title(), payload.items())));
        registrar.playToClient(UtilityFlagsPayload.TYPE, UtilityFlagsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SideBackpackClient.receiveUtilityFlags(payload)));
        registrar.playToClient(SmithingSyncPayload.TYPE, SmithingSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SideBackpackClient.receiveSmithing(payload)));
        registrar.playToClient(CraftingSyncPayload.TYPE, CraftingSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SideBackpackClient.receiveCrafting(payload)));
        registrar.playToClient(BackpackCarriedPayload.TYPE, BackpackCarriedPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SideBackpackClient.receiveCarried(payload.carried())));
    }

    private static void server(IPayloadContext context, Runnable action) {
        if (context.player() instanceof ServerPlayer)
            action.run();
    }
}
