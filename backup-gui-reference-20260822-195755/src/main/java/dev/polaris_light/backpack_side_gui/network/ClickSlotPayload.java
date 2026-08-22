package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ClickSlotPayload implements CustomPacketPayload {
    private final int logicalSlot;
    private final int button;
    private final ItemStack clientCarried;
    public static final CustomPacketPayload.Type<ClickSlotPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "click_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClickSlotPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ClickSlotPayload>() {
        public ClickSlotPayload decode(RegistryFriendlyByteBuf buf) {
            return new ClickSlotPayload(buf.readVarInt(), buf.readVarInt(), ItemNetUtil.readStackWithRealCount(buf));
        }

        public void encode(RegistryFriendlyByteBuf buf, ClickSlotPayload payload) {
            buf.writeVarInt(payload.logicalSlot);
            buf.writeVarInt(payload.button);
            ItemNetUtil.writeStackWithRealCount(buf, payload.clientCarried);
        }
    };

    public ClickSlotPayload(int logicalSlot, int button, ItemStack clientCarried) {
        this.logicalSlot = logicalSlot;
        this.button = button;
        this.clientCarried = clientCarried;
    }

    public int logicalSlot() {
        return this.logicalSlot;
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
