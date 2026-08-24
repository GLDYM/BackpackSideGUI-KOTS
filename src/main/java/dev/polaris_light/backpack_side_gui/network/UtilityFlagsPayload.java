package dev.polaris_light.backpack_side_gui.network;

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
            .of((b, p) -> {
                b.writeBoolean(p.crafting());
                b.writeBoolean(p.furnace());
                b.writeBoolean(p.anvil());
                b.writeBoolean(p.smithing());
                b.writeBoolean(p.stonecutter());
            }, b -> new UtilityFlagsPayload(b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(),
                    b.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
