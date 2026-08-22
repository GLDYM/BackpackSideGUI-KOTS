package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class RequestPanelPayload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestPanelPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "request_panel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPanelPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, RequestPanelPayload>() {
        public RequestPanelPayload decode(RegistryFriendlyByteBuf buf) {
            return new RequestPanelPayload();
        }

        public void encode(RegistryFriendlyByteBuf buf, RequestPanelPayload payload) {
        }
    };

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
