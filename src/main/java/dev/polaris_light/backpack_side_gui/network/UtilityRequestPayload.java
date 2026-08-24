package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UtilityRequestPayload(int utilityType) implements CustomPacketPayload {
    public static final Type<UtilityRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "utility_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UtilityRequestPayload> STREAM_CODEC = StreamCodec
            .of((b, p) -> b.writeVarInt(p.utilityType()), b -> new UtilityRequestPayload(b.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
