package com.sora.splineroads.mixin;

import com.sora.splineroads.compat.mtr.MtrRailAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MTR rail nodes have no collision so trains can pass; once a railbed covers the cell the ground
 * must be solid again. Replaces the 1.20.1 coremod injection into the node collision shape.
 */
@Mixin(targets = "org.mtr.block.BlockNode")
public abstract class BlockNodeMixin {
  @Inject(method = "getCollisionShape", at = @At("RETURN"), cancellable = true)
  private void splineroads$collision(BlockState state, BlockGetter world, BlockPos pos,
      CollisionContext context, CallbackInfoReturnable<VoxelShape> callback) {
    VoxelShape original = callback.getReturnValue();
    Object shaped = MtrRailAccess.nodeShape(original, world, pos);
    if (shaped instanceof VoxelShape shape && shape != original) callback.setReturnValue(shape);
  }
}
