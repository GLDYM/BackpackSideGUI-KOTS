package dev.polaris_light.backpack_side_gui.server.action;

import java.util.Optional;

import dev.polaris_light.backpack_side_gui.server.BackpackResolver;
import dev.polaris_light.backpack_side_gui.server.record.BackpackAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Common trust-boundary checks for serverbound backpack actions. */
public final class ServerActionValidator {
    private ServerActionValidator() {}

    public static Optional<BackpackAccess> backpack(ServerPlayer player, int slot) {
        if (player == null || !player.isAlive() || player.level().isClientSide()) return Optional.empty();
        Optional<BackpackAccess> access = BackpackResolver.resolve(player);
        return access.filter(a -> slot >= 0 && slot < a.handler().getSlots());
    }

    public static boolean carriedMatches(ServerPlayer player, ItemStack claimed) {
        if (player == null || claimed == null) return false;
        return player.gameMode.isCreative() || ItemStack.matches(player.containerMenu.getCarried(), claimed);
    }
}
