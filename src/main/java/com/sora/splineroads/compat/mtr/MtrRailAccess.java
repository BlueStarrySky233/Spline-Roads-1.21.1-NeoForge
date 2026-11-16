package com.sora.splineroads.compat.mtr;

import com.sora.splineroads.core.RailbedPath;
import com.sora.splineroads.core.RoadGeometry.V;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.neoforged.fml.ModList;

/**
 * MTR bridge facade for the 1.21.1 NeoForge port.
 *
 * <p>No MTR type may appear in this class: MTR is optional, so everything that touches
 * {@code org.mtr.*} lives in {@link Mtr4Adapter} and {@link MtrNearby} and is only reached after
 * {@link #unavailable()} returned null. The two Forge JavaScript coremods of the 1.20.1 build are
 * replaced by four Mixins (see {@code splineroads.mixins.json}).
 *
 * <p>Requests carry values only and are advanced on MTR's own simulator thread by
 * {@link #tick(Object)}; nothing here reads live railway state from a Minecraft thread.
 */
public final class MtrRailAccess {
  /** MTR 4.1.x is the line this port was written and tested against. */
  static final String SUPPORTED_PREFIX = "4.1";

  record Request(String dimension, RailbedPath.Node a, RailbedPath.Node b, long deadline,
      CompletableFuture<RailbedPath.Path> result) {}

  record NearRequest(String dimension, V min, V max, CompletableFuture<List<RailbedPath.Path>> result) {}

  static final Queue<Request> requests = new ConcurrentLinkedQueue<>();
  static final Queue<NearRequest> nearby = new ConcurrentLinkedQueue<>();
  private static final AtomicInteger pending = new AtomicInteger();
  private static final java.util.concurrent.atomic.AtomicBoolean HOOK_CONFIRMED =
      new java.util.concurrent.atomic.AtomicBoolean();
  private static final ConcurrentHashMap<String, AtomicLong> revisions = new ConcurrentHashMap<>();

  /** Null when the railway source is usable, otherwise a player-facing reason. */
  public static String unavailable() {
    if (!ModList.get().isLoaded("mtr")) return "请安装 MTR 4.1.0（Minecraft 1.21.1 NeoForge）";
    String version = ModList.get().getModContainerById("mtr")
        .map(container -> container.getModInfo().getVersion().toString())
        .orElse("");
    if (!version.startsWith(SUPPORTED_PREFIX)) return "此 MTR 版本尚未适配：" + version;
    return null;
  }

  public static long revision(String dimension) {
    AtomicLong value = revisions.get(dimension);
    return value == null ? 0L : value.get();
  }

  static void changedDimension(String dimension) {
    revisions.computeIfAbsent(dimension, key -> new AtomicLong()).incrementAndGet();
    Mtr4Adapter.invalidate(dimension);
  }

  public static boolean trainNode(Object block) {
    return unavailable() == null && Mtr4Adapter.trainNode(block);
  }

  public static boolean railbedItem(Object item) {
    return unavailable() == null && Mtr4Adapter.railbedItem(item);
  }

  public static Object nodeShape(Object original, Object world, Object position) {
    return unavailable() == null ? Mtr4Adapter.nodeShape(original, world, position) : original;
  }

  public static CompletableFuture<RailbedPath.Path> read(
      String dimension, RailbedPath.Node a, RailbedPath.Node b) {
    String problem = unavailable();
    if (problem != null) return CompletableFuture.failedFuture(new IllegalArgumentException(problem));
    if (a.equals(b)) return CompletableFuture.failedFuture(new IllegalArgumentException("请选择不同的铁路节点"));
    if (pending.incrementAndGet() > 16) {
      pending.decrementAndGet();
      return CompletableFuture.failedFuture(new IllegalArgumentException("铁路读取队列繁忙，请稍后重试"));
    }
    var future = new CompletableFuture<RailbedPath.Path>();
    future.orTimeout(20, TimeUnit.SECONDS);
    var request = new Request(dimension, a, b, System.nanoTime() + TimeUnit.SECONDS.toNanos(20), future);
    future.whenComplete((value, error) -> {
      pending.decrementAndGet();
      requests.remove(request);
    });
    requests.add(request);
    return future;
  }

  public static CompletableFuture<List<RailbedPath.Path>> nearby(String dimension, RailbedPath.Path base) {
    String problem = unavailable();
    if (problem != null) return CompletableFuture.failedFuture(new IllegalArgumentException(problem));
    if (nearby.size() >= 16) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("邻轨查询队列繁忙"));
    }
    var points = base.positions();
    var min = new V(
        points.stream().mapToDouble(V::x).min().orElseThrow() - 72,
        points.stream().mapToDouble(V::y).min().orElseThrow() - 100,
        points.stream().mapToDouble(V::z).min().orElseThrow() - 72);
    var max = new V(
        points.stream().mapToDouble(V::x).max().orElseThrow() + 72,
        points.stream().mapToDouble(V::y).max().orElseThrow() + 12,
        points.stream().mapToDouble(V::z).max().orElseThrow() + 72);
    var result = new CompletableFuture<List<RailbedPath.Path>>();
    result.orTimeout(30, TimeUnit.SECONDS);
    var request = new NearRequest(dimension, min, max, result);
    result.whenComplete((value, error) -> nearby.remove(request));
    nearby.add(request);
    return result;
  }

  /** Called from {@code Data.sync()}: the railway graph changed, drop cached adjacencies. */
  public static void changed(Object simulator) {
    if (unavailable() != null) return;
    // First graph sync proves the Data mixin reached MTR in this launch.
    if (HOOK_CONFIRMED.compareAndSet(false, true)) {
      System.getLogger("SplineRoads").log(System.Logger.Level.INFO,
          "[SR] MTR graph sync hook reached; railway integration is live");
    }
    MtrNearby.changed(simulator);
  }

  /** Called from {@code Simulator.tick()}: advance pending reads on MTR's own thread. */
  public static void tick(Object simulator) {
    if (unavailable() != null) return;
    if (!requests.isEmpty() || Mtr4Adapter.active()) Mtr4Adapter.tick(simulator);
    MtrNearby.tick(simulator);
  }

  private MtrRailAccess() {}
}
