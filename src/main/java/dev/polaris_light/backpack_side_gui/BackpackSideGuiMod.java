package dev.polaris_light.backpack_side_gui;

import dev.polaris_light.backpack_side_gui.client.ClientBootstrap;
import dev.polaris_light.backpack_side_gui.network.ModNetwork;
import dev.polaris_light.backpack_side_gui.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.config.ModConfig;

@Mod(BackpackSideGuiMod.MOD_ID)
public final class BackpackSideGuiMod {
    public static final String MOD_ID = "backpack_side_gui";
    public BackpackSideGuiMod(IEventBus modBus, ModContainer container) {
        ModMenus.register(modBus);
        container.registerConfig(ModConfig.Type.CLIENT, BackpackSideGuiConfig.CLIENT_SPEC);
        modBus.addListener(ModNetwork::registerPayloads);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.init(modBus);
        }
    }
}
