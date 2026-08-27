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
            .of((b, p) -> {
                write(b, p.input);
                write(b, p.output);
                b.writeVarInt(p.recipes.length);
                for (ItemStack s : p.recipes)
                    write(b, s);
                b.writeVarInt(p.selected);
            }, b -> {
                ItemStack i = read(b), o = read(b);
                int n = b.readVarInt();
                ItemStack[] r = new ItemStack[n];
                for (int x = 0; x < n; x++)
                    r[x] = read(b);
                return new StonecutterSyncPayload(i, o, r, b.readVarInt());
            });

    private static void write(RegistryFriendlyByteBuf b, ItemStack s) {
        b.writeBoolean(!s.isEmpty());
        if (!s.isEmpty())
            ItemStack.STREAM_CODEC.encode(b, s);
    }

    private static ItemStack read(RegistryFriendlyByteBuf b) {
        return b.readBoolean() ? ItemStack.STREAM_CODEC.decode(b) : ItemStack.EMPTY;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
