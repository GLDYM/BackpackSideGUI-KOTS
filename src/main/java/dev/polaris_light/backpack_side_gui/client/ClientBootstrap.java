package dev.polaris_light.backpack_side_gui.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientBootstrap::registerKeys);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onMousePressedPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onMouseReleasedPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onMouseDraggedPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onMouseScrolledPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onKeyPressedPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onCharacterTypedPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onScreenClosed);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SideBackpackClient.QUICK_TRANSFER_KEY);
    }
}
