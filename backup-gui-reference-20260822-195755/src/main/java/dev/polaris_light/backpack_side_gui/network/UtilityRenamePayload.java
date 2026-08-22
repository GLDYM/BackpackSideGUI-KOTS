package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class UtilityRenamePayload implements CustomPacketPayload {
    private final String name;
    public static final CustomPacketPayload.Type<UtilityRenamePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "utility_rename"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UtilityRenamePayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, UtilityRenamePayload>() {
        public UtilityRenamePayload decode(RegistryFriendlyByteBuf buf) {
            return new UtilityRenamePayload(buf.readUtf(64));
        }

        public void encode(RegistryFriendlyByteBuf buf, UtilityRenamePayload payload) {
            buf.writeUtf(payload.name == null ? "" : payload.name, 64);
        }
    };

    public UtilityRenamePayload(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
