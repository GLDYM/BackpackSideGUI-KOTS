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
            (buffer, payload) -> {
                writeStack(buffer, payload.template);
                writeStack(buffer, payload.base);
                writeStack(buffer, payload.addition);
                writeStack(buffer, payload.result);
            },
            buffer -> new SmithingSyncPayload(readStack(buffer), readStack(buffer), readStack(buffer),
                    readStack(buffer)));

    private static void writeStack(RegistryFriendlyByteBuf buffer, ItemStack stack) {
        boolean present = stack != null && !stack.isEmpty();
        buffer.writeBoolean(present);
        if (present)
            ItemStack.STREAM_CODEC.encode(buffer, stack);
    }

    private static ItemStack readStack(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? ItemStack.STREAM_CODEC.decode(buffer) : ItemStack.EMPTY;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
