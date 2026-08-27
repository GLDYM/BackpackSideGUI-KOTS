package dev.polaris_light.backpack_side_gui.network.payload;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AnvilRenamePayload(String name) implements CustomPacketPayload {
    public static final Type<AnvilRenamePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "anvil_rename"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilRenamePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.name == null ? "" : payload.name, 50),
            buffer -> new AnvilRenamePayload(buffer.readUtf(50)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
