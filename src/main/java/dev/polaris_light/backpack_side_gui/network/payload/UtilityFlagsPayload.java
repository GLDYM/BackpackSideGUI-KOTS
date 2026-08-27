package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UtilityFlagsPayload(boolean crafting, boolean furnace, boolean anvil, boolean smithing,
        boolean stonecutter) implements CustomPacketPayload {
    public static final Type<UtilityFlagsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "utility_flags"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UtilityFlagsPayload> STREAM_CODEC = StreamCodec
            .of((buffer, payload) -> {
                buffer.writeBoolean(payload.crafting());
                buffer.writeBoolean(payload.furnace());
                buffer.writeBoolean(payload.anvil());
                buffer.writeBoolean(payload.smithing());
                buffer.writeBoolean(payload.stonecutter());
            }, buffer -> new UtilityFlagsPayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
