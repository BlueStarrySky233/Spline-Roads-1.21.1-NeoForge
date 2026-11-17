package com.sora.splineroads.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL45C;

/**
 * Draws road surfaces beyond the vanilla render distance through Voxy's own distant-geometry path.
 *
 * <p>This follows exactly what {@code me.cortex.voxy.client.compat.copycat.CopycatDistantRenderer}
 * does, because that is Voxy's supported way to inject foreign geometry into its LOD pass:
 *
 * <pre>
 * DistantMeshBuilder.rawVertex(...) -&gt; build() -&gt; DistantMesh.draw()
 * DistantShaders.forCopycatPipeline(pipeline, false).bind()
 * DistantShaders.bindTextures()        // block atlas -&gt; unit 0, lightmap -&gt; unit 1
 * DistantShaders.uploadTransform(mvp)
 * </pre>
 *
 * <p>Everything is reflective, so no Voxy types are compiled against and the class loads when Voxy
 * is absent. Using its shader is the whole point: lighting comes from {@code texture(uLightMap,
 * lightUv)}, so the surface follows day, night and lamps exactly like Voxy's own LOD.
 */
public final class RoadDistantRenderer {
  private static final Class<?> VIEWPORT = resolve("me.cortex.voxy.client.core.rendering.Viewport");
  private static final Class<?> PIPELINE = resolve("me.cortex.voxy.client.core.AbstractRenderPipeline");
  private static final Field CAMERA_X = field("cameraX");
  private static final Field CAMERA_Y = field("cameraY");
  private static final Field CAMERA_Z = field("cameraZ");
  private static final Field MVP = field("MVP");
  private static final Method SETUP = setup();

  private static final MethodHandle BUILDER_NEW = ctor();
  private static final MethodHandle BUILDER_RAW_VERTEX = rawVertex();
  private static final MethodHandle BUILDER_EMPTY = noArg("isEmpty");
  private static final MethodHandle BUILDER_BUILD = buildMesh();
  private static final MethodHandle SHADER_FOR_COPYCAT = forCopycat();
  private static final MethodHandle BIND_TEXTURES = staticVoid("bindTextures");
  private static final MethodHandle UPLOAD_TRANSFORM = uploadTransform();
  private static final Method SHADER_BIND = shaderBind();
  private static final MethodHandle MESH_DRAW = meshCall("draw");
  private static final MethodHandle MESH_FREE = meshCall("free");

  /** asphalt repeats every four blocks, matching the near renderer's model UVs. */
  private static final double TEXTURE_TILES = 4.0;
  private static final double MARKING_LIFT = 0.05;
  private static final int FACE_UP = 1;

  private static Class<?> resolve(String name) {
    try {
      return Class.forName(name);
    } catch (Throwable absent) {
      return null;
    }
  }

  private static Field field(String name) {
    if (VIEWPORT == null) return null;
    try {
      Field field = VIEWPORT.getField(name);
      field.setAccessible(true);
      return field;
    } catch (Throwable absent) {
      return null;
    }
  }

  private static Method setup() {
    if (PIPELINE == null || VIEWPORT == null) return null;
    try {
      Method method = PIPELINE.getMethod("setupAndBindOpaque", VIEWPORT);
      method.setAccessible(true);
      return method;
    } catch (Throwable absent) {
      return null;
    }
  }

  private static Class<?> builderClass() {
    return resolve("me.cortex.voxy.client.compat.create.DistantMeshBuilder");
  }

  private static MethodHandle ctor() {
    try {
      return MethodHandles.publicLookup()
          .findConstructor(builderClass(), MethodType.methodType(void.class));
    } catch (Throwable absent) {
      return null;
    }
  }

  private static MethodHandle rawVertex() {
    try {
      return MethodHandles.publicLookup().findVirtual(builderClass(), "rawVertex",
          MethodType.methodType(void.class, float.class, float.class, float.class,
              float.class, float.class, int.class, int.class, float.class, int.class, int.class));
    } catch (Throwable absent) {
      return null;
    }
  }

  private static MethodHandle noArg(String name) {
    try {
      return MethodHandles.publicLookup()
          .findVirtual(builderClass(), name, MethodType.methodType(boolean.class));
    } catch (Throwable absent) {
      return null;
    }
  }

  private static MethodHandle buildMesh() {
    try {
      return MethodHandles.publicLookup().findVirtual(builderClass(), "build",
          MethodType.methodType(resolve("me.cortex.voxy.client.compat.create.DistantMesh")));
    } catch (Throwable absent) {
      return null;
    }
  }

  private static MethodHandle forCopycat() {
    if (PIPELINE == null) return null;
    try {
      Class<?> shaders = resolve("me.cortex.voxy.client.compat.create.DistantShaders");
      return MethodHandles.publicLookup().findStatic(shaders, "forCopycatPipeline",
          MethodType.methodType(resolve("me.cortex.voxy.client.core.gl.shader.Shader"),
              PIPELINE, boolean.class));
    } catch (Throwable absent) {
      return null;
    }
  }

  private static MethodHandle staticVoid(String name) {
    try {
      Class<?> shaders = resolve("me.cortex.voxy.client.compat.create.DistantShaders");
      return MethodHandles.publicLookup()
          .findStatic(shaders, name, MethodType.methodType(void.class));
    } catch (Throwable absent) {
      return null;
    }
  }

  private static MethodHandle uploadTransform() {
    try {
      Class<?> shaders = resolve("me.cortex.voxy.client.compat.create.DistantShaders");
      return MethodHandles.publicLookup().findStatic(shaders, "uploadTransform",
          MethodType.methodType(void.class, Matrix4f.class));
    } catch (Throwable absent) {
      return null;
    }
  }

  private static Method shaderBind() {
    try {
      return resolve("me.cortex.voxy.client.core.gl.shader.Shader").getMethod("bind");
    } catch (Throwable absent) {
      return null;
    }
  }

  private static MethodHandle meshCall(String name) {
    try {
      return MethodHandles.publicLookup().findVirtual(
          resolve("me.cortex.voxy.client.compat.create.DistantMesh"), name,
          MethodType.methodType(void.class));
    } catch (Throwable absent) {
      return null;
    }
  }

  /** One road's uploaded mesh plus the handle needed to release it. */
  private record Batch(Object mesh) {}

  private final Map<UUID, Batch> batches = new HashMap<>();
  private final Set<UUID> skipped = new HashSet<>();
  private long revision = Long.MIN_VALUE;
  private int reported = -1;
  private boolean broken;
  private boolean probed;
  private static int structureParts;
  private static int drawnParts;
  /** Visible fixtures per road; beyond this the mesh grows faster than the eye gains. */
  private static final int MAX_STRUCTURE_PARTS = 800;

  public void onResourceReload() { clear(); }

  public void render(Object pipeline, Object viewport, int depthFunc) {
    if (!probed) {
      probed = true;
      System.getLogger("SplineRoads").log(System.Logger.Level.INFO,
          "[SR] voxy mesh wiring: setup=" + (SETUP != null) + " mvp=" + (MVP != null)
              + " builder=" + (BUILDER_NEW != null) + " vertex=" + (BUILDER_RAW_VERTEX != null)
              + " build=" + (BUILDER_BUILD != null) + " shader=" + (SHADER_FOR_COPYCAT != null)
              + " textures=" + (BIND_TEXTURES != null) + " transform=" + (UPLOAD_TRANSFORM != null)
              + " draw=" + (MESH_DRAW != null));
    }
    if (broken || SETUP == null || MVP == null || BUILDER_NEW == null
        || BUILDER_RAW_VERTEX == null || BUILDER_BUILD == null || SHADER_FOR_COPYCAT == null
        || BIND_TEXTURES == null || UPLOAD_TRANSFORM == null || MESH_DRAW == null
        || SHADER_BIND == null) {
      return;
    }
    if (!RoadVoxyBridge.enabled()
        || !com.sora.splineroads.config.RoadClientConfig.VOXY_DISTANT_ROADS.get()) return;
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null || minecraft.player == null) return;
    long current = ClientRoads.visibilityRevision();
    if (current != revision) {
      revision = current;
      clear();
      skipped.clear();
    }
    ensureGeometry(viewport);
    if (batches.isEmpty()) return;
    try {
      Matrix4f transform = new Matrix4f((Matrix4f) MVP.get(viewport)).translate(
          (float) -CAMERA_X.getDouble(viewport),
          (float) -CAMERA_Y.getDouble(viewport),
          (float) -CAMERA_Z.getDouble(viewport));
      SETUP.invoke(pipeline, viewport);
      Object shader = SHADER_FOR_COPYCAT.invoke(pipeline, false);
      SHADER_BIND.invoke(shader);
      BIND_TEXTURES.invoke();
      UPLOAD_TRANSFORM.invoke(transform);
      GL45C.glEnable(GL45C.GL_DEPTH_TEST);
      GL45C.glDepthFunc(depthFunc);
      GL45C.glDepthMask(true);
      GL45C.glDisable(GL45C.GL_BLEND);
      GL45C.glDisable(GL45C.GL_CULL_FACE);
      GL45C.glEnable(GL45C.GL_POLYGON_OFFSET_FILL);
      GL45C.glPolygonOffset(-4f, -4f);
      for (Batch batch : batches.values()) MESH_DRAW.invoke(batch.mesh());
    } catch (Throwable failure) {
      broken = true;
      clear();
      System.getLogger("SplineRoads").log(System.Logger.Level.ERROR,
          "[SR] Voxy distant roads disabled: " + failure, failure);
    } finally {
      GL45C.glBindVertexArray(0);
      org.lwjgl.opengl.GL20C.glUseProgram(0);
      GL45C.glPolygonOffset(0f, 0f);
      GL45C.glDisable(GL45C.GL_POLYGON_OFFSET_FILL);
      GL45C.glDepthMask(true);
    }
  }

  private void ensureGeometry(Object viewport) {
    if (broken || CAMERA_X == null) return;
    double cameraX, cameraY, cameraZ;
    try {
      cameraX = CAMERA_X.getDouble(viewport);
      cameraY = CAMERA_Y.getDouble(viewport);
      cameraZ = CAMERA_Z.getDouble(viewport);
    } catch (Throwable unavailable) {
      return;
    }
    var eye = new com.sora.splineroads.core.RoadGeometry.V(cameraX, cameraY, cameraZ);
    double near = Math.max(64.0,
        Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0) + 32.0;
    try (var budget = com.sora.splineroads.core.RoadPlanningBudget.open("SR distant LOD", 0.008)) {
      for (var built : ClientRoads.INDEX.roads.values()) {
        var id = built.record.id();
        if (batches.containsKey(id) || skipped.contains(id)) continue;
        var samples = built.mesh.samples();
        if (samples.size() < 2) { skipped.add(id); continue; }
        boolean anyFar = false;
        for (var sample : samples) {
          if (sample.center().sub(eye).horizontalLength() >= near) { anyFar = true; break; }
        }
        if (!anyFar) { skipped.add(id); continue; }
        try {
          var geometry = com.sora.splineroads.core.RoadSurface.build(built.mesh, List.of(), List.of());
          Object mesh = build(built, geometry, eye, near);
          if (mesh != null) batches.put(id, new Batch(mesh)); else skipped.add(id);
        } catch (com.sora.splineroads.core.RoadPlanningBudget.Expired more) {
          throw more;
        } catch (Throwable failure) {
          skipped.add(id);
        }
      }
    } catch (com.sora.splineroads.core.RoadPlanningBudget.Expired more) {
      // keep going next frame
    }
    if (batches.size() != reported) {
      reported = batches.size();
      System.getLogger("SplineRoads").log(System.Logger.Level.INFO,
          "[SR] voxy distant roads: " + batches.size() + " roads, near=" + (int) near
              + ", skipped=" + skipped.size() + ", planned=" + structureParts + ", drawn=" + drawnParts);
    }
  }

  private static Object build(com.sora.splineroads.world.RoadIndex.Built built,
      com.sora.splineroads.core.RoadSurface.Geometry geometry,
      com.sora.splineroads.core.RoadGeometry.V eye, double near) throws Throwable {
    Object builder = BUILDER_NEW.invoke();
    var sprite = asphaltSprite();
    ribbon(builder, built.mesh.samples(), eye, near, sprite);
    var nearSq = near * near;
    // Real marking faces from RoadSurface.build, shifted as a whole onto the deck. Their y base is
    // offset from the samples (the first vertex sat at y=-60), so one translation is applied to the
    // batch; flattening every vertex to a single height destroyed their shape into stripes.
    var marks = geometry.markings();
    double yOffset = 0;
    var deckSamples = built.mesh.samples();
    if (!marks.isEmpty() && !marks.get(0).points().isEmpty() && !deckSamples.isEmpty()) {
      yOffset = deckSamples.get(deckSamples.size() / 2).center().y()
          - marks.get(0).points().get(0).y();
    }
    for (var face : marks) appendFace(builder, face, whiteSprite(), yOffset);
    // Structures (gantries, lamps, railings, piers) are empty block models, so Voxy cannot bake
    // them; their parts are drawn here as boxes in the same quad layout.
    try {
      var ownId = built.record.id();
      // Same ground implementation the near renderer uses, so fixtures that terrain or a
      // neighbouring road rules out are ruled out here too.
      var nearby = new ArrayList<com.sora.splineroads.core.RoadGeometry.Mesh>();
      for (var entry : ClientRoads.INDEX.roads.entrySet()) {
        if (entry.getValue().record.id().equals(ownId)) continue;
        var other = entry.getValue().mesh;
        if (other.max().x() < built.mesh.min().x() - 96
            || other.min().x() > built.mesh.max().x() + 96
            || other.max().z() < built.mesh.min().z() - 96
            || other.min().z() > built.mesh.max().z() + 96) {
          continue;
        }
        nearby.add(other);
      }
      // The same Ground the near renderer builds at RoadRenderer:1466, so every judgement
      // (blocked, joined, furnitureClear, railPost) comes from one implementation. Only top() is
      // guarded: it answers NaN in unloaded chunks and Part rejects a non-finite support height,
      // which made plan() return nothing and erased all fixtures.
      var level = Minecraft.getInstance().level;
      if (level == null) return null;
      var delegate = new RoadPreviewGround(level, built.mesh, nearby);
      com.sora.splineroads.core.RoadStructures.Ground ground =
          new com.sora.splineroads.core.RoadStructures.Ground() {
            @Override public double top(double x, double z, double deckY) {
              double top = delegate.top(x, z, deckY);
              if (!Double.isFinite(top) || top > deckY - 0.5) return deckY - 6.0;
              return top;
            }
            @Override public boolean joined(com.sora.splineroads.core.RoadGeometry.V point) {
              return delegate.joined(point);
            }
            @Override public boolean furnitureClear(com.sora.splineroads.core.RoadGeometry.V point) {
              return delegate.furnitureClear(point);
            }
            @Override public boolean blocked(com.sora.splineroads.core.RoadStructures.Part part) {
              return delegate.blocked(part);
            }
            @Override public boolean lampBlocked(com.sora.splineroads.core.RoadStructures.Part part,
                com.sora.splineroads.core.RoadGeometry.Sample mount) {
              return delegate.lampBlocked(part, mount);
            }
            @Override public boolean railPost(com.sora.splineroads.core.RoadGeometry.V position,
                boolean highway, boolean raised) {
              return delegate.railPost(position, highway, raised);
            }
          };
      // Structures are planned on the server and stored on the record; the near renderer draws
      // that saved list (StructurePlanner:489 reads record.structures()). Re-running plan() here
      // produced a different set — including railings on roads that have none.
      var parts = built.record.structures();
      if (parts.isEmpty()) {
        com.sora.splineroads.core.RoadStructures.Ground fallback =
            new com.sora.splineroads.core.RoadStructures.Ground() {
              @Override public double top(double x, double z, double deckY) { return deckY - 6.0; }
              @Override public boolean joined(com.sora.splineroads.core.RoadGeometry.V point) { return false; }
              @Override public boolean blocked(com.sora.splineroads.core.RoadStructures.Part part) { return false; }
            };
        parts = com.sora.splineroads.core.RoadStructures.plan(built.mesh, fallback,
            com.sora.splineroads.world.FurnitureSpacing.onMesh(built.record, built.mesh));
      }      structureParts += parts.size();
      // plan() returns dense trim as well as gantries and lamps; cap the count so the mesh stays
      // uploadable, keeping the parts closest to the eye first.
      int budget = MAX_STRUCTURE_PARTS;
      for (var part : parts) {
        if (budget <= 0) break;
        if (!near(part, eye, near)) continue;
        appendPart(builder, part, sprite);
        budget--;
        drawnParts++;
      }
    } catch (Throwable ignored) {
    }
    if ((boolean) BUILDER_EMPTY.invoke(builder)) return null;
    return BUILDER_BUILD.invoke(builder);
  }

  private static void ribbon(Object builder,
      List<com.sora.splineroads.core.RoadGeometry.Sample> samples,
      com.sora.splineroads.core.RoadGeometry.V eye, double near,
      TextureAtlasSprite sprite) throws Throwable {
    float shade = RoadVoxyBridge.pavementShade();
    for (int i = 1; i < samples.size(); i++) {
      var previous = samples.get(i - 1);
      var current = samples.get(i);
      double dx = current.center().x() - eye.x(), dz = current.center().z() - eye.z();
      if (dx * dx + dz * dz < near * near) continue;
      var corners = new com.sora.splineroads.core.RoadGeometry.V[] {
          offset(previous, -1), offset(current, -1), offset(current, 1), offset(previous, 1)};
      double[] along = {previous.distance() / TEXTURE_TILES, current.distance() / TEXTURE_TILES,
          current.distance() / TEXTURE_TILES, previous.distance() / TEXTURE_TILES};
      double[] across = {-previous.halfWidth() / TEXTURE_TILES, -current.halfWidth() / TEXTURE_TILES,
          current.halfWidth() / TEXTURE_TILES, previous.halfWidth() / TEXTURE_TILES};
      // DistantMesh stores quads, so each segment contributes exactly four vertices.
      for (int corner = 0; corner < 4; corner++) {
        var p = corners[corner];
        vertex(builder, p.x(), p.y(), p.z(), along[corner], across[corner], shade, 0xffffff, sprite, false);
      }
    }
  }

  private static void appendFace(Object builder,
      com.sora.splineroads.core.RoadSurface.Face face, TextureAtlasSprite sprite, double yOffset) throws Throwable {
    var points = face.points();
    int argb = face.color();
    float r = ((argb >> 16) & 0xff) / 255f * 0.95f;
    float g = ((argb >> 8) & 0xff) / 255f * 0.95f;
    float b = (argb & 0xff) / 255f * 0.95f;
    // Fan triangulation, each triangle padded to a quad: the raw marking polygons are clipped and
    // often non-convex, so joining four of their points into one quad self-intersects into stripes.
    int tint = tint(r, g, b);
    var first = points.get(0);
    for (int i = 1; i + 1 < points.size(); i++) {
      var mid = points.get(i);
      var last = points.get(i + 1);
      for (var p : new com.sora.splineroads.core.RoadGeometry.V[] {first, mid, last, last}) {
        vertex(builder, p.x(), p.y() + yOffset + MARKING_LIFT, p.z(), 0.5, 0.5, 1f, tint, whiteSprite(), false);
      }
    }
  }

  /** Fixture colour per material, mirroring the table RoadStructures uses for its own faces. */
  private static int materialColor(com.sora.splineroads.core.RoadStructures.Material material) {
    return switch (material) {
      case DEFAULT -> 0xD2D8D9;
      case STEEL, GANTRY_FRAME -> 0xC3CDCF;
      case DARK_STEEL -> 0x697C82;
      case TUNNEL -> 0xC7CCC7;
      case CONCRETE -> 0xB9B9AD;
      case ARCH_STEEL -> 0xAC4E38;
      case SOIL -> 0x66543E;
      case GREEN -> 0x73AA51;
      case LAMP -> 0xFFF1C7;
      case SIGN_GREEN -> 0x14734A;
      case SIGN_BLUE, LAMP_BLUE -> 0x235994;
      case SIGN_WHITE -> 0xF4F3DC;
      case TACTILE -> 0xE9BE42;
      case SIGNAL_MAIN, SIGNAL_RAMP, SIGNAL_VEHICLE, SIGNAL_PEDESTRIAN, SIGNAL_LEFT -> 0x181D20;
      case CB_POST, CB_ARM, CB_BASE -> 0xE7E7E7;
      case FLOWER_PINK -> 0xE783B2;
      case FLOWER_YELLOW -> 0xF0C73D;
      case FLOWER_WHITE -> 0xFFF8EB;
      case OAK_LEAVES -> 0x6A9D46;
      case BIRCH_LEAVES -> 0x80A755;
      case SPRUCE_LEAVES -> 0x4C7455;
      default -> 0xB4B4B4;
    };
  }  /**
   * True when a fixture's centre falls inside another road's carriageway.
   *
   * <p>Cheap by construction: an AABB reject per candidate road, then at most 64 sampled points
   * along it. That is enough to drop the duplicate railings that appear inside a junction while
   * leaving everything else alone.
   */
  private static boolean insideNeighbourRoad(com.sora.splineroads.core.RoadGeometry.V point,
      java.util.UUID ownId) {
    double cx = point.x(), cz = point.z();
    for (var entry : ClientRoads.INDEX.roads.entrySet()) {
      if (entry.getValue().record.id().equals(ownId)) continue;
      var other = entry.getValue().mesh;
      if (cx < other.min().x() - 1 || cx > other.max().x() + 1
          || cz < other.min().z() - 1 || cz > other.max().z() + 1) {
        continue;
      }
      var samples = other.samples();
      int step = Math.max(1, samples.size() / 64);
      for (int i = 0; i < samples.size(); i += step) {
        var sample = samples.get(i);
        double dx = sample.center().x() - cx, dz = sample.center().z() - cz;
        double hw = sample.halfWidth() - 0.35;
        if (dx * dx + dz * dz < hw * hw) return true;
      }
    }
    return false;
  }
  /** True when a point lies inside another road's carriageway. */
  private static boolean near(com.sora.splineroads.core.RoadStructures.Part part,
      com.sora.splineroads.core.RoadGeometry.V eye, double near) {
    var a = part.a();
    var b = part.b();
    double ax = a.x() - eye.x(), az = a.z() - eye.z();
    double bx = b.x() - eye.x(), bz = b.z() - eye.z();
    double nearSq = near * near;
    return ax * ax + az * az >= nearSq || bx * bx + bz * bz >= nearSq;
  }

  /**
   * Draws one structural part.
   *
   * <p>{@code RoadStructures.Part} is not a tube from {@code a} to {@code b}: {@code a鈫抌} is the
   * horizontal base line whose length already equals {@code width}, and {@code height} extends
   * upward from it. Reading it as an axis made every post collapse into a short lying box.
   */
  private static void appendPart(Object builder,
      com.sora.splineroads.core.RoadStructures.Part part, TextureAtlasSprite sprite) throws Throwable {
    var a = part.a();
    var b = part.b();
    double dx = b.x() - a.x(), dz = b.z() - a.z();
    double length = Math.sqrt(dx * dx + dz * dz);
    double halfWidth = part.width() / 2;
    double nx, nz;
    if (length > 1e-6) {
      nx = -dz / length * halfWidth;
      nz = dx / length * halfWidth;
    } else {
      nx = halfWidth;
      nz = 0;
    }
    double height = part.height();
    double[] xs = {a.x() - nx, b.x() - nx, b.x() + nx, a.x() + nx};
    double[] ys = {a.y(), b.y(), b.y(), a.y()};
    double[] zs = {a.z() - nz, b.z() - nz, b.z() + nz, a.z() + nz};
    int tint = materialColor(part.material());
    // Bottom ring 0..3, top ring 4..7.
    int[][] faces = {
        {0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {1, 2, 6, 5}, {2, 3, 7, 6}, {3, 0, 4, 7}};
    for (int[] face : faces) {
      for (int corner : face) {
        boolean top = corner >= 4;
        int i = corner & 3;
        vertex(builder, xs[i], ys[i] + (top ? height : 0), zs[i], 0.5, 0.5, 1f, tint, sprite, part.luminous());
      }
    }
  }

  /**
   * One vertex in Voxy's layout. Sky and block light are the inputs its shader feeds into the
   * lightmap sampler, which is where distance brightness comes from.
   */
  private static void vertex(Object builder, double x, double y, double z,
      double u, double v, float shade, int tintRgb, TextureAtlasSprite sprite, boolean bright) throws Throwable {
    float tu = sprite == null ? 0f : sprite.getU((float) (u - Math.floor(u)));
    float tv = sprite == null ? 0f : sprite.getV((float) (v - Math.floor(v)));
    int[] light = bright ? new int[] {15, 15} : lightAt(x, y, z);
    BUILDER_RAW_VERTEX.invoke(builder,
        (float) x, (float) y, (float) z, tu, tv, light[0], light[1],
        shade, FACE_UP, tintRgb);
  }

  private static int tint(float r, float g, float b) {
    int ri = Math.min(255, Math.round(r * 255));
    int gi = Math.min(255, Math.round(g * 255));
    int bi = Math.min(255, Math.round(b * 255));
    return (ri << 16) | (gi << 8) | bi;
  }

  /** Sky and block light in 0..15. Unloaded chunks fall back to full sky, which is what
   * Voxy stores for its own voxels; the day/night curve is applied by the lightmap texture. */
  private static int[] lightAt(double x, double y, double z) {
    var level = Minecraft.getInstance().level;
    if (level == null) return new int[] {15, 0};
    var pos = net.minecraft.core.BlockPos.containing(x, y + 0.1, z);
    if (!level.hasChunkAt(pos)) return new int[] {15, 0};
    return new int[] {
        level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos),
        level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos)};
  }

  private static com.sora.splineroads.core.RoadGeometry.V offset(
      com.sora.splineroads.core.RoadGeometry.Sample sample, int side) {
    return sample.center().add(sample.left().mul(side * sample.halfWidth()));
  }

  private static TextureAtlasSprite asphaltSprite() { return sprite("block/terrain_asphalt"); }

  /** Lane paint samples this white sheet, so the tint decides the colour instead of asphalt. */
  /** Opaque white sheet so lane paint takes its colour from the tint, not from asphalt. */
  private static TextureAtlasSprite whiteSprite() {
    var atlas = Minecraft.getInstance()
        .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
    return atlas.apply(net.minecraft.resources.ResourceLocation.withDefaultNamespace(
        "block/white_concrete"));
  }

  private static TextureAtlasSprite sprite(String path) {
    var atlas = Minecraft.getInstance()
        .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
    return atlas.apply(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
        com.sora.splineroads.SplineRoads.ID, path));
  }

  private static boolean far(com.sora.splineroads.core.RoadSurface.Face face,
      com.sora.splineroads.core.RoadGeometry.V eye, double nearSq) {
    var points = face.points();
    if (points.isEmpty()) return false;
    double x = 0, z = 0;
    for (var p : points) { x += p.x(); z += p.z(); }
    int count = points.size();
    double dx = x / count - eye.x(), dz = z / count - eye.z();
    return dx * dx + dz * dz >= nearSq;
  }

  private void clear() {
    for (Batch batch : batches.values()) {
      try {
        MESH_FREE.invoke(batch.mesh());
      } catch (Throwable ignored) {
      }
    }
    batches.clear();
  }
}
