package com.sora.splineroads.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Render-thread scratch storage for batched geometry.
 *
 * <p>1.20.1 allocated one {@link BufferBuilder} per arena and reused it. 1.21.1 removed that
 * constructor, and {@code Tesselator} is a single shared instance: every arena that went through
 * it shared one buffer, so a preview or signal upload could discard a road batch that had not been
 * built yet, leaving the affected region permanently short of geometry.
 *
 * <p>Each batch therefore owns a private {@link ByteBufferBuilder} here. {@link MeshData#close()}
 * releases it, and {@link #discard(MeshData)} covers the paths that never upload.
 */
final class RoadUploadArena {
  private static final int INITIAL_CAPACITY = 4096;

  BufferBuilder begin() {
    return begin(DefaultVertexFormat.NEW_ENTITY);
  }

  BufferBuilder begin(VertexFormat format) {
    return new BufferBuilder(new ByteBufferBuilder(INITIAL_CAPACITY), VertexFormat.Mode.QUADS, format);
  }

  /** Byte size of a built batch, used for GPU budget accounting. */
  static long bytes(MeshData data) {
    if (data == null) return 0L;
    MeshData.DrawState state = data.drawState();
    return (long) state.vertexCount() * state.format().getVertexSize();
  }

  /** Closes a batch that will not be uploaded. */
  static void discard(MeshData data) {
    if (data != null) data.close();
  }
}
