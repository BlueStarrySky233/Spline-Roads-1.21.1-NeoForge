package com.sora.splineroads.mixin;

import com.sora.splineroads.compat.mtr.MtrRailAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invalidates cached railway adjacency after MTR synchronises its graph.
 *
 * <p>Replaces the 1.20.1 coremod injection into {@code Data#sync()}.
 */
@Mixin(targets = "org.mtr.core.data.Data")
public abstract class DataMixin {
  @Inject(method = "sync()V", at = @At("RETURN"))
  private void splineroads$sync(CallbackInfo callback) {
    MtrRailAccess.changed(this);
  }
}
