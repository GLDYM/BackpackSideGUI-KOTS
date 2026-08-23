// package dev.polaris_light.backpack_side_gui.registry;

// import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
// import dev.polaris_light.backpack_side_gui.server.BackpackMenu;
// import net.minecraft.core.registries.Registries;
// import net.minecraft.world.inventory.MenuType;
// import net.neoforged.bus.api.IEventBus;
// import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
// import net.neoforged.neoforge.registries.DeferredHolder;
// import net.neoforged.neoforge.registries.DeferredRegister;

// public final class ModMenus {
//     private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BackpackSideGuiMod.MOD_ID);
//     public static final DeferredHolder<MenuType<?>, MenuType<BackpackMenu>> BACKPACK = MENUS.register("backpack", () -> IMenuTypeExtension.create(BackpackMenu::client));
//     private ModMenus() {}
//     public static void register(IEventBus bus) { MENUS.register(bus); }
// }
