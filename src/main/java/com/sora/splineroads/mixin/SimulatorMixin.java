package com.sora.splineroads.mixin;

import com.sora.splineroads.compat.mtr.MtrRailAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Advances pending railway reads on MTR's own simulator thread.
 *
 * <p>Replaces the 1.20.1 coremod injection into {@code Simulator#tick()}.
 */
@Mixin(targets = "org.mtr.core.simulation.Simulator")
public abstract class SimulatorMixin {
  @Inject(method = "tick()V", at = @At("RETURN"))
  private void splineroads$tick(CallbackInfo callback) {
    MtrRailAccess.tick(this);
  }
}
