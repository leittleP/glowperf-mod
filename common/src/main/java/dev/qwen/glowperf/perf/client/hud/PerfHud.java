package dev.qwen.glowperf.perf.client.hud;

import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.perf.client.ui.VrTheme;
import dev.qwen.glowperf.perf.core.Counters;
import dev.qwen.glowperf.perf.core.FrameProbe;
import dev.qwen.glowperf.perf.core.GovernorLite;
import dev.qwen.glowperf.perf.core.MemWatch;
import dev.qwen.glowperf.perf.core.PerfState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class PerfHud {
   public static final PerfHud INSTANCE = new PerfHud();
   private static String l1 = "-- fps";
   private static String l2 = "";
   private static String l3 = "";
   private static String l4 = "";
   private static String l5 = "";
   private static int counter;
   private static float heapRatioCache;

   private PerfHud() {
   }

   public void render(GuiGraphics g, float partialTick, int width, int height) {
      if (PerfState.panel) {
         Minecraft mc = Minecraft.getInstance();
         if (!mc.options.hideGui) {
            if (++counter >= 8) {
               counter = 0;
               this.refresh();
            }

            int x = Math.max(2, Math.min(PerfState.panelX, width - 60));
            int y = Math.max(2, Math.min(PerfState.panelY, height - 18));
            if (PerfState.panelCompact) {
               int w = mc.font.width(l1) + 12;
               VrTheme.panel(g, x - 4, y - 3, w, 14, -1291383016);
               g.drawString(mc.font, l1, x + 2, y + 1, this.fpsColor());
            } else {
               int w = 150;
               int h = 76;
               VrTheme.panel(g, x - 6, y - 6, w, h);
               VrTheme.scanline(g, x - 6, y - 6, w, h);
               g.drawString(mc.font, l1, x, y - 1, this.fpsColor());
               VrTheme.meter(g, x, y + 11, w - 16, 4, this.frameRatio(), this.frameRatio() > 0.9F ? -38022 : (this.frameRatio() > 0.65F ? -15253 : -11010128));
               VrTheme.meter(g, x, y + 17, w - 16, 3, heapRatioCache, heapRatioCache > PerfState.memSoftLimit ? -38022 : -2143755777);
               g.drawString(mc.font, l2, x, y + 23, -2689537);
               g.drawString(mc.font, l3, x, y + 34, -7358785);
               g.drawString(mc.font, l4, x, y + 45, -7358785);
               g.drawString(mc.font, l5, x, y + 56, PerfState.enabled ? -13049345 : -38022);
            }
         }
      }
   }

   private void refresh() {
      PerfState.Preset eff = PerfState.effective;
      l1 = String.format("%.0f fps ｜ %.1f ms ｜ P95 %.1f", FrameProbe.fps(), FrameProbe.frameAvg(), FrameProbe.frameP95());
      l2 = String.format("TPS %.1f ｜ tick %.1f ms ｜ 长帧 %d", FrameProbe.tps(), FrameProbe.tickAvg(), Counters.longFramesTotal);
      l3 = String.format("堆 %d/%d MB (%.0f%%) ｜ 趋势 %+d MB", MemWatch.usedMb(), MemWatch.maxMb(), MemWatch.ratio() * 100.0F, MemWatch.trendMb());
      l4 = String.format("剔除实体 %d ｜ 玩家 %d ｜ 拦生成 %d ｜ 冻AI %d", Counters.shownCulled, Counters.shownPlayers, Counters.shownCapped, Counters.shownFrozen);
      l5 = String.format(
         "优化 %s%s ｜ 光影 %s",
         eff.label,
         PerfState.preset == PerfState.Preset.AUTO ? "（自动→" + eff.label + "）" : "",
         PerfState.glowMode.label()
            + (GlowState.enabled ? "/" + GlowState.activeTier.name() : "")
            + (PerfState.glowApplied == 1 ? "（柔光）" : (PerfState.glowApplied == 2 ? "（已关）" : ""))
      );
      heapRatioCache = MemWatch.ratio();
   }

   private float frameRatio() {
      return Math.max(0.0F, Math.min(1.0F, FrameProbe.frameP95() / Math.max(4.0F, PerfState.budgetMs * 2.0F)));
   }

   private int fpsColor() {
      float fps = FrameProbe.fps();
      if (fps >= 60.0F) {
         return -11010128;
      } else {
         return fps >= 30.0F ? -15253 : -38022;
      }
   }

   public static String statsLine() {
      return String.format("%.0f fps ｜ P95 %.1f ms ｜ TPS %.1f ｜ 堆 %d MB", FrameProbe.fps(), FrameProbe.frameP95(), FrameProbe.tps(), MemWatch.usedMb());
   }

   public static String autoLine() {
      return GovernorLite.describe();
   }

   public static void reset() {
      counter = 0;
      l1 = "-- fps";
      l5 = "";
      l4 = "";
      l3 = "";
      l2 = "";
   }
}
