package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record AnvilSyncPayload(ItemStack first, ItemStack second, ItemStack result, int cost, String name)
        implements CustomPacketPayload {
    public static final Type<AnvilSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "anvil_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilSyncPayload> STREAM_CODEC = StreamCodec
            .of((buffer, payload) -> {
                writeStack(buffer, payload.first);
                writeStack(buffer, payload.second);
                writeStack(buffer, payload.result);
                buffer.writeVarInt(payload.cost);
                buffer.writeUtf(payload.name, 50);
            }, buffer -> new AnvilSyncPayload(readStack(buffer), readStack(buffer), readStack(buffer),
                    buffer.readVarInt(), buffer.readUtf(50)));

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
