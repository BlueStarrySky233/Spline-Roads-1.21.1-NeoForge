package com.sora.splineroads.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.sora.splineroads.SplineRoads;
import com.sora.splineroads.config.RoadClientConfig;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import static net.minecraft.commands.Commands.*;

/** Local display preferences are available without operator rights on any server. */
@EventBusSubscriber(modid=SplineRoads.ID,value=Dist.CLIENT)
public final class RoadClientCommands {
  @SubscribeEvent public static void register(RegisterClientCommandsEvent event){
    var perf=literal("perf").executes(c->perf(c.getSource(),!RoadClientConfig.PERF_OVERLAY.get()));
    perf.then(literal("on").executes(c->perf(c.getSource(),true)));
    perf.then(literal("off").executes(c->perf(c.getSource(),false)));
    event.getDispatcher().register(literal("sr")
        .then(literal("previewdepth").executes(c->previewDepth(c.getSource(),!RoadClientConfig.PREVIEW_DEPTH_TEST.get()))
          .then(literal("on").executes(c->previewDepth(c.getSource(),true)))
          .then(literal("off").executes(c->previewDepth(c.getSource(),false))))
        .then(perf)
        .then(literal("signs").executes(c->signs(c.getSource(),!RoadSignVisibility.enabled()))
          .then(literal("on").executes(c->signs(c.getSource(),true)))
          .then(literal("off").executes(c->signs(c.getSource(),false))))
        .then(literal("voxy").executes(c->voxy(c.getSource(),!RoadVoxyBridge.enabled()))
          .then(literal("on").executes(c->voxy(c.getSource(),true)))
          .then(literal("off").executes(c->voxy(c.getSource(),false))))
        .then(literal("voxyshade")
          .then(argument("value",com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0f,6f))
            .executes(c->voxyShade(c.getSource(),com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c,"value")))))
        .then(literal("voxyfloor")
          .then(argument("value",com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0f,6f))
            .executes(c->voxyFloor(c.getSource(),com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c,"value")))))
        .then(literal("voxylight")
          .then(argument("value",com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0f,6f))
            .executes(c->voxyLight(c.getSource(),com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c,"value")))))
        .then(literal("sf").executes(c->{
          c.getSource().sendSuccess(()->Component.literal("SR 表面模式："+RoadClientConfig.SURFACE_BACKEND.get()+"；/sr sf auto|VBO|terrain"),false);return 1;
        }).then(argument("mode",StringArgumentType.word())
            .suggests((c,b)->SharedSuggestionProvider.suggest(List.of("auto","VBO","terrain"),b))
            .executes(c->{
              String name=StringArgumentType.getString(c,"mode").toUpperCase(Locale.ROOT);
              if(name.equals("TERRIAN"))name="TERRAIN";
              RoadClientConfig.SurfaceBackend mode;
              try{mode=RoadClientConfig.SurfaceBackend.valueOf(name);}
              catch(IllegalArgumentException e){throw new SimpleCommandExceptionType(Component.literal("mode 可选：auto、VBO、terrain")).create();}
              RoadClientConfig.SURFACE_BACKEND.set(mode); // Forge's file config autosaves.
              // The existing frame transition retains valid caches and dirties affected sections.
              RoadTerrainModels.beginFrame();
              c.getSource().sendSuccess(()->Component.literal("SR 表面模式已切换为 "+mode),false);return 1;
            }))));
  }
  private static int signs(net.minecraft.commands.CommandSourceStack source,boolean enabled){
    RoadSignVisibility.set(enabled);source.sendSuccess(()->Component.literal("SR 全部标牌已"+(enabled?"显示":"隐藏（性能排查）")+"；/sr signs on|off"),false);return 1;
  }
  private static int previewDepth(net.minecraft.commands.CommandSourceStack source,boolean enabled){
    RoadClientConfig.PREVIEW_DEPTH_TEST.set(enabled);
    source.sendSuccess(()->Component.literal("SR 建造预览深度测试已"+(enabled?"开启（会被地形遮挡）":"关闭（透过地形显示）")+"；/sr previewdepth on|off"),false);return 1;
  }
  private static int voxy(net.minecraft.commands.CommandSourceStack source,boolean enable){
    RoadVoxyBridge.setEnabled(enable);
    source.sendSuccess(()->Component.literal("SR 远景道路几何已"+(enable?"开启":"关闭")
        +"（钩子状态："+RoadVoxyBridge.state()+"）；/sr voxy on|off"),false);return 1;
  }
  private static int voxyShade(net.minecraft.commands.CommandSourceStack source,float value){
    RoadVoxyBridge.setPavementShade(value);
    source.sendSuccess(()->Component.literal("SR 远景路面亮度 = "+value
        +"（0~6，重建远景后生效）；/sr voxyshade <值>"),false);return 1;
  }
  private static int voxyFloor(net.minecraft.commands.CommandSourceStack source,float value){
    RoadVoxyBridge.setLightFloor(value);
    source.sendSuccess(()->Component.literal("SR 远景底光 = "+value
        +"（0~6，越小夜里越黑）；/sr voxyfloor <值>"),false);return 1;
  }
  private static int voxyLight(net.minecraft.commands.CommandSourceStack source,float value){
    RoadVoxyBridge.setLightGain(value);
    source.sendSuccess(()->Component.literal("SR 远景光照响应 = "+value
        +"（0~6，越大明暗差越强）；/sr voxylight <值>"),false);return 1;
  }
  private static int perf(net.minecraft.commands.CommandSourceStack source,boolean enabled){
    RoadClientConfig.PERF_OVERLAY.set(enabled); // Forge's file config autosaves.
    source.sendSuccess(()->Component.literal("SR 性能显示已"+(enabled?"开启":"关闭")),false);return 1;
  }
  private RoadClientCommands(){}
}
