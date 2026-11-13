package dev.qwen.glowperf.perf.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;

/**
 * 俯视区块采样缓存：全屏地图与常驻小地图共用。
 * 按「每格像素 -> 采样步长」映射，缩得越小采样越稀，控制绘制量。
 */
public final class MapRaster {
   private int[] pixels;
   private int n;
   private int step = 1;
   private int startX;
   private int startZ;
   private double cx;
   private double cz;
   private double scale;
   private boolean cave;
   private boolean stereo;
   private long stamp = Long.MIN_VALUE;

   public boolean needs(ClientLevel level, double cx, double cz, double scale, boolean cave, boolean stereo, long now) {
      return this.pixels == null
         || now - this.stamp > 250L
         || cave != this.cave
         || stereo != this.stereo
         || Math.abs(cx - this.cx) > 4.0
         || Math.abs(cz - this.cz) > 4.0
         || Math.abs(scale - this.scale) > 0.01;
   }

   public void build(ClientLevel level, double cx, double cz, int sizePx, double scale, boolean cave, boolean stereo) {
      int st = scale < 1.5 ? 1 : (scale < 3.0 ? 2 : 4);
      int half = sizePx / 2 + st * 2;
      int count = half * 2 / st + 1;
      this.n = count;
      this.step = st;
      this.cx = cx;
      this.cz = cz;
      this.scale = scale;
      this.cave = cave;
      this.stereo = stereo;
      this.stamp = System.currentTimeMillis();
      if (this.pixels == null || this.pixels.length < count * count) {
         this.pixels = new int[count * count];
      }

      int sx = (int)Math.floor(cx) - half;
      int sz = (int)Math.floor(cz) - half;
      this.startX = sx;
      this.startZ = sz;
      Brightness bright = stereo ? Brightness.HIGH : Brightness.NORMAL;

      for (int row = 0; row < count; row++) {
         for (int col = 0; col < count; col++) {
            int wx = sx + col * st;
            int wz = sz + row * st;
            this.pixels[row * count + col] = sample(level, wx, wz, bright);
         }
      }
   }

   private int sample(ClientLevel level, int wx, int wz, Brightness bright) {
      try {
         MutableBlockPos pos = new MutableBlockPos();
         int y;
         if (this.cave) {
            LocalPlayer p = Minecraft.getInstance().player;
            int base = p == null ? -32 : (int)Math.floor(p.getY());
            int hit = -1;

            for (int dy = -3; dy <= 3; dy++) {
               pos.move(wx, base + dy, wz);
               BlockState s = level.getBlockState(pos);
               if (s != null && !s.hasLargeCollisionShape()) {
                  hit = base + dy;
                  break;
               }
            }

            if (hit < 0) {
               return -15724520;
            }

            y = hit;
         } else {
            y = level.getHeight(Types.WORLD_SURFACE, wx, wz) - 1;
            if (y < level.getMinBuildHeight()) {
               return -16777216;
            }
         }

         pos.move(wx, y, wz);
         BlockState state = level.getBlockState(pos);
         if (state == null || state.hasLargeCollisionShape()) {
            return this.cave ? -15198176 : -8343297;
         }

         MapColor mc = state.getMapColor(level, pos);
         if (mc == null) {
            return -8355712;
         }

         int rgb = mc.calculateRGBColor(bright);
         if (this.stereo && !this.cave) {
            int v = Math.max(0, Math.min(31, y / 8 + 16));
            int r = clamp((rgb >> 16 & 0xFF) + v - 16);
            int g = clamp((rgb >> 8 & 0xFF) + v - 16);
            int b = clamp((rgb & 0xFF) + v - 16);
            rgb = r << 16 | g << 8 | b;
         }

         return 0xFF000000 | rgb;
      } catch (Throwable t) {
         return -12566464;
      }
   }

   private static int clamp(int v) {
      return v < 0 ? 0 : (v > 255 ? 255 : v);
   }

   public void draw(GuiGraphics g, int centerX, int centerY, int clipX, int clipY, int clipW, int clipH) {
      if (this.pixels == null) {
         return;
      }

      for (int row = 0; row < this.n; row++) {
         for (int col = 0; col < this.n; col++) {
            int wx = this.startX + col * this.step;
            int wz = this.startZ + row * this.step;
            double px = (double)centerX + ((double)wx - this.cx) * this.scale;
            double py = (double)centerY + ((double)wz - this.cz) * this.scale;
            int cell = (int)Math.max(1.0, (double)this.step * this.scale);
            if (px + (double)cell < (double)clipX
               || px > (double)(clipX + clipW)
               || py + (double)cell < (double)clipY
               || py > (double)(clipY + clipH)) {
               continue;
            }

            g.fill((int)px, (int)py, (int)px + cell, (int)py + cell, this.pixels[row * this.n + col]);
         }
      }
   }
}
