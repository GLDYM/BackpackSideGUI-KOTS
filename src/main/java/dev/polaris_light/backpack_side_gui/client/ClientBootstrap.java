package dev.polaris_light.backpack_side_gui.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onMousePressedPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onMouseDraggedPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onMouseReleasedPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onMouseScrolledPre);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(SideBackpackClient::onCharacterTyped);
    }
}
