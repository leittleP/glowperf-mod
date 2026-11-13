package dev.qwen.glowperf.fabric;

import dev.qwen.glowperf.GlowPerfMod;
import dev.qwen.glowperf.glow.config.GlowConfig;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.perf.PerfCommon;
import dev.qwen.glowperf.perf.client.map.MapEvents;
import dev.qwen.glowperf.perf.config.PerfConfig;
import dev.qwen.glowperf.perf.core.PerfState;
import dev.qwen.glowperf.platform.JsonConfigSupport;
import dev.qwen.glowperf.platform.LoaderPlatform;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class GlowPerfFabric implements ModInitializer {
   @Override
   public void onInitialize() {
      FabricLoader loader = FabricLoader.getInstance();
      LoaderPlatform.initialize(new LoaderPlatform.Provider() {
         @Override
         public boolean isModLoaded(String modId) {
            return loader.isModLoaded(modId);
         }

         @Override
         public int loadedModCount() {
            return loader.getAllMods().size();
         }

         @Override
         public Path configDirectory() {
            return loader.getConfigDir();
         }
      });

      Path configDirectory = LoaderPlatform.configDirectory();
      JsonConfigSupport.bind(GlowConfig.SPEC, configDirectory.resolve("glowperf-client.json"), GlowState::readFromConfig);
      JsonConfigSupport.bind(PerfConfig.SPEC, configDirectory.resolve("glowperf-common.json"), PerfState::readFromConfig);
      GlowPerfMod.initialize();

      ServerTickEvents.START_SERVER_TICK.register(server -> PerfCommon.onServerTickStart());
      ServerTickEvents.END_SERVER_TICK.register(PerfCommon::onServerTickEnd);
      ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> PerfCommon.onEntityLoad(entity));
      ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> PerfCommon.onEntityLeave(entity));
      ServerWorldEvents.UNLOAD.register((server, level) -> PerfCommon.onLevelUnload());
      ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> MapEvents.onLogin(handler.player));
   }
}
