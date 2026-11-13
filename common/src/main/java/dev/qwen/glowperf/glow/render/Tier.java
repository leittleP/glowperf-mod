package dev.qwen.glowperf.glow.render;

import java.util.Locale;

public enum Tier {
   AUTO(true, 4, 2, 512, 1.0F, 1.0F),
   ULTRA(true, 2, 4, 512, 1.0F, 1.0F),
   HIGH(true, 2, 3, 384, 0.92F, 1.0F),
   BALANCED(true, 4, 2, 192, 0.8F, 1.0F),
   LITE(false, 0, 0, 96, 0.7F, 0.7F),
   OFF(false, 0, 0, 0, 0.0F, 0.0F);

   public final boolean pipelineEnabled;
   public final int downscale;
   public final int blurIterations;
   public final int emitterCap;
   public final float strengthScale;
   public final float sizeScale;

   private Tier(boolean pipelineEnabled, int downscale, int blurIterations, int emitterCap, float strengthScale, float sizeScale) {
      this.pipelineEnabled = pipelineEnabled;
      this.downscale = downscale;
      this.blurIterations = blurIterations;
      this.emitterCap = emitterCap;
      this.strengthScale = strengthScale;
      this.sizeScale = sizeScale;
   }

   public int effectiveDownscale(int userDownscale) {
      return !this.pipelineEnabled ? 0 : Math.max(this.downscale == 0 ? 4 : this.downscale, Math.max(1, userDownscale));
   }

   public int effectiveBlurIterations() {
      return this.pipelineEnabled ? this.blurIterations : 0;
   }

   public int effectiveCap(int userCap) {
      return Math.min(this.emitterCap, Math.max(0, userCap));
   }

   public static Tier parse(String s) {
      if (s == null) {
         return AUTO;
      } else {
         String v = s.trim().toUpperCase(Locale.ROOT);

         for (Tier t : values()) {
            if (t.name().equals(v)) {
               return t;
            }
         }

         return AUTO;
      }
   }

   public Tier weaker() {
      switch (this) {
         case ULTRA:
            return HIGH;
         case HIGH:
         case AUTO:
            return BALANCED;
         case BALANCED:
            return LITE;
         default:
            return OFF;
      }
   }

   public Tier stronger() {
      switch (this) {
         case HIGH:
            return ULTRA;
         case AUTO:
         default:
            return ULTRA;
         case BALANCED:
            return HIGH;
         case OFF:
            return LITE;
         case LITE:
            return BALANCED;
      }
   }
}
