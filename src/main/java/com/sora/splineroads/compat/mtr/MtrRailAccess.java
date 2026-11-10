package com.sora.splineroads.compat.mtr;

import com.sora.splineroads.core.RailbedPath;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MTR / NeoMTR bridge, 1.21.1 NeoForge port.
 *
 * <p>The 1.20.1 implementation bound directly to MTR 4.0.5 internals
 * ({@code org.mtr.core.*}, {@code org.mtr.mapping.*}, {@code org.mtr.mod.*}) and relied on two
 * Forge JavaScript coremods to patch MTR bytecode. NeoForge 1.21.1 has neither that coremod
 * runtime nor those MTR types, so the bridge is intentionally inert until a target-specific
 * adapter is written. All road/lane functionality that does not need railway data is unaffected.
 *
 * <p>To restore railbed support, implement {@link #read} and {@link #nearby} against the chosen
 * MTR build (official MTR 4.x is the lower-effort path; NeoMTR is a 3.x fork with a different
 * data model) and relax {@link #unavailable()}.
 */
public final class MtrRailAccess {
  public static final String REASON = "此版本尚未适配 MTR（1.21.1 NeoForge 移植中）";

  /**
   * Returns null when the railway source is usable, otherwise a player-facing reason.
   */
  public static String unavailable() {
    return REASON;
  }

  public static long revision(String dimension) {
    return 0L;
  }

  public static boolean trainNode(Object block) {
    return false;
  }

  public static boolean railbedItem(Object item) {
    return false;
  }

  public static Object nodeShape(Object original, Object world, Object position) {
    return original;
  }

  public static void changed(Object simulator) {}

  public static void tick(Object simulator) {}

  public static CompletableFuture<RailbedPath.Path> read(
      String dimension, RailbedPath.Node a, RailbedPath.Node b) {
    return CompletableFuture.failedFuture(new IllegalArgumentException(REASON));
  }

  public static CompletableFuture<List<RailbedPath.Path>> nearby(
      String dimension, RailbedPath.Path base) {
    return CompletableFuture.failedFuture(new IllegalArgumentException(REASON));
  }

  private MtrRailAccess() {}
}
