package dev.polaris_light.backpack_side_gui.network;
import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf; import net.minecraft.network.codec.StreamCodec; import net.minecraft.network.protocol.common.custom.CustomPacketPayload; import net.minecraft.resources.ResourceLocation; import net.minecraft.world.item.ItemStack;
public record CraftingClickPayload(int slot, int button, boolean shift, ItemStack carried) implements CustomPacketPayload {
 public static final Type<CraftingClickPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID,"crafting_click"));
 public static final StreamCodec<RegistryFriendlyByteBuf,CraftingClickPayload> STREAM_CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.slot);b.writeVarInt(p.button);b.writeBoolean(p.shift);b.writeBoolean(!p.carried.isEmpty());if(!p.carried.isEmpty())ItemStack.STREAM_CODEC.encode(b,p.carried);},b->new CraftingClickPayload(b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean()?ItemStack.STREAM_CODEC.decode(b):ItemStack.EMPTY));
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
