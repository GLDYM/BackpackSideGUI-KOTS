package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record StonecutterSyncPayload(ItemStack input, ItemStack output, ItemStack[] recipes, int selected)
        implements CustomPacketPayload {
    public static final Type<StonecutterSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "stonecutter_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StonecutterSyncPayload> STREAM_CODEC = StreamCodec
            .of((buffer, payload) -> {
                write(buffer, payload.input);
                write(buffer, payload.output);
                buffer.writeVarInt(payload.recipes.length);
                for (ItemStack s : payload.recipes)
                    write(buffer, s);
                buffer.writeVarInt(payload.selected);
            }, buffer -> {
                ItemStack i = read(buffer), o = read(buffer);
                int n = buffer.readVarInt();
                ItemStack[] r = new ItemStack[n];
                for (int x = 0; x < n; x++)
                    r[x] = read(buffer);
                return new StonecutterSyncPayload(i, o, r, buffer.readVarInt());
            });

    private static void write(RegistryFriendlyByteBuf buffer, ItemStack s) {
        buffer.writeBoolean(!s.isEmpty());
        if (!s.isEmpty())
            ItemStack.STREAM_CODEC.encode(buffer, s);
    }

    private static ItemStack read(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? ItemStack.STREAM_CODEC.decode(buffer) : ItemStack.EMPTY;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
