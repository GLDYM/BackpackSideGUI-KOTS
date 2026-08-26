package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenBackpackPayload() implements CustomPacketPayload {
    public static final Type<OpenBackpackPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "open_backpack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpackPayload> STREAM_CODEC = StreamCodec
            .unit(new OpenBackpackPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
