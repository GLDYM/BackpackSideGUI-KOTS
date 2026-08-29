package dev.polaris_light.backpack_side_gui.network.payload;
import java.util.List;
import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
public record JeiSmithingFillPayload(List<List<ItemStack>> ingredients, boolean maxTransfer) implements CustomPacketPayload {
 public static final Type<JeiSmithingFillPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID,"jei_smithing_fill"));
 public static final StreamCodec<RegistryFriendlyByteBuf,JeiSmithingFillPayload> STREAM_CODEC = StreamCodec.of(
     (buffer, payload) -> {
         int ingredientCount = Math.min(3, payload.ingredients.size());
         buffer.writeVarInt(ingredientCount);
         for (int index = 0; index < ingredientCount; index++) {
             var ingredientOptions = payload.ingredients.get(index);
             int optionCount = Math.min(16, ingredientOptions.size());
             buffer.writeVarInt(optionCount);
             for (int optionIndex = 0; optionIndex < optionCount; optionIndex++)
                 ItemStack.STREAM_CODEC.encode(buffer, ingredientOptions.get(optionIndex));
         }
         buffer.writeBoolean(payload.maxTransfer);
     },
     buffer -> {
         int ingredientCount = Math.min(3, buffer.readVarInt());
         var ingredients = new java.util.ArrayList<List<ItemStack>>();
         for (int index = 0; index < ingredientCount; index++) {
             int optionCount = Math.min(16, buffer.readVarInt());
             var ingredientOptions = new java.util.ArrayList<ItemStack>();
             for (int optionIndex = 0; optionIndex < optionCount; optionIndex++)
                 ingredientOptions.add(ItemStack.STREAM_CODEC.decode(buffer));
             ingredients.add(ingredientOptions);
         }
         return new JeiSmithingFillPayload(ingredients, buffer.readBoolean());
     });
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
