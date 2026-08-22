package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class UtilityDragDistributePayload implements CustomPacketPayload {
    private final int utilityType;
    private final List<Integer> slots;
    private final int button;
    private final ItemStack clientCarried;
    public static final CustomPacketPayload.Type<UtilityDragDistributePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "utility_drag_distribute"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UtilityDragDistributePayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, UtilityDragDistributePayload>() {
        public UtilityDragDistributePayload decode(RegistryFriendlyByteBuf buf) {
            int type = buf.readVarInt();
            int encodedSize = Math.max(0, buf.readVarInt());
            int keptSize = Math.min(64, encodedSize);
            List<Integer> slots = new ArrayList<>(keptSize);
            for (int i = 0; i < encodedSize; i++) {
                int slot = buf.readVarInt();
                if (i < keptSize) {
                    slots.add(Integer.valueOf(slot));
                }
            }
            int button = buf.readVarInt();
            ItemStack carried = ItemNetUtil.readStackWithRealCount(buf);
            return new UtilityDragDistributePayload(type, slots, button, carried);
        }

        public void encode(RegistryFriendlyByteBuf buf, UtilityDragDistributePayload payload) {
            List<Integer> slots = payload.slots == null ? List.of() : payload.slots;
            buf.writeVarInt(payload.utilityType);
            int size = Math.min(64, slots.size());
            buf.writeVarInt(size);
            for (int i = 0; i < size; i++) {
                Integer slot = slots.get(i);
                buf.writeVarInt(slot == null ? -1 : slot.intValue());
            }
            buf.writeVarInt(payload.button);
            ItemNetUtil.writeStackWithRealCount(buf, payload.clientCarried);
        }
    };

    public UtilityDragDistributePayload(int utilityType, List<Integer> slots, int button, ItemStack clientCarried) {
        this.utilityType = utilityType;
        this.slots = slots;
        this.button = button;
        this.clientCarried = clientCarried;
    }

    public int utilityType() {
        return this.utilityType;
    }

    public List<Integer> slots() {
        return this.slots;
    }

    public int button() {
        return this.button;
    }

    public ItemStack clientCarried() {
        return this.clientCarried;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
