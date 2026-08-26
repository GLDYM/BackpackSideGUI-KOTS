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
            (b, p) -> b.writeUtf(p.name == null ? "" : p.name, 50), b -> new AnvilRenamePayload(b.readUtf(50)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
