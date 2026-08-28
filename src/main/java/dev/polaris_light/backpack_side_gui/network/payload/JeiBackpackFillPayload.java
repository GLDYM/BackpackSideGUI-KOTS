package dev.polaris_light.backpack_side_gui.network.payload;

import java.util.ArrayList;
import java.util.List;
import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record JeiBackpackFillPayload(List<List<ItemStack>> ingredients, boolean maxTransfer)
        implements CustomPacketPayload {
    public static final Type<JeiBackpackFillPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "jei_backpack_fill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JeiBackpackFillPayload> STREAM_CODEC = StreamCodec
            .of((b, p) -> {
                int n = Math.min(9, p.ingredients().size());
                b.writeVarInt(n);
                for (int i = 0; i < n; i++) {
                    List<ItemStack> a = p.ingredients().get(i);
                    int m = Math.min(64, a == null ? 0 : a.size());
                    b.writeVarInt(m);
                    for (int j = 0; j < m; j++)
                        ItemStack.STREAM_CODEC.encode(b, a.get(j));
                }
                b.writeBoolean(p.maxTransfer());
            }, b -> {
                int n = Math.min(9, b.readVarInt());
                List<List<ItemStack>> out = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    int m = Math.min(64, b.readVarInt());
                    List<ItemStack> a = new ArrayList<>(m);
                    for (int j = 0; j < m; j++)
                        a.add(ItemStack.STREAM_CODEC.decode(b));
                    out.add(a);
                }
                return new JeiBackpackFillPayload(out, b.readBoolean());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
