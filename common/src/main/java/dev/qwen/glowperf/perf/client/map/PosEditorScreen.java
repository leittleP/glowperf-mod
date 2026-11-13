package dev.qwen.glowperf.perf.client.map;

import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.core.PerfState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PosEditorScreen extends Screen {
   private static final int HANDLE = 10;
   private final PosEditorScreen.Panel[] panels;
   private int dragging = -1;
   private int dragOffX;
   private int dragOffY;
   private boolean changed;

   public PosEditorScreen() {
      super(Component.literal("HUD 位置调整"));
      this.panels = new PosEditorScreen.Panel[]{
         new PosEditorScreen.Panel(
            "性能面板", () -> PerfState.panelX, () -> PerfState.panelY, 140, 80, var0 -> PerfState.panelX = var0, var0 -> PerfState.panelY = var0
         ),
         new PosEditorScreen.Panel(
            "坐标面板", () -> PerfState.coordsX, () -> PerfState.coordsY, 180, 70, var0 -> PerfState.coordsX = var0, var0 -> PerfState.coordsY = var0
         ),
         new PosEditorScreen.Panel(
            "小地图",
            () -> PerfState.mapX,
            () -> PerfState.mapY,
            PerfState.mapSize,
            PerfState.mapSize + 24,
            var0 -> PerfState.mapX = var0,
            var0 -> PerfState.mapY = var0
         ),
         new PosEditorScreen.Panel(
            "常驻小地图",
            () -> PerfState.minimapX,
            () -> PerfState.minimapY,
            PerfState.minimapSize,
            PerfState.minimapSize + 16,
            var0 -> PerfState.minimapX = var0,
            var0 -> PerfState.minimapY = var0
         )
      };
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      Font var5 = Minecraft.getInstance().font;
      var1.fill(0, 0, this.width, this.height, Integer.MIN_VALUE);

      for (int var6 = 0; var6 < this.panels.length; var6++) {
         PosEditorScreen.Panel var7 = this.panels[var6];
         int var8 = var7.x.get();
         int var9 = var7.y.get();
         int var10 = var6 == this.dragging ? -13049345 : -7358785;
         var1.fill(var8, var9, var8 + var7.w, var9 + var7.h, 1140850688);
         var1.fill(var8, var9, var8 + var7.w, var9 + 1, var10);
         var1.fill(var8, var9 + var7.h - 1, var8 + var7.w, var9 + var7.h, var10);
         var1.fill(var8, var9, var8 + 1, var9 + var7.h, var10);
         var1.fill(var8 + var7.w - 1, var9, var8 + var7.w, var9 + var7.h, var10);
         var1.drawString(var5, var7.name, var8 + 4, var9 + 4, -2689537);
         var1.drawString(var5, var8 + "," + var9, var8 + 4, var9 + var7.h - 12, -7358785);
      }

      byte var11 = 6;
      int var12 = this.width - 120;
      this.drawBtn(var1, var5, var12, var11, 50, 18, "✕ 取消", -38022, var2, var3);
      this.drawBtn(var1, var5, var12 + 58, var11, 50, 18, "✓ 保存", -11010128, var2, var3);
      var1.drawString(var5, "拖动面板调整位置，保存后写入配置", 8, var11 + 5, -2689537);
   }

   private void drawBtn(GuiGraphics var1, Font var2, int var3, int var4, int var5, int var6, String var7, int var8, int var9, int var10) {
      boolean var11 = var9 >= var3 && var9 <= var3 + var5 && var10 >= var4 && var10 <= var4 + var6;
      var1.fill(var3, var4, var3 + var5, var4 + var6, var11 ? -14013910 : -15395563);
      var1.fill(var3, var4, var3 + var5, var4 + 1, var8);
      var1.drawString(var2, var7, var3 + (var5 - var2.width(var7)) / 2, var4 + 5, var11 ? -1 : var8);
   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (var5 != 0) {
         return super.mouseClicked(var1, var3, var5);
      } else {
         byte var6 = 6;
         int var7 = this.width - 120;
         if (inRect(var1, var3, var7, var6, 50, 18)) {
            this.onClose();
            return true;
         } else if (inRect(var1, var3, var7 + 58, var6, 50, 18)) {
            PerfClient.persist();
            this.changed = false;
            this.onClose();
            return true;
         } else {
            for (int var8 = 0; var8 < this.panels.length; var8++) {
               PosEditorScreen.Panel var9 = this.panels[var8];
               if (inRect(var1, var3, var9.x.get(), var9.y.get(), var9.w, var9.h)) {
                  this.dragging = var8;
                  this.dragOffX = (int)var1 - var9.x.get();
                  this.dragOffY = (int)var3 - var9.y.get();
                  return true;
               }
            }

            return super.mouseClicked(var1, var3, var5);
         }
      }
   }

   public boolean mouseReleased(double var1, double var3, int var5) {
      if (this.dragging >= 0) {
         this.dragging = -1;
         this.changed = true;
         return true;
      } else {
         return super.mouseReleased(var1, var3, var5);
      }
   }

   public boolean mouseDragged(double var1, double var3, int var5, double var6, double var8) {
      if (this.dragging >= 0) {
         PosEditorScreen.Panel var10 = this.panels[this.dragging];
         var10.setX.set((int)var1 - this.dragOffX);
         var10.setY.set((int)var3 - this.dragOffY);
         return true;
      } else {
         return super.mouseDragged(var1, var3, var5, var6, var8);
      }
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (var1 == 256) {
         if (this.changed) {
            PerfClient.persist();
         }

         this.onClose();
         return true;
      } else {
         return super.keyPressed(var1, var2, var3);
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private static boolean inRect(double var0, double var2, int var4, int var5, int var6, int var7) {
      return var0 >= (double)var4 && var0 <= (double)(var4 + var6) && var2 >= (double)var5 && var2 <= (double)(var5 + var7);
   }

   private interface IntGetter {
      int get();
   }

   private interface IntSetter {
      void set(int var1);
   }

   private static final class Panel {
      final String name;
      final PosEditorScreen.IntGetter x;
      final PosEditorScreen.IntGetter y;
      final int w;
      final int h;
      final PosEditorScreen.IntSetter setX;
      final PosEditorScreen.IntSetter setY;

      Panel(
         String var1,
         PosEditorScreen.IntGetter var2,
         PosEditorScreen.IntGetter var3,
         int var4,
         int var5,
         PosEditorScreen.IntSetter var6,
         PosEditorScreen.IntSetter var7
      ) {
         this.name = var1;
         this.x = var2;
         this.y = var3;
         this.w = var4;
         this.h = var5;
         this.setX = var6;
         this.setY = var7;
      }
   }
}
