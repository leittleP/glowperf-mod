package dev.qwen.glowperf.glow.render.gl;

import com.mojang.blaze3d.pipeline.TextureTarget;
import dev.qwen.glowperf.GlowPerfMod;

public final class GlTargets implements AutoCloseable {
   public TextureTarget mask;
   public TextureTarget swap;
   private int width = -1;
   private int height = -1;
   private int downscale = -1;
   private long lastRebuildMs;

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public boolean ensure(int screenW, int screenH, int downscale) {
      if (downscale < 1) {
         downscale = 1;
      }

      int w = Math.max(16, screenW / downscale);
      int h = Math.max(16, screenH / downscale);
      if (w == this.width && h == this.height && downscale == this.downscale && this.mask != null && this.swap != null) {
         return true;
      } else {
         long now = System.currentTimeMillis();
         if (now - this.lastRebuildMs >= 500L) {
            this.lastRebuildMs = now;
            this.dispose();

            try {
               this.mask = new TextureTarget(w, h, true, false);
               this.swap = new TextureTarget(w, h, true, false);
               this.mask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
               this.swap.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
               this.mask.setFilterMode(9729);
               this.swap.setFilterMode(9729);
               this.width = w;
               this.height = h;
               this.downscale = downscale;
               return true;
            } catch (Throwable var9) {
               GlowPerfMod.LOG.warn("离屏目标创建失败，光效将自动关闭: {}", var9.toString());
               this.dispose();
               return false;
            }
         } else {
            return this.mask != null && this.swap != null;
         }
      }
   }

   public void dispose() {
      if (this.mask != null) {
         try {
            this.mask.destroyBuffers();
         } catch (Throwable var3) {
         }

         this.mask = null;
      }

      if (this.swap != null) {
         try {
            this.swap.destroyBuffers();
         } catch (Throwable var2) {
         }

         this.swap = null;
      }

      this.width = -1;
      this.height = -1;
      this.downscale = -1;
   }

   @Override
   public void close() {
      this.dispose();
   }
}
