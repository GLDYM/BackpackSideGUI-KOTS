package dev.polaris_light.backpack_side_gui.network;

import java.util.List;

import dev.polaris_light.backpack_side_gui.network.payload.AnvilClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.AnvilRenamePayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackDragPayload;
import dev.polaris_light.backpack_side_gui.network.payload.BackpackSlotPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.CraftingDragPayload;
import dev.polaris_light.backpack_side_gui.network.payload.FurnaceClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.JeiCraftingFillPayload;
import dev.polaris_light.backpack_side_gui.network.payload.OpenBackpackPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SmithingClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.SortPayload;
import dev.polaris_light.backpack_side_gui.network.payload.StonecutterClickPayload;
import dev.polaris_light.backpack_side_gui.network.payload.UtilityRequestPayload;
import dev.polaris_light.backpack_side_gui.network.payload.JeiSmithingFillPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side packet construction kept separate from server packet handlers.
 */
public final class ClientPacketSender {
    private ClientPacketSender() {
    }

    public static void open() {
        PacketDistributor.sendToServer(new OpenBackpackPayload(),
                new CustomPacketPayload[0]);
    }

    public static void utility(int type) {
        PacketDistributor.sendToServer(new UtilityRequestPayload(type),
                new CustomPacketPayload[0]);
    }

    public static void sort(int mode) {
        PacketDistributor.sendToServer(new SortPayload(mode),
                new CustomPacketPayload[0]);
    }

    public static void backpackSlot(int slot, int clickType, ItemStack carried) {
        PacketDistributor.sendToServer(new BackpackSlotPayload(slot, clickType, carried.copy()),
                new CustomPacketPayload[0]);
    }

    public static void backpackDrag(List<Integer> slots, int button, ItemStack carried) {
        PacketDistributor.sendToServer(new BackpackDragPayload(List.copyOf(slots), button, carried.copy()),
                new CustomPacketPayload[0]);
    }

    public static void smithingSlot(int slot, int button, ItemStack carried) {
        PacketDistributor.sendToServer(new SmithingClickPayload(slot, button, carried.copy()));
    }

    public static void anvilSlot(int slot, int button, ItemStack carried) {
        PacketDistributor.sendToServer(new AnvilClickPayload(slot, button, carried.copy()));
    }

    public static void anvilRename(String name) {
        PacketDistributor.sendToServer(new AnvilRenamePayload(name == null ? "" : name));
    }

    public static void craftingSlot(int slot, int button, boolean shift, ItemStack carried) {
        PacketDistributor.sendToServer(new CraftingClickPayload(slot, button, shift, carried.copy()));
    }

    public static void craftingDrag(List<Integer> slots, int button, ItemStack carried) {
        PacketDistributor.sendToServer(new CraftingDragPayload(List.copyOf(slots), button, carried.copy()));
    }

    public static void jeiCraftingFill(List<List<ItemStack>> ingredients, boolean maxTransfer) {
        PacketDistributor.sendToServer(new JeiCraftingFillPayload(ingredients, maxTransfer));
    }
    public static void jeiSmithingFill(List<List<ItemStack>> ingredients, boolean maxTransfer) { PacketDistributor.sendToServer(new JeiSmithingFillPayload(ingredients, maxTransfer)); }

    public static void furnaceSlot(int slot, int button, ItemStack carried) {
        PacketDistributor.sendToServer(new FurnaceClickPayload(slot, button, carried.copy()));
    }

    public static void stonecutterSlot(int slot, int button, int recipe, boolean shift, ItemStack carried) {
        PacketDistributor.sendToServer(new StonecutterClickPayload(slot, button, recipe, shift, carried.copy()));
    }
}
