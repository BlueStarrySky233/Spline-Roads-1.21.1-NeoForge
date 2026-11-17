package com.sora.splineroads.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;

/**
 * Optional hand-off to Voxy's public LOD render hook.
 *
 * <p>Voxy exposes {@code me.cortex.voxy.client.compat.LodPipelineHooks#register(Renderer)} and uses
 * it for its own distant renderers (Create trains, copycat blocks, LittleTiles). Nothing here
 * touches Voxy internals: no mixin, no fork, and no compile-time dependency. The {@code Renderer}
 * interface is satisfied by a dynamic proxy, so {@link RoadDistantRenderer} needs no Voxy types.
 */
public final class RoadVoxyBridge {
  private static final String HOOKS = "me.cortex.voxy.client.compat.LodPipelineHooks";
  private static boolean installed;
  private static String state = "not installed";
  /** Runtime switch: /sr voxy on|off. Survives config rewrites, unlike the file entry. */
  private static volatile boolean enabled = true;
  /** Pavement brightness for the distant layer; /sr voxyshade tunes it live. */
  private static volatile float pavementShade = 0.82f;
  /** Brightness floor: what a fully unlit road still shows. /sr voxyfloor. */
  private static volatile float lightFloor = 0.12f;
  /** How strongly sampled light moves the result. /sr voxylight. */
  private static volatile float lightGain = 1.05f;

  public static boolean enabled() { return enabled; }

  public static void setEnabled(boolean value) { enabled = value; }

  public static float pavementShade() { return pavementShade; }

  public static void setPavementShade(float value) { pavementShade = value; }

  public static float lightFloor() { return lightFloor; }

  public static void setLightFloor(float value) { lightFloor = value; }

  public static float lightGain() { return lightGain; }

  public static void setLightGain(float value) { lightGain = value; }

  /** Client setup: install only when Voxy is present, and only once. */
  public static void install() {
    if (installed) return;
    installed = true;
    if (!net.neoforged.fml.ModList.get().isLoaded("voxy")) {
      state = "voxy absent";
      return;
    }
    try {
      Class<?> hooks = Class.forName(HOOKS);
      Class<?> rendererType = Class.forName(HOOKS + "$Renderer");
      var renderer = new RoadDistantRenderer();
      Object proxy = Proxy.newProxyInstance(rendererType.getClassLoader(), new Class<?>[] {rendererType},
          (self, method, arguments) -> {
            switch (method.getName()) {
              case "render" -> renderer.render(arguments[0], arguments[1], (Integer) arguments[2]);
              case "onResourceReload" -> renderer.onResourceReload();
              default -> { }
            }
            return null;
          });
      MethodHandle register = MethodHandles.publicLookup()
          .findStatic(hooks, "register", MethodType.methodType(void.class, rendererType));
      register.invoke(proxy);
      state = "registered";
      System.getLogger("SplineRoads").log(System.Logger.Level.INFO,"[SR] Voxy LOD hook registered; voxyDistantRoads="
          + com.sora.splineroads.config.RoadClientConfig.VOXY_DISTANT_ROADS.get()
          + " lodInfillBlock='" + com.sora.splineroads.config.RoadClientConfig.LOD_INFILL_BLOCK.get() + "'");
    } catch (ClassNotFoundException absent) {
      state = "voxy hook missing";
    } catch (Throwable failure) {
      state = "failed: " + failure;
    }
    System.getLogger("SplineRoads").log(System.Logger.Level.INFO,"[SR] Voxy LOD hook " + state);
  }

  public static String state() { return state; }

  private RoadVoxyBridge() {}
}
