package com.sora.splineroads.client;

import net.minecraft.client.resources.model.BakedModel;

/**
 * Seam for the optional Fabric renderer bridge used by the 1.20.1 branch.
 *
 * <p>That bridge exists so a Fabric-api renderer (Sodium/Iris via Sinytra) can clip the vanilla
 * chunk quads; NeoForge 1.21.1 has no Fabric renderer to attach to, so the seam deliberately
 * returns the plain terrain model. Keeping the type lets the terrain model code stay identical
 * to upstream and leaves one obvious place to add a NeoForge equivalent later.
 */
final class RoadFabricModels {
  /** No Fabric renderer is present, so the caller's terrain model is used unchanged. */
  static BakedModel wrap(BakedModel terrain) {
    return terrain;
  }

  /** Nothing is proxied, so unwrapping is the identity. */
  static BakedModel unwrap(BakedModel model) {
    return model;
  }

  private RoadFabricModels() {}
}
