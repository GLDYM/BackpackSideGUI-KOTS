package dev.polaris_light.backpack_side_gui.network;

import dev.polaris_light.backpack_side_gui.BackpackSideGuiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class JeiRetryTransferPayload implements CustomPacketPayload {
    private final int utilityToOpen;
    private final boolean retryOriginalClick;
    public static final int NONE = -1;
    public static final CustomPacketPayload.Type<JeiRetryTransferPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackSideGuiMod.MOD_ID, "jei_retry_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JeiRetryTransferPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, JeiRetryTransferPayload>() {
        public JeiRetryTransferPayload decode(RegistryFriendlyByteBuf buf) {
            return new JeiRetryTransferPayload(buf.readVarInt(), buf.readBoolean());
        }

        public void encode(RegistryFriendlyByteBuf buf, JeiRetryTransferPayload payload) {
            buf.writeVarInt(payload.utilityToOpen);
            buf.writeBoolean(payload.retryOriginalClick);
        }
    };

    public JeiRetryTransferPayload(int utilityToOpen, boolean retryOriginalClick) {
        this.utilityToOpen = utilityToOpen;
        this.retryOriginalClick = retryOriginalClick;
    }

    public int utilityToOpen() {
        return this.utilityToOpen;
    }

    public boolean retryOriginalClick() {
        return this.retryOriginalClick;
    }

    public static JeiRetryTransferPayload retry() {
        return new JeiRetryTransferPayload(-1, true);
    }

    public static JeiRetryTransferPayload openCrafting() {
        return new JeiRetryTransferPayload(0, false);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
