package dev.polaris_light.backpack_side_gui;

import dev.polaris_light.backpack_side_gui.client.ClientBootstrap;
import dev.polaris_light.backpack_side_gui.network.ModNetwork;
import dev.polaris_light.backpack_side_gui.server.ServerBackpackAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BackpackSideGuiMod.MOD_ID)
public final class BackpackSideGuiMod {
    public static final String MOD_ID = "backpack_side_gui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public BackpackSideGuiMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, BackpackSideGuiConfig.CLIENT_SPEC);
        modBus.addListener(ModNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(ServerBackpackAccess::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(ServerBackpackAccess::onServerTick);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.init(modBus);
        }
    }
}
