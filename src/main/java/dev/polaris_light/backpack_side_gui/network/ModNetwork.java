package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import dev.polaris_light.backpack_side_gui.client.SideBackpackClient;
import java.util.ArrayList;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        var resolved = BackpackResolver.resolve(player);
                        if (resolved.isEmpty()) {
                            PacketDistributor.sendToPlayer(player, new BackpackSyncPayload("", java.util.List.of()),
                                    new CustomPacketPayload[0]);
                        } else
                            resolved.ifPresent(access -> {
                                var stacks = new ArrayList<net.minecraft.world.item.ItemStack>();
                                for (int i = 0; i < access.handler().getSlots(); i++)
                                    stacks.add(access.handler().getStackInSlot(i).copy());
                                PacketDistributor.sendToPlayer(player,
                                        new BackpackSyncPayload(access.stack().getHoverName().getString(), stacks),
                                        new CustomPacketPayload[0]);
                            });
                    }
                }));
        registrar.playToClient(BackpackSyncPayload.TYPE, BackpackSyncPayload.STREAM_CODEC,
                (payload, context) -> context
                        .enqueueWork(() -> SideBackpackClient.receive(payload.title(), payload.items())));
    }

    public static void requestOpen() {
        PacketDistributor.sendToServer(new OpenBackpackPayload(), new CustomPacketPayload[0]);
    }
}
