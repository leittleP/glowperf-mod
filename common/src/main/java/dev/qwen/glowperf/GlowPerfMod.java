package dev.qwen.glowperf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlowPerfMod {
   public static final String MODID = "glowperf";
   public static final Logger LOG = LoggerFactory.getLogger("GlowPerf");

   private GlowPerfMod() {
   }

   public static void initialize() {
      LOG.info("光影性能核心已加载：/light and shadow 控制光影，/performance optimization 调优，/perf help 查看面板指令，/gp scan 一键体检");
   }
}
