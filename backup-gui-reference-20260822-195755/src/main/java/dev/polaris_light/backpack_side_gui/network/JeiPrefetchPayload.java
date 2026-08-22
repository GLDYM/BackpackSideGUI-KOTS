package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class JeiPrefetchPayload implements CustomPacketPayload {
    private final List<List<ItemStack>> ingredientGroups;
    public static final CustomPacketPayload.Type<JeiPrefetchPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "jei_prefetch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JeiPrefetchPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, JeiPrefetchPayload>() {
        public JeiPrefetchPayload decode(RegistryFriendlyByteBuf buf) {
            int encodedGroups = Math.max(0, buf.readVarInt());
            int keptGroups = Math.min(32, encodedGroups);
            List<List<ItemStack>> out = new ArrayList<>(keptGroups);
            for (int g = 0; g < encodedGroups; g++) {
                int encodedSize = Math.max(0, buf.readVarInt());
                int keptSize = Math.min(16, encodedSize);
                List<ItemStack> opts = new ArrayList<>(keptSize);
                for (int i = 0; i < encodedSize; i++) {
                    ItemStack stack = ItemNetUtil.readStackWithRealCount(buf);
                    if (g < keptGroups && i < keptSize) {
                        opts.add(stack);
                    }
                }
                if (g < keptGroups) {
                    out.add(opts);
                }
            }
            return new JeiPrefetchPayload(out);
        }

        public void encode(RegistryFriendlyByteBuf buf, JeiPrefetchPayload payload) {
            List<List<ItemStack>> groups = payload.ingredientGroups == null ? List.of() : payload.ingredientGroups;
            int groupCount = Math.min(32, groups.size());
            buf.writeVarInt(groupCount);
            for (int g = 0; g < groupCount; g++) {
                List<ItemStack> safe = groups.get(g) == null ? List.of() : groups.get(g);
                int optionCount = Math.min(16, safe.size());
                buf.writeVarInt(optionCount);
                for (int i = 0; i < optionCount; i++) {
                    ItemNetUtil.writeStackWithRealCount(buf, safe.get(i));
                }
            }
        }
    };

    public JeiPrefetchPayload(List<List<ItemStack>> ingredientGroups) {
        this.ingredientGroups = ingredientGroups;
    }

    public List<List<ItemStack>> ingredientGroups() {
        return this.ingredientGroups;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
