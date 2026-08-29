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
            .of((buffer, payload) -> {
                int n = Math.min(9, payload.ingredients().size());
                buffer.writeVarInt(n);
                for (int i = 0; i < n; i++) {
                    List<ItemStack> ingredientOptions = payload.ingredients().get(i);
                    int m = Math.min(64, ingredientOptions == null ? 0 : ingredientOptions.size());
                    buffer.writeVarInt(m);
                    for (int j = 0; j < m; j++)
                        ItemStack.STREAM_CODEC.encode(buffer, ingredientOptions.get(j));
                }
                buffer.writeBoolean(payload.maxTransfer());
            }, buffer -> {
                int n = Math.min(9, buffer.readVarInt());
                List<List<ItemStack>> out = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    int m = Math.min(64, buffer.readVarInt());
                    List<ItemStack> ingredientOptions = new ArrayList<>(m);
                    for (int j = 0; j < m; j++)
                        ingredientOptions.add(ItemStack.STREAM_CODEC.decode(buffer));
                    out.add(ingredientOptions);
                }
                return new JeiBackpackFillPayload(out, buffer.readBoolean());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
