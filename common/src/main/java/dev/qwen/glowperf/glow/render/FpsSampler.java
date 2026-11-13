package dev.qwen.glowperf.glow.render;

public final class FpsSampler {
   private static final int CAP = 128;
   private static final float[] RING = new float[128];
   private static final float[] SORT = new float[128];
   private static int index;
   private static int filled;
   private static float avgMs = 20.0F;
   private static float p95Ms = 25.0F;
   private static float minMs = 20.0F;
   private static float maxMs = 20.0F;
   private static int ticks;

   private FpsSampler() {
   }

   public static void sample(float ms) {
      if (!(ms <= 0.0F) && !(ms > 5000.0F)) {
         RING[index] = ms;
         index = (index + 1) % 128;
         if (filled < 128) {
            filled++;
         }

         if (++ticks >= 8) {
            ticks = 0;
            recompute();
         }
      }
   }

   private static void recompute() {
      int n = filled;
      if (n != 0) {
         float sum = 0.0F;
         float min = Float.MAX_VALUE;
         float max = -Float.MAX_VALUE;

         for (int i = 0; i < n; i++) {
            float v = RING[i];
            sum += v;
            if (v < min) {
               min = v;
            }

            if (v > max) {
               max = v;
            }

            SORT[i] = v;
         }

         for (int i = 1; i < n; i++) {
            float key = SORT[i];

            int j;
            for (j = i - 1; j >= 0 && SORT[j] > key; j--) {
               SORT[j + 1] = SORT[j];
            }

            SORT[j + 1] = key;
         }

         avgMs = sum / (float)n;
         minMs = min;
         maxMs = max;
         int p95i = Math.min(n - 1, (int)((float)n * 0.95F));
         p95Ms = SORT[p95i];
      }
   }

   public static float avgMs() {
      return avgMs;
   }

   public static float p95Ms() {
      return p95Ms;
   }

   public static float minMs() {
      return minMs;
   }

   public static float maxMs() {
      return maxMs;
   }

   public static float fps() {
      return avgMs <= 0.001F ? 0.0F : 1000.0F / avgMs;
   }

   public static int sampledFrames() {
      return filled;
   }

   public static void reset() {
      index = 0;
      filled = 0;
      ticks = 0;
      avgMs = 20.0F;
      p95Ms = 25.0F;
      minMs = 20.0F;
      maxMs = 20.0F;
   }
}
