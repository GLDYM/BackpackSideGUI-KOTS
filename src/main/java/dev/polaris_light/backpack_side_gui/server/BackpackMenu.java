// package dev.polaris_light.backpack_side_gui.server;

// import dev.polaris_light.backpack_side_gui.registry.ModMenus;
// import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
// import net.minecraft.network.RegistryFriendlyByteBuf;
// import net.minecraft.network.chat.Component;
// import net.minecraft.world.SimpleContainer;
// import net.minecraft.world.entity.player.Inventory;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.inventory.AbstractContainerMenu;
// import net.minecraft.world.inventory.Slot;
// import net.minecraft.world.item.ItemStack;
// import net.neoforged.neoforge.items.IItemHandler;
// import net.neoforged.neoforge.items.SlotItemHandler;

// public final class BackpackMenu extends AbstractContainerMenu {
//     private final int rows;
//     private final BackpackAccess access;
//     private final Component title;

//     public static BackpackMenu server(int id, Inventory inventory, BackpackAccess access) {
//         return new BackpackMenu(id, access, access.handler().getSlots(), Component.translatable("container.backpack_side_gui.backpack"));
//     }

//     public static BackpackMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
//         int slots = Math.max(1, Math.min(162, data.readVarInt()));
//         return new BackpackMenu(id, null, slots, Component.literal(data.readUtf(128)));
//     }

//     private BackpackMenu(int id, BackpackAccess access, int slots, Component title) {
//         super(ModMenus.BACKPACK.get(), id);
//         this.access = access;
//         this.title = title;
//         this.rows = Math.max(1, (slots + 8) / 9);
//         IItemHandler handler = access == null ? null : access.handler();
//         SimpleContainer clientSlots = handler == null ? new SimpleContainer(slots) : null;
//         for (int slot = 0; slot < slots; slot++) {
//             int x = 8 + (slot % 9) * 18;
//             int y = 18 + (slot / 9) * 18;
//             addSlot(handler == null ? new Slot(clientSlots, slot, x, y) : new SlotItemHandler(handler, slot, x, y));
//         }
//     }

//     public int rows() { return rows; }
//     public Component title() { return title; }

//     @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

//     @Override public boolean stillValid(Player player) {
//         return access != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
//                 && BackpackResolver.isValid(serverPlayer, access);
//     }
// }
