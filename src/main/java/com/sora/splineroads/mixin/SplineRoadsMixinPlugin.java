package com.sora.splineroads.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Every mixin in this package targets an MTR class, and MTR is optional. Applying them to a
 * missing target would abort startup, so the whole set is gated on MTR being present.
 */
public final class SplineRoadsMixinPlugin implements IMixinConfigPlugin {
  private static final boolean MTR = isMtrPresent();

  static {
    // One line per launch tells whether the railway hooks are live; without it a missing MTR
    // and a broken hook look identical from the outside.
    System.getLogger("SplineRoads").log(System.Logger.Level.INFO,
        "[SR] MTR railway mixins " + (MTR ? "enabled" : "disabled (MTR absent)"));
  }

  private static boolean isMtrPresent() {
    try {
      return LoadingModList.get().getModFileById("mtr") != null;
    } catch (RuntimeException | LinkageError unavailable) {
      return false;
    }
  }

  @Override
  public void onLoad(String mixinPackage) {}

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
    return MTR;
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
      IMixinInfo mixinInfo) {}

  @Override
  public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
      IMixinInfo mixinInfo) {}
}
