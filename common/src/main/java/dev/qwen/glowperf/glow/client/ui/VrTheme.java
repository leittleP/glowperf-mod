package dev.qwen.glowperf.glow.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class VrTheme {
   public static final int PANEL_BG = -536408296;
   public static final int PANEL_BG_SOFT = -1609951710;
   public static final int NEON = -13049345;
   public static final int NEON_DIM = -2143755777;
   public static final int NEON_FAINT = 859365887;
   public static final int ACCENT = -45149;
   public static final int GRID = 339272191;
   public static final int TEXT = -2689537;
   public static final int TEXT_DIM = -7358785;
   public static final int OK = -11010128;
   public static final int WARN = -15253;
   public static final int BAD = -38022;

   private VrTheme() {
   }

   public static void panel(GuiGraphics g, int x, int y, int w, int h) {
      g.fill(x, y, x + w, y + h, -536408296);
      grid(g, x, y, w, h);
      frame(g, x, y, w, h, -2143755777);
      g.fill(x + 1, y, x + w - 1, y + 1, -13049345);
      g.fill(x + 1, y + h - 1, x + w - 1, y + h, 859365887);
      cornerTicks(g, x, y, w, h);
   }

   public static void panel(GuiGraphics g, int x, int y, int w, int h, int background) {
      g.fill(x, y, x + w, y + h, background);
      frame(g, x, y, w, h, 859365887);
      g.fill(x + 1, y, x + w - 1, y + 1, -2143755777);
      cornerTicks(g, x, y, w, h);
   }

   private static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
      g.fill(x, y, x + 1, y + h, color);
      g.fill(x + w - 1, y, x + w, y + h, color);
      g.fill(x, y + h - 1, x + w, y + h, color);
   }

   private static void cornerTicks(GuiGraphics g, int x, int y, int w, int h) {
      int t = 4;
      g.fill(x, y, x + t, y + 1, -13049345);
      g.fill(x, y, x + 1, y + t, -13049345);
      g.fill(x + w - t, y, x + w, y + 1, -13049345);
      g.fill(x + w - 1, y, x + w, y + t, -13049345);
      g.fill(x, y + h - 1, x + t, y + h, -13049345);
      g.fill(x, y + h - t, x + 1, y + h, -13049345);
      g.fill(x + w - t, y + h - 1, x + w, y + h, -13049345);
      g.fill(x + w - 1, y + h - t, x + w, y + h, -13049345);
   }

   private static void grid(GuiGraphics g, int x, int y, int w, int h) {
      for (int gy = y + 6; gy < y + h - 3; gy += 6) {
         g.fill(x + 2, gy, x + w - 2, gy + 1, 339272191);
      }
   }

   public static void scanline(GuiGraphics g, int x, int y, int w, int h) {
      long t = System.currentTimeMillis();
      int line = y + (int)(t / 24L % (long)Math.max(1, h - 2));
      g.fill(x + 1, line, x + w - 1, line + 1, 708370943);
   }

   public static void meter(GuiGraphics g, int x, int y, int w, int h, float ratio, int color) {
      g.fill(x, y, x + w, y + h, 1073741824);
      int fw = (int)((float)w * Math.max(0.0F, Math.min(1.0F, ratio)));
      if (fw > 0) {
         g.fill(x, y, x + fw, y + h, color);
      }

      g.fill(x + fw, y, Math.min(x + fw + 1, x + w), y + h, -1);
   }

   public static void toggle(GuiGraphics g, int x, int y, boolean on, String label) {
      Minecraft mc = Minecraft.getInstance();
      g.fill(x, y, x + 22, y + 10, on ? 1715003903 : 1073741824);
      g.fill(x, y, x + 22, y + 1, on ? -13049345 : -7358785);
      g.fill(x, y + 9, x + 22, y + 10, on ? -13049345 : -7358785);
      g.fill(on ? x + 13 : x + 1, y + 2, on ? x + 21 : x + 9, y + 8, on ? -13049345 : -7358785);
      g.drawString(mc.font, label, x + 28, y + 1, on ? -2689537 : -7358785);
   }

   public static void slider(GuiGraphics g, int x, int y, int w, float value01, String label, String valueText) {
      Minecraft mc = Minecraft.getInstance();
      g.drawString(mc.font, label, x, y - 10, -7358785);
      g.drawString(mc.font, valueText, x + w - mc.font.width(valueText), y - 10, -13049345);
      g.fill(x, y + 2, x + w, y + 4, 1610612736);
      g.fill(x, y + 2, x + w, y + 3, 859365887);
      int knob = x + (int)((float)w * Math.max(0.0F, Math.min(1.0F, value01)));
      g.fill(knob - 2, y - 2, knob + 2, y + 8, -13049345);
      g.fill(knob - 2, y - 2, knob + 2, y - 1, -1);
   }

   public static String title() {
      return "物品光影 · 控制台";
   }
}
