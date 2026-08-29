package dev.polaris_light.backpack_side_gui.network.payload;

import java.util.ArrayList;
import java.util.List;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record JeiCraftingFillPayload(List<List<ItemStack>> ingredients, boolean maxTransfer)
        implements CustomPacketPayload {
    public static final Type<JeiCraftingFillPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "jei_crafting_fill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JeiCraftingFillPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public JeiCraftingFillPayload decode(RegistryFriendlyByteBuf buffer) {
            int slots = Math.min(9, buffer.readVarInt());
            List<List<ItemStack>> result = new ArrayList<>(slots);
            for (int slot = 0; slot < slots; slot++) {
                int optionCount = Math.min(32, buffer.readVarInt());
                List<ItemStack> options = new ArrayList<>(optionCount);
                for (int option = 0; option < optionCount; option++)
                    options.add(ItemStack.STREAM_CODEC.decode(buffer));
                result.add(options);
            }
            return new JeiCraftingFillPayload(result, buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, JeiCraftingFillPayload payload) {
            int slots = Math.min(9, payload.ingredients.size());
            buffer.writeVarInt(slots);
            for (int slot = 0; slot < slots; slot++) {
                List<ItemStack> options = payload.ingredients.get(slot);
                int optionCount = Math.min(32, options.size());
                buffer.writeVarInt(optionCount);
                for (int option = 0; option < optionCount; option++)
                    ItemStack.STREAM_CODEC.encode(buffer, options.get(option).copyWithCount(1));
            }
            buffer.writeBoolean(payload.maxTransfer);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
