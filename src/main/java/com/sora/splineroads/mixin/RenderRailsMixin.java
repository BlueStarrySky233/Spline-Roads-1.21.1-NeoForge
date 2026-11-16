package com.sora.splineroads.mixin;

import com.sora.splineroads.compat.mtr.MtrRailAccess;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Holding the railbed tool counts as holding a rail-related item, so MTR keeps drawing the rail
 * overlay the player is aiming at. Replaces the 1.20.1 coremod injection into the holding check.
 */
@Mixin(targets = "org.mtr.render.RenderRails")
public abstract class RenderRailsMixin {
  @Inject(method = "isHoldingRailRelated", at = @At("RETURN"), cancellable = true)
  private static void splineroads$holding(LocalPlayer player, CallbackInfoReturnable<Boolean> callback) {
    if (!callback.getReturnValueZ() && player != null
        && MtrRailAccess.railbedItem(player.getMainHandItem())) {
      callback.setReturnValue(true);
    }
  }
}
