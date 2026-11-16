package com.sora.splineroads.compat.mtr;

import com.sora.splineroads.core.RailbedPath;
import com.sora.splineroads.core.RoadGeometry.V;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.mtr.core.data.Position;
import org.mtr.core.data.Rail;
import org.mtr.core.data.TransportMode;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Vector;

/**
 * MTR 4.1 adapter. Only MTR's simulator thread enters here; no live {@link Rail} escapes to a
 * Minecraft thread.
 *
 * <p>4.1.0 dropped {@code Simulator.positionsToRail} and {@code railIdMap}, so the adjacency is
 * built from {@link Simulator#rails} on demand and cached per dimension until {@code Data.sync()}
 * invalidates it. Rails have no exposed hex id either, so the segment id is a digest of the
 * bounding box, which is stable across rebuilds of the same graph.
 */
final class Mtr4Adapter {
  private record Edge(Rail rail, Position from, Position to, boolean reverse) {}

  private static final Map<Simulator, Task> tasks = Collections.synchronizedMap(new WeakHashMap<>());
  private static final Map<String, Map<Position, List<Rail>>> graphs = new ConcurrentHashMap<>();
  private static final Set<String> building = ConcurrentHashMap.newKeySet();

  static boolean active() {
    return !tasks.isEmpty();
  }

  /** Called on {@code Data.sync()}: the graph is stale, so drop every cached adjacency. */
  static void invalidate(String dimension) {
    if (dimension == null) graphs.clear();
    else {
      graphs.remove(dimension);
      graphs.remove(flip(dimension));
    }
  }

  static boolean trainNode(Object block) {
    return block instanceof org.mtr.block.BlockNode node && node.transportMode == TransportMode.TRAIN;
  }

  static boolean railbedItem(Object item) {
    return item instanceof com.sora.splineroads.world.RailbedTool;
  }

  /**
   * The rail node block has an empty collision shape; once a railbed covers that cell the ground
   * under the track must be solid again, so the node inherits a full block shape there.
   */
  static Object nodeShape(Object original, Object world, Object position) {
    if (!(world instanceof net.minecraft.world.level.Level level)
        || !(position instanceof net.minecraft.core.BlockPos pos)) return original;
    var index = com.sora.splineroads.world.RoadBlocks.index(level);
    if (index == null) return original;
    long chunk = net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    for (UUID id : index.inChunk(chunk)) {
      var built = index.roads.get(id);
      if (built != null && built.record.railway() && !built.boxes(pos).isEmpty()) {
        return net.minecraft.world.phys.shapes.Shapes.block();
      }
    }
    return original;
  }

  static void tick(Object value) {
    if (!(value instanceof Simulator simulator)) return;
    Task task = tasks.get(simulator);
    if (task == null) {
      for (var request : MtrRailAccess.requests) {
        if (!sameDimension(request.dimension(), simulator.dimension)) continue;
        if (!MtrRailAccess.requests.remove(request)) continue;
        if (request.result().isDone()) continue;
        try {
          task = new Task(simulator, request);
          tasks.put(simulator, task);
        } catch (RuntimeException failure) {
          request.result().completeExceptionally(failure);
        }
        break;
      }
    }
    if (task == null) return;
    try {
      long deadline = System.nanoTime() + 2_000_000L;
      int steps = 0;
      while (!task.request.result().isDone() && steps++ < 256 && System.nanoTime() < deadline) {
        task.step(simulator);
      }
    } catch (RuntimeException failure) {
      task.request.result().completeExceptionally(failure);
    }
    if (task.request.result().isDone()) tasks.remove(simulator);
  }

  /** MTR stores dimensions as namespace/path; accept either separator. */
  static boolean sameDimension(String a, String b) {
    if (a == null || b == null) return false;
    return a.equals(b) || flip(a).equals(b) || a.equals(flip(b));
  }

  private static String flip(String dimension) {
    int colon = dimension.indexOf(':');
    if (colon >= 0) return dimension.substring(0, colon) + "/" + dimension.substring(colon + 1);
    int slash = dimension.indexOf('/');
    if (slash >= 0) return dimension.substring(0, slash) + ":" + dimension.substring(slash + 1);
    return "minecraft:" + dimension;
  }

  private static Position position(RailbedPath.Node node) {
    return new Position(node.x(), node.y(), node.z());
  }

  private static RailbedPath.Node node(Position position) {
    return new RailbedPath.Node((int) position.getX(), (int) position.getY(), (int) position.getZ());
  }

  /** Rail geometry is in block-centre space, the node grid is in block-corner space. */
  private static Position position(Vector vector) {
    return new Position(Math.round(vector.x() - .5), Math.round(vector.y()), Math.round(vector.z() - .5));
  }

  private static Position start(Rail rail) {
    return position(rail.railMath.getPosition(0, false));
  }

  private static Position end(Rail rail) {
    return position(rail.railMath.getPosition(rail.railMath.getLength(), false));
  }

  private static boolean train(Rail rail) {
    return rail != null && rail.isValid() && rail.getTransportMode() == TransportMode.TRAIN
        && !rail.canConnectRemotely();
  }

  private static Map<Position, List<Rail>> graph(Simulator simulator) {
    String key = simulator.dimension;
    var cached = graphs.get(key);
    if (cached != null) return cached;
    if (!building.add(key)) {
      // Another request is already scanning this dimension; fall back to a one-off scan.
      return scan(simulator);
    }
    try {
      var built = scan(simulator);
      graphs.putIfAbsent(key, built);
      return built;
    } finally {
      building.remove(key);
    }
  }

  private static Map<Position, List<Rail>> scan(Simulator simulator) {
    var map = new HashMap<Position, List<Rail>>();
    int count = 0;
    for (var rail : simulator.rails) {
      if (!train(rail)) continue;
      if (++count > 200_000) throw new IllegalArgumentException("MTR 轨道数量超过预算，请减少当前维度的轨道");
      map.computeIfAbsent(start(rail), k -> new ArrayList<>()).add(rail);
      map.computeIfAbsent(end(rail), k -> new ArrayList<>()).add(rail);
    }
    return map;
  }

  private static List<Edge> path(Simulator simulator, Position from, Position to) {
    var adjacency = graph(simulator);
    var results = new ArrayList<List<Edge>>();
    walk(adjacency, from, to, new ArrayList<>(), new HashSet<>(), results);
    if (results.size() != 1) {
      throw new IllegalArgumentException(results.isEmpty()
          ? "请先使用 MTR 连接这两个轨道节点"
          : "铁路途中存在道岔、环线或超长区间，请分段选择");
    }
    var edges = results.get(0);
    double length = 0;
    for (var edge : edges) length += edge.rail().railMath.getLength();
    if (edges.size() > RailbedPath.MAX_SEGMENTS || length > RailbedPath.MAX_LENGTH) {
      throw new IllegalArgumentException("铁路长度超过 2048 格，请分段选择");
    }
    return edges;
  }

  private static void walk(Map<Position, List<Rail>> adjacency, Position current, Position target,
      List<Edge> accumulated, Set<Rail> used, List<List<Edge>> results) {
    if (results.size() > 1) return;
    if (current.equals(target) && !accumulated.isEmpty()) {
      results.add(List.copyOf(accumulated));
      return;
    }
    if (accumulated.size() >= RailbedPath.MAX_SEGMENTS) return;
    for (var rail : adjacency.getOrDefault(current, List.of())) {
      if (!used.add(rail)) continue;
      var edgeStart = start(rail);
      var edgeEnd = end(rail);
      boolean reverse = !edgeStart.equals(current);
      var next = reverse ? edgeStart : edgeEnd;
      accumulated.add(new Edge(rail, current, next, reverse));
      walk(adjacency, next, target, accumulated, used, results);
      accumulated.remove(accumulated.size() - 1);
      used.remove(rail);
    }
  }

  /** Stable within a graph rebuild: no MTR rail id is exposed in 4.1.0. */
  private static String segmentId(Rail rail) {
    var math = rail.railMath;
    long hash = 1125899906842597L;
    for (long value : new long[] {math.minX, math.minY, math.minZ, math.maxX, math.maxY, math.maxZ}) {
      hash = 31 * hash + value;
    }
    return Long.toHexString(hash);
  }

  private static final class Task {
    final MtrRailAccess.Request request;
    final List<Edge> edges;
    final List<RailbedPath.Segment> output = new ArrayList<>();
    RailbedPath.Sampler sampler;
    int index;
    int total;

    Task(Simulator simulator, MtrRailAccess.Request request) {
      this.request = request;
      this.edges = path(simulator, position(request.a()), position(request.b()));
    }

    void step(Simulator simulator) {
      if (System.nanoTime() > request.deadline()) {
        throw new IllegalArgumentException("铁路读取超时，请缩短区间");
      }
      var edge = edges.get(index);
      var math = edge.rail().railMath;
      if (sampler == null) {
        sampler = new RailbedPath.Sampler(math.getLength(), distance -> {
          var point = math.getPosition(distance, edge.reverse());
          return new V(point.x(), point.y(), point.z());
        });
      }
      sampler.step();
      if (!sampler.done()) return;
      var points = sampler.result();
      total += points.size();
      if (total > RailbedPath.MAX_POINTS) {
        throw new IllegalArgumentException("铁路采样超过预算，请分段选择");
      }
      output.add(new RailbedPath.Segment(segmentId(edge.rail()), node(edge.from()), node(edge.to()),
          edge.reverse(), math.getLength(), List.copyOf(edge.rail().getStyles()), points));
      sampler = null;
      if (++index == edges.size()) request.result().complete(new RailbedPath.Path(output));
    }
  }

  private Mtr4Adapter() {}
}
