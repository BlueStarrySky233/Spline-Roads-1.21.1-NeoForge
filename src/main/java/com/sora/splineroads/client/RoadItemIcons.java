package com.sora.splineroads.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sora.splineroads.SplineRoads;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Draws a dedicated 2D icon for tool items in the inventory instead of their in-world model.
 *
 * <p>Upstream overrode {@code BakedModel.applyTransform(ItemDisplayContext, ...)}. 1.21.1
 * replaced that with {@code getTransforms()}, which can only change the transform, never the
 * geometry. The equivalent here is a client item extension whose custom renderer picks the flat
 * icon model for {@link ItemDisplayContext#GUI} and the ordinary item model everywhere else.
 */
@EventBusSubscriber(modid = SplineRoads.ID, value = Dist.CLIENT)
public final class RoadItemIcons {
  private static final List<String> ITEMS = List.of(
      "elevated_access_creator", "endpoint_creator", "frontage_generator", "gantry_creator",
      "gantry_editor", "interchange_connector", "interchange_connector_3", "interchange_connector_5",
      "interchange_connector_6", "junction_connector", "junction_point_creator", "lane_line_editor",
      "lane_point_tool", "layered_connector", "railbed_connector", "ramp_connector", "road_connector",
      "road_node", "road_remover", "road_splitter", "roundabout_generator", "text_editor",
      "toll_plaza_generator", "y_junction_editor");

  private static ResourceLocation icon(String item) {
    return ResourceLocation.fromNamespaceAndPath(SplineRoads.ID, "item/gui/" + item);
  }

  @SubscribeEvent
  public static void registerAdditional(ModelEvent.RegisterAdditional event) {
    for (var item : ITEMS) event.register(ModelResourceLocation.standalone(icon(item)));
  }

  @SubscribeEvent
  public static void registerExtensions(RegisterClientExtensionsEvent event) {
    for (var holder : SplineRoads.ITEMS.getEntries()) {
      var name = holder.getId().getPath();
      if (!ITEMS.contains(name)) continue;
      event.registerItem(new IconExtensions(icon(name)), holder.get());
    }
  }

  private record IconExtensions(ResourceLocation flat) implements IClientItemExtensions {
    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
      return new IconRenderer(flat);
    }
  }

  private static final class IconRenderer extends BlockEntityWithoutLevelRenderer {
    private final ResourceLocation flat;

    IconRenderer(ResourceLocation flat) {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
          Minecraft.getInstance().getEntityModels());
      this.flat = flat;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
        MultiBufferSource buffer, int light, int overlay) {
      var mc = Minecraft.getInstance();
      var renderer = mc.getItemRenderer();
      // getItemModelShaper bypasses the custom renderer, so the in-world path cannot recurse.
      var model = context == ItemDisplayContext.GUI
          ? mc.getModelManager().getModel(ModelResourceLocation.standalone(flat))
          : renderer.getItemModelShaper().getItemModel(stack);
      renderer.render(stack, context, false, pose, buffer, light, overlay, model);
    }
  }

  private RoadItemIcons() {}
}
