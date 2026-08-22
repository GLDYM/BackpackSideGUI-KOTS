package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class UtilitySyncPayload implements CustomPacketPayload {
    private final int utilityType;
    private final List<ItemStack> items;
    private final int litTime;
    private final int litDuration;
    private final int cookProgress;
    private final int cookTotal;
    private final int anvilCost;
    private final String anvilName;
    public static final CustomPacketPayload.Type<UtilitySyncPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "utility_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UtilitySyncPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, UtilitySyncPayload>() {
        public UtilitySyncPayload decode(RegistryFriendlyByteBuf buf) {
            int type = buf.readVarInt();
            int encodedSize = Math.max(0, buf.readVarInt());
            int keptSize = Math.min(32, encodedSize);
            List<ItemStack> items = new ArrayList<>(keptSize);
            for (int i = 0; i < encodedSize; i++) {
                ItemStack stack = ItemNetUtil.readStackWithRealCount(buf);
                if (i < keptSize) {
                    items.add(stack);
                }
            }
            return new UtilitySyncPayload(type, items, buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(64));
        }

        public void encode(RegistryFriendlyByteBuf buf, UtilitySyncPayload payload) {
            buf.writeVarInt(payload.utilityType);
            int size = Math.min(32, payload.items.size());
            buf.writeVarInt(size);
            for (int i = 0; i < size; i++) {
                ItemNetUtil.writeStackWithRealCount(buf, payload.items.get(i));
            }
            buf.writeVarInt(payload.litTime);
            buf.writeVarInt(payload.litDuration);
            buf.writeVarInt(payload.cookProgress);
            buf.writeVarInt(payload.cookTotal);
            buf.writeVarInt(payload.anvilCost);
            buf.writeUtf(payload.anvilName == null ? "" : payload.anvilName, 64);
        }
    };

    public UtilitySyncPayload(int utilityType, List<ItemStack> items, int litTime, int litDuration, int cookProgress, int cookTotal, int anvilCost, String anvilName) {
        this.utilityType = utilityType;
        this.items = items;
        this.litTime = litTime;
        this.litDuration = litDuration;
        this.cookProgress = cookProgress;
        this.cookTotal = cookTotal;
        this.anvilCost = anvilCost;
        this.anvilName = anvilName;
    }

    public int utilityType() {
        return this.utilityType;
    }

    public List<ItemStack> items() {
        return this.items;
    }

    public int litTime() {
        return this.litTime;
    }

    public int litDuration() {
        return this.litDuration;
    }

    public int cookProgress() {
        return this.cookProgress;
    }

    public int cookTotal() {
        return this.cookTotal;
    }

    public int anvilCost() {
        return this.anvilCost;
    }

    public String anvilName() {
        return this.anvilName;
    }

    public int furnaceLitTime() {
        return this.litTime;
    }

    public int furnaceLitDuration() {
        return this.litDuration;
    }

    public int furnaceCookProgress() {
        return this.cookProgress;
    }

    public int furnaceCookTotal() {
        return this.cookTotal;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
