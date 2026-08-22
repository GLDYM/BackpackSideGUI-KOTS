package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class DragDistributePayload implements CustomPacketPayload {
    private final List<Integer> slots;
    private final int button;
    private final ItemStack clientCarried;
    public static final CustomPacketPayload.Type<DragDistributePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "drag_distribute"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DragDistributePayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, DragDistributePayload>() {
        public DragDistributePayload decode(RegistryFriendlyByteBuf buf) {
            int encodedSize = Math.max(0, buf.readVarInt());
            int keptSize = Math.min(128, encodedSize);
            List<Integer> slots = new ArrayList<>(keptSize);
            for (int i = 0; i < encodedSize; i++) {
                int slot = buf.readVarInt();
                if (i < keptSize) {
                    slots.add(Integer.valueOf(slot));
                }
            }
            int button = buf.readVarInt();
            ItemStack carried = ItemNetUtil.readStackWithRealCount(buf);
            return new DragDistributePayload(slots, button, carried);
        }

        public void encode(RegistryFriendlyByteBuf buf, DragDistributePayload payload) {
            List<Integer> slots = payload.slots == null ? List.of() : payload.slots;
            int size = Math.min(128, slots.size());
            buf.writeVarInt(size);
            for (int i = 0; i < size; i++) {
                Integer slot = slots.get(i);
                buf.writeVarInt(slot == null ? -1 : slot.intValue());
            }
            buf.writeVarInt(payload.button);
            ItemNetUtil.writeStackWithRealCount(buf, payload.clientCarried);
        }
    };

    public DragDistributePayload(List<Integer> slots, int button, ItemStack clientCarried) {
        this.slots = slots;
        this.button = button;
        this.clientCarried = clientCarried;
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
