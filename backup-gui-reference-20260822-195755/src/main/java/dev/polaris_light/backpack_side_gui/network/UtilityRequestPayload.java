package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class UtilityRequestPayload implements CustomPacketPayload {
    private final int utilityType;
    public static final CustomPacketPayload.Type<UtilityRequestPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "utility_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UtilityRequestPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, UtilityRequestPayload>() {
        public UtilityRequestPayload decode(RegistryFriendlyByteBuf buf) {
            return new UtilityRequestPayload(buf.readVarInt());
        }

        public void encode(RegistryFriendlyByteBuf buf, UtilityRequestPayload payload) {
            buf.writeVarInt(payload.utilityType);
        }
    };

    public UtilityRequestPayload(int utilityType) {
        this.utilityType = utilityType;
    }

    public int utilityType() {
        return this.utilityType;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
