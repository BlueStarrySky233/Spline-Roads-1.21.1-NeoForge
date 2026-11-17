package com.sora.splineroads.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * World-space pose helper for {@link RenderLevelStageEvent} handlers that submit vertices through
 * a {@code VertexBuffer} or the font renderer.
 *
 * <p>Those paths hand the pose matrix straight to the shader as {@code ModelViewMat}, so the pose
 * must carry the camera rotation: 1.21.1 moved that multiplication out of {@code LevelRenderer}, and
 * the pose stack given to the event only carries per-object translations.
 *
 * <p><strong>Do not use this for immediate-mode geometry</strong> ({@code MultiBufferSource} and
 * {@code RenderType.lines}/{@code translucent}): those shaders already apply the camera transform,
 * so multiplying {@code getModelViewMatrix()} in as well rotates the geometry twice and makes it
 * follow the camera. Immediate-mode paths keep vertices camera-relative instead: take
 * {@code event.getPoseStack()}, {@code translate(-camera)}, and emit world coordinates.
 */
public final class WorldRenderPose {
  public static PoseStack of(RenderLevelStageEvent event) {
    PoseStack pose = new PoseStack();
    pose.mulPose(event.getModelViewMatrix());
    return pose;
  }

  private WorldRenderPose() {}
}
