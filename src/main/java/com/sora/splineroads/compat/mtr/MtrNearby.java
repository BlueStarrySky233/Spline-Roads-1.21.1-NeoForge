package com.sora.splineroads.compat.mtr;

import com.sora.splineroads.core.RailbedPath;
import com.sora.splineroads.core.RoadGeometry.V;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.mtr.core.data.Rail;
import org.mtr.core.data.TransportMode;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Vector;

/**
 * Nearby-rail lookups for railbed generation, advanced on MTR's simulator thread.
 *
 * <p>4.1.0 exposes no position index, so this scans {@link Simulator#rails} with a bounding-box
 * filter and a hard budget. Nearby lookups are low frequency, so a cached spatial index would add
 * invalidation risk for no measurable gain.
 */
final class MtrNearby {
  private static final Map<Simulator, Task> tasks = Collections.synchronizedMap(new WeakHashMap<>());

  static void changed(Object value) {
    if (value instanceof Simulator simulator) {
      MtrRailAccess.changedDimension(simulator.dimension);
      tasks.remove(simulator);
    }
  }

  static void tick(Object value) {
    if (!(value instanceof Simulator simulator)) return;
    var task = tasks.get(simulator);
    if (task == null) {
      if (MtrRailAccess.nearby.isEmpty()) return;
      for (var request : MtrRailAccess.nearby) {
        if (!Mtr4Adapter.sameDimension(request.dimension(), simulator.dimension)) continue;
        if (!MtrRailAccess.nearby.remove(request)) continue;
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

  private static boolean train(Rail rail) {
    return rail != null && rail.isValid() && rail.getTransportMode() == TransportMode.TRAIN
        && !rail.canConnectRemotely();
  }

  private static RailbedPath.Node node(Vector vector) {
    return new RailbedPath.Node((int) Math.round(vector.x() - .5), (int) Math.round(vector.y()),
        (int) Math.round(vector.z() - .5));
  }

  private static String segmentId(Rail rail) {
    var math = rail.railMath;
    long hash = 1125899906842597L;
    for (long value : new long[] {math.minX, math.minY, math.minZ, math.maxX, math.maxY, math.maxZ}) {
      hash = 31 * hash + value;
    }
    return Long.toHexString(hash);
  }

  private static final class Task {
    final MtrRailAccess.NearRequest request;
    final List<Rail> rails;
    final List<RailbedPath.Path> output = new ArrayList<>();
    int cursor;
    RailbedPath.Sampler sampler;

    Task(Simulator simulator, MtrRailAccess.NearRequest request) {
      this.request = request;
      var min = request.min();
      var max = request.max();
      var found = new ArrayList<Rail>();
      for (var rail : simulator.rails) {
        if (!train(rail)) continue;
        var math = rail.railMath;
        if (math.maxX >= min.x() && math.minX <= max.x()
            && math.maxZ >= min.z() && math.minZ <= max.z()
            && math.maxY >= min.y() && math.minY <= max.y()) {
          found.add(rail);
        }
      }
      if (found.size() > 128) {
        throw new IllegalArgumentException("附近轨道过多，请缩短 A–B 区间");
      }
      this.rails = List.copyOf(found);
    }

    void step(Simulator simulator) {
      if (cursor == rails.size()) {
        request.result().complete(List.copyOf(output));
        return;
      }
      var rail = rails.get(cursor);
      var math = rail.railMath;
      if (sampler == null) {
        sampler = new RailbedPath.Sampler(math.getLength(), distance -> {
          var point = math.getPosition(distance, false);
          return new V(point.x(), point.y(), point.z());
        });
      }
      sampler.step();
      if (!sampler.done()) return;
      var points = sampler.result();
      output.add(new RailbedPath.Path(List.of(new RailbedPath.Segment(segmentId(rail),
          node(math.getPosition(0, false)), node(math.getPosition(math.getLength(), false)), false,
          math.getLength(), List.copyOf(rail.getStyles()), points))));
      sampler = null;
      cursor++;
    }
  }

  private MtrNearby() {}
}
