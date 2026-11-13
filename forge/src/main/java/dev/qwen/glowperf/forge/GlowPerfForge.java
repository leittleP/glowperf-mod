package dev.qwen.glowperf.forge;

import dev.qwen.glowperf.GlowPerfMod;
import dev.qwen.glowperf.glow.config.GlowConfig;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.perf.config.PerfConfig;
import dev.qwen.glowperf.perf.core.PerfState;
import dev.qwen.glowperf.platform.LoaderPlatform;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;

@Mod("glowperf")
public final class GlowPerfForge {
   public GlowPerfForge() {
      LoaderPlatform.initialize(new FmlLoaderPlatform());
      new FmlConfigBridge(Type.CLIENT, GlowConfig.SPEC, GlowState::readFromConfig);
      new FmlConfigBridge(Type.COMMON, PerfConfig.SPEC, PerfState::readFromConfig);
      GlowPerfMod.initialize();
   }
}
