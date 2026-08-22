package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.client.SideBackpackClient;
import dev.polaris_light.backpack_side_gui.server.ServerBackpackAccess;
import java.util.List;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(RequestPanelPayload.TYPE, RequestPanelPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.syncTo(player);
                }
            });
        });
        registrar.playToServer(ClickSlotPayload.TYPE, ClickSlotPayload.STREAM_CODEC, (payload2, context2) -> {
            context2.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context2.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handlePanelClick(player, payload2.logicalSlot(), payload2.button(), payload2.clientCarried());
                }
            });
        });
        registrar.playToServer(SortPayload.TYPE, SortPayload.STREAM_CODEC, (payload3, context3) -> {
            context3.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context3.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handleSort(player, payload3.sortMode());
                }
            });
        });
        registrar.playToServer(UtilityRequestPayload.TYPE, UtilityRequestPayload.STREAM_CODEC, (payload4, context4) -> {
            context4.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context4.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handleUtilityRequest(player, payload4.utilityType());
                }
            });
        });
        registrar.playToServer(UtilityClickPayload.TYPE, UtilityClickPayload.STREAM_CODEC, (payload5, context5) -> {
            context5.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context5.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handleUtilityClick(player, payload5.utilityType(), payload5.slot(), payload5.button(), payload5.clientCarried());
                }
            });
        });
        registrar.playToServer(UtilityRenamePayload.TYPE, UtilityRenamePayload.STREAM_CODEC, (payload6, context6) -> {
            context6.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context6.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handleUtilityRename(player, payload6.name());
                }
            });
        });
        registrar.playToServer(UtilityDoubleCollectPayload.TYPE, UtilityDoubleCollectPayload.STREAM_CODEC, (payload7, context7) -> {
            context7.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context7.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handleUtilityDoubleCollect(player, payload7.utilityType(), payload7.slot(), payload7.clientCarried());
                }
            });
        });
        registrar.playToServer(UtilityDragDistributePayload.TYPE, UtilityDragDistributePayload.STREAM_CODEC, (payload8, context8) -> {
            context8.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context8.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handleUtilityDragDistribute(player, payload8.utilityType(), payload8.slots(), payload8.button(), payload8.clientCarried());
                }
            });
        });
        registrar.playToServer(QuickTransferPayload.TYPE, QuickTransferPayload.STREAM_CODEC, (payload9, context9) -> {
            context9.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context9.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handleQuickTransfer(player, payload9.side(), payload9.slot());
                }
            });
        });
        registrar.playToServer(DoubleCollectPayload.TYPE, DoubleCollectPayload.STREAM_CODEC, (payload10, context10) -> {
            context10.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context10.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handleDoubleCollect(player, payload10.slot(), payload10.clientCarried());
                }
            });
        });
        registrar.playToServer(DragDistributePayload.TYPE, DragDistributePayload.STREAM_CODEC, (payload11, context11) -> {
            context11.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context11.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.handleDragDistribute(player, payload11.slots(), payload11.button(), payload11.clientCarried());
                }
            });
        });
        registrar.playToServer(JeiPrefetchPayload.TYPE, JeiPrefetchPayload.STREAM_CODEC, (payload12, context12) -> {
            context12.enqueueWork(() -> {
                ServerPlayer serverPlayerPlayer = context12.player() instanceof ServerPlayer p ? p : null;
                if (serverPlayerPlayer instanceof ServerPlayer) {
                    ServerPlayer player = serverPlayerPlayer;
                    ServerBackpackAccess.syncTo(player);
                }
            });
        });
        registrar.playToClient(PanelSyncPayload.TYPE, PanelSyncPayload.STREAM_CODEC, (payload13, context13) -> {
            context13.enqueueWork(() -> {
                SideBackpackClient.receiveSync(payload13);
            });
        });
        registrar.playToClient(JeiRetryTransferPayload.TYPE, JeiRetryTransferPayload.STREAM_CODEC, (payload14, context14) -> {
            context14.enqueueWork(() -> {
                SideBackpackClient.receiveJeiRetryTransfer(payload14);
            });
        });
        registrar.playToClient(UtilitySyncPayload.TYPE, UtilitySyncPayload.STREAM_CODEC, (payload15, context15) -> {
            context15.enqueueWork(() -> {
                SideBackpackClient.receiveUtilitySync(payload15);
            });
        });
    }

    public static void sendRequest() {
        PacketDistributor.sendToServer(new RequestPanelPayload(), new CustomPacketPayload[0]);
    }

    public static void sendClick(int logicalSlot, int button) {
        PacketDistributor.sendToServer(new ClickSlotPayload(logicalSlot, button, ItemStack.EMPTY), new CustomPacketPayload[0]);
    }

    public static void sendClick(int backpackSlot, int logicalSlot, int button, ItemStack clientCarried) {
        PacketDistributor.sendToServer(new ClickSlotPayload(logicalSlot, button, clientCarried == null ? ItemStack.EMPTY : clientCarried.copy()), new CustomPacketPayload[0]);
    }

    public static void sendSort(int sortMode) {
        PacketDistributor.sendToServer(new SortPayload(sortMode), new CustomPacketPayload[0]);
    }

    public static void sendSort(int backpackSlot, int sortMode) {
        sendSort(sortMode);
    }

    public static void sendUtilityRequest(int utilityType) {
        PacketDistributor.sendToServer(new UtilityRequestPayload(utilityType), new CustomPacketPayload[0]);
    }

    public static void sendUtilityRequest(int backpackSlot, int utilityType) {
        sendUtilityRequest(utilityType);
    }

    public static void sendUtilityClick(int utilityType, int slot, int button) {
        PacketDistributor.sendToServer(new UtilityClickPayload(utilityType, slot, button, ItemStack.EMPTY), new CustomPacketPayload[0]);
    }

    public static void sendUtilityClick(int backpackSlot, int utilityType, int slot, int button, ItemStack clientCarried) {
        PacketDistributor.sendToServer(new UtilityClickPayload(utilityType, slot, button, clientCarried == null ? ItemStack.EMPTY : clientCarried.copy()), new CustomPacketPayload[0]);
    }

    public static void sendUtilityRename(String name) {
        PacketDistributor.sendToServer(new UtilityRenamePayload(name), new CustomPacketPayload[0]);
    }

    public static void sendQuickTransfer(int side, int slot) {
        PacketDistributor.sendToServer(new QuickTransferPayload(side, slot), new CustomPacketPayload[0]);
    }

    public static void sendQuickTransfer(int backpackSlot, int panelSlot, int menuSlot) {
        if (panelSlot < 0) {
            if (menuSlot >= 0) {
                PacketDistributor.sendToServer(new QuickTransferPayload(1, menuSlot), new CustomPacketPayload[0]);
                return;
            }
            return;
        }
        PacketDistributor.sendToServer(new QuickTransferPayload(0, panelSlot), new CustomPacketPayload[0]);
    }

    public static void sendDoubleCollect(int slot) {
        PacketDistributor.sendToServer(new DoubleCollectPayload(slot, ItemStack.EMPTY), new CustomPacketPayload[0]);
    }

    public static void sendDoubleCollect(int backpackSlot, int slot, ItemStack clientCarried) {
        PacketDistributor.sendToServer(new DoubleCollectPayload(slot, clientCarried == null ? ItemStack.EMPTY : clientCarried.copy()), new CustomPacketPayload[0]);
    }

    public static void sendDragDistribute(int backpackSlot, List<Integer> slots, int button, ItemStack clientCarried) {
        PacketDistributor.sendToServer(new DragDistributePayload(slots, button, clientCarried == null ? ItemStack.EMPTY : clientCarried.copy()), new CustomPacketPayload[0]);
    }

    public static void sendUtilityDoubleCollect(int backpackSlot, int utilityType, int slot, ItemStack clientCarried) {
        PacketDistributor.sendToServer(new UtilityDoubleCollectPayload(utilityType, slot, clientCarried == null ? ItemStack.EMPTY : clientCarried.copy()), new CustomPacketPayload[0]);
    }

    public static void sendUtilityDragDistribute(int backpackSlot, int utilityType, List<Integer> slots, int button, ItemStack clientCarried) {
        PacketDistributor.sendToServer(new UtilityDragDistributePayload(utilityType, slots, button, clientCarried == null ? ItemStack.EMPTY : clientCarried.copy()), new CustomPacketPayload[0]);
    }

    public static void sendJeiPrefetch(int backpackSlot, List<List<ItemStack>> ingredientGroups) {
    }

    public static void sendPanelSync(ServerPlayer player, PanelSyncPayload payload) {
        PacketDistributor.sendToPlayer(player, payload, new CustomPacketPayload[0]);
    }

    public static void sendUtilitySync(ServerPlayer player, UtilitySyncPayload payload) {
        PacketDistributor.sendToPlayer(player, payload, new CustomPacketPayload[0]);
    }

    public static void sendJeiRetry(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, JeiRetryTransferPayload.retry(), new CustomPacketPayload[0]);
    }

    public static void sendJeiOpenCrafting(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, JeiRetryTransferPayload.openCrafting(), new CustomPacketPayload[0]);
    }
}
