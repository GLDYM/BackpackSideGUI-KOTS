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
 public static final StreamCodec<RegistryFriendlyByteBuf,JeiSmithingFillPayload> STREAM_CODEC=StreamCodec.of((b,p)->{b.writeVarInt(Math.min(3,p.ingredients.size())); for(int i=0;i<Math.min(3,p.ingredients.size());i++){var l=p.ingredients.get(i);b.writeVarInt(Math.min(16,l.size()));for(int j=0;j<Math.min(16,l.size());j++)ItemStack.STREAM_CODEC.encode(b,l.get(j));}b.writeBoolean(p.maxTransfer);},b->{int n=Math.min(3,b.readVarInt());var out=new java.util.ArrayList<List<ItemStack>>();for(int i=0;i<n;i++){int m=Math.min(16,b.readVarInt());var l=new java.util.ArrayList<ItemStack>();for(int j=0;j<m;j++)l.add(ItemStack.STREAM_CODEC.decode(b));out.add(l);}return new JeiSmithingFillPayload(out,b.readBoolean());});
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
