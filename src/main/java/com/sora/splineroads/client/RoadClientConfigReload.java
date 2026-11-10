package com.sora.splineroads.client;

import com.sora.splineroads.SplineRoads;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * Client preferences use NightConfig and are written by commands and the config UI.
 *
 * <p>The 1.20.1 implementation also watched the file on disk and re-dispatched Forge's
 * {@code IConfigEvent} so an external editor would hot-reload. NeoForge 1.21.1 removed both
 * {@code IConfigEvent} and {@code ModConfig#getConfigData()}, so external file watching is dropped:
 * in-game changes still take effect immediately, and edits made outside the game apply on restart.
 */
@EventBusSubscriber(modid = SplineRoads.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RoadClientConfigReload {
  @SubscribeEvent
  public static void loaded(ModConfigEvent.Loading event) {
    // Nothing cached depends on config values at load time; kept so the listener still exists.
  }

  private RoadClientConfigReload() {}
}
