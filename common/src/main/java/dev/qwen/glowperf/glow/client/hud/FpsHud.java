package dev.qwen.glowperf.glow.client.hud;

import dev.qwen.glowperf.glow.client.ui.VrTheme;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.glow.render.FpsSampler;
import dev.qwen.glowperf.glow.render.GlowRenderer;
import dev.qwen.glowperf.glow.render.Tier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class FpsHud {
   public static final FpsHud INSTANCE = new FpsHud();
   private static String line1 = "-- fps";
   private static String line2 = "";
   private static String line3 = "";
   private static String line4 = "";
   private static int counter;
   private static float ratioCache;

   private FpsHud() {
   }

   public void render(GuiGraphics g, float partialTick, int width, int height) {
      if (GlowState.fpsHud && GlowState.glAvailable) {
         if (!GlowState.hudSuppressed) {
            Minecraft mc = Minecraft.getInstance();
            if (!mc.options.hideGui) {
               if (mc.level != null) {
                  if (++counter >= 8) {
                     counter = 0;
                     this.refresh();
                  }

                  int x = Math.max(2, Math.min(GlowState.hudX, width - 40));
                  int y = Math.max(2, Math.min(GlowState.hudY, height - 16));
                  if (GlowState.hudCompact) {
                     int w = mc.font.width(line1) + 12;
                     VrTheme.panel(g, x - 4, y - 3, w, 14, -1291383016);
                     g.drawString(mc.font, line1, x + 2, y + 1, this.fpsColor());
                  } else {
                     int w = 128;
                     int h = 62;
                     VrTheme.panel(g, x - 6, y - 6, w, h);
                     VrTheme.scanline(g, x - 6, y - 6, w, h);
                     g.drawString(mc.font, line1, x, y - 1, this.fpsColor());
                     VrTheme.meter(g, x, y + 11, w - 16, 4, ratioCache, ratioCache > 0.9F ? -38022 : (ratioCache > 0.65F ? -15253 : -11010128));
                     g.drawString(mc.font, line2, x, y + 19, -2689537);
                     g.drawString(mc.font, line3, x, y + 30, -7358785);
                     g.drawString(mc.font, line4, x, y + 41, GlowState.enabled ? -13049345 : -38022);
                  }
               }
            }
         }
      }
   }

   private void refresh() {
      float fps = FpsSampler.fps();
      float p95 = FpsSampler.p95Ms();
      ratioCache = Math.max(0.0F, Math.min(1.0F, p95 / Math.max(4.0F, GlowState.budgetMs * 2.0F)));
      Tier t = GlowState.activeTier;
      line1 = String.format("%.0f fps  %.1f ms", fps, FpsSampler.avgMs());
      line2 = String.format("P95 %.1f ms  预算 %.0f ms", p95, GlowState.budgetMs);
      line3 = String.format(
         "档位 %s  发光物 %d  模糊 %d 遍", GlowState.tierMode == Tier.AUTO ? "AUTO→" + t.name() : t.name(), GlowState.lastEmitterCount, GlowState.lastBlurPasses
      );
      if (!GlowState.glAvailable) {
         line4 = "GL 不可用 · 已停用";
      } else if (GlowState.bloomActive()) {
         line4 = String.format("管线 %.2f ms  遮罩 %dx%d", GlowState.lastPipelineMs, GlowRenderer.targetWidth(), GlowRenderer.targetHeight());
      } else {
         line4 = GlowState.enabled ? "柔光模式（无离屏管线）" : "已关闭 · /light and shadow open";
      }
   }

   private int fpsColor() {
      float fps = FpsSampler.fps();
      if (fps >= 60.0F) {
         return -11010128;
      } else {
         return fps >= 30.0F ? -15253 : -38022;
      }
   }

   public static String statsLine() {
      return String.format("%.0f fps / P95 %.1f ms", FpsSampler.fps(), FpsSampler.p95Ms());
   }

   public static void reset() {
      counter = 0;
      line1 = "-- fps";
      line2 = "";
      line3 = "";
      line4 = "";
   }
}
