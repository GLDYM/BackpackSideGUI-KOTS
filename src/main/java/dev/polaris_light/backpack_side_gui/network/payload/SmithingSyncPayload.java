package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SmithingSyncPayload(ItemStack template, ItemStack base, ItemStack addition, ItemStack result)
        implements CustomPacketPayload {
    public static final Type<SmithingSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "smithing_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SmithingSyncPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> {
                writeStack(b, p.template);
                writeStack(b, p.base);
                writeStack(b, p.addition);
                writeStack(b, p.result);
            },
            b -> new SmithingSyncPayload(readStack(b), readStack(b), readStack(b), readStack(b)));

    private static void writeStack(RegistryFriendlyByteBuf b, ItemStack stack) {
        boolean present = stack != null && !stack.isEmpty();
        b.writeBoolean(present);
        if (present)
            ItemStack.STREAM_CODEC.encode(b, stack);
    }

    private static ItemStack readStack(RegistryFriendlyByteBuf b) {
        return b.readBoolean() ? ItemStack.STREAM_CODEC.decode(b) : ItemStack.EMPTY;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
