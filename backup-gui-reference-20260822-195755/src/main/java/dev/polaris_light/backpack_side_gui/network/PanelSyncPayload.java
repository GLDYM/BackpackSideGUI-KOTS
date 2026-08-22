package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PanelSyncPayload implements CustomPacketPayload {
    private final boolean available;
    private final int slotCount;
    private final String displayName;
    private final List<ItemStack> items;
    private final boolean craftingUpgrade;
    private final boolean furnaceUpgrade;
    private final boolean anvilUpgrade;
    private final boolean smithingUpgrade;
    public static final CustomPacketPayload.Type<PanelSyncPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "panel_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PanelSyncPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, PanelSyncPayload>() {
        public PanelSyncPayload decode(RegistryFriendlyByteBuf buf) {
            boolean available = buf.readBoolean();
            int slotCount = buf.readVarInt();
            String name = buf.readUtf(128);
            int encodedSize = Math.max(0, buf.readVarInt());
            int keptSize = Math.min(512, encodedSize);
            List<ItemStack> items = new ArrayList<>(keptSize);
            for (int i = 0; i < encodedSize; i++) {
                ItemStack stack = ItemNetUtil.readStackWithRealCount(buf);
                if (i < keptSize) {
                    items.add(stack);
                }
            }
            boolean crafting = buf.readBoolean();
            boolean furnace = buf.readBoolean();
            boolean anvil = buf.readBoolean();
            boolean smithing = buf.readBoolean();
            return new PanelSyncPayload(available, slotCount, name, items, crafting, furnace, anvil, smithing);
        }

        public void encode(RegistryFriendlyByteBuf buf, PanelSyncPayload payload) {
            buf.writeBoolean(payload.available);
            buf.writeVarInt(payload.slotCount);
            buf.writeUtf(payload.displayName == null ? "" : payload.displayName, 128);
            int size = Math.min(512, payload.items.size());
            buf.writeVarInt(size);
            for (int i = 0; i < size; i++) {
                ItemNetUtil.writeStackWithRealCount(buf, payload.items.get(i));
            }
            buf.writeBoolean(payload.craftingUpgrade);
            buf.writeBoolean(payload.furnaceUpgrade);
            buf.writeBoolean(payload.anvilUpgrade);
            buf.writeBoolean(payload.smithingUpgrade);
        }
    };

    public PanelSyncPayload(boolean available, int slotCount, String displayName, List<ItemStack> items, boolean craftingUpgrade, boolean furnaceUpgrade, boolean anvilUpgrade, boolean smithingUpgrade) {
        this.available = available;
        this.slotCount = slotCount;
        this.displayName = displayName;
        this.items = items;
        this.craftingUpgrade = craftingUpgrade;
        this.furnaceUpgrade = furnaceUpgrade;
        this.anvilUpgrade = anvilUpgrade;
        this.smithingUpgrade = smithingUpgrade;
    }

    public boolean available() {
        return this.available;
    }

    public int slotCount() {
        return this.slotCount;
    }

    public String displayName() {
        return this.displayName;
    }

    public List<ItemStack> items() {
        return this.items;
    }

    public boolean craftingUpgrade() {
        return this.craftingUpgrade;
    }

    public boolean furnaceUpgrade() {
        return this.furnaceUpgrade;
    }

    public boolean anvilUpgrade() {
        return this.anvilUpgrade;
    }

    public boolean smithingUpgrade() {
        return this.smithingUpgrade;
    }

    public static PanelSyncPayload empty() {
        return new PanelSyncPayload(false, 0, "", List.of(), false, false, false, false);
    }

    public int backpackSlot() {
        return -1;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
