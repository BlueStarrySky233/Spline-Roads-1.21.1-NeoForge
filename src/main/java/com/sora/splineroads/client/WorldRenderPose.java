package com.sora.splineroads.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * World-space pose helper for {@link RenderLevelStageEvent} handlers.
 *
 * <p>1.20.1 applied the camera rotation to the pose stack inside {@code LevelRenderer.renderLevel},
 * so the event pose stack already mapped world space into view space. 1.21.1 moved that
 * multiplication out of the level renderer: {@code renderLevel} now receives it as a separate
 * model-view matrix, and the pose stack handed to the event only carries per-object translations.
 *
 * <p>Drawing world geometry with the raw event pose stack therefore omits the camera rotation and
 * the geometry ends up in view space, appearing glued to the camera instead of sitting in the world.
 * Callers must apply {@link RenderLevelStageEvent#getModelViewMatrix()} first, which this helper does.
 */
public final class WorldRenderPose {
  public static PoseStack of(RenderLevelStageEvent event) {
    PoseStack pose = new PoseStack();
    pose.mulPose(event.getModelViewMatrix());
    return pose;
  }

  private WorldRenderPose() {}
}
