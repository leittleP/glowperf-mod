package dev.qwen.glowperf.perf.core;

public final class FrameProbe {
   private static final int CAP = 128;
   private static final float[] FRAME = new float[128];
   private static final float[] TICK = new float[128];
   private static final float[] SORT = new float[128];
   private static int fIndex;
   private static int tIndex;
   private static int fFilled;
   private static int tFilled;
   private static int ticks;
   private static float frameAvg = 20.0F;
   private static float frameP95 = 25.0F;
   private static float frameMax = 20.0F;
   private static float tickAvg = 20.0F;
   private static float tickP95 = 22.0F;
   private static float tickMax = 20.0F;
   private static float tps = 20.0F;
   private static long lastNs;

   private FrameProbe() {
   }

   public static void frame(float ms) {
      if (!(ms <= 0.0F) && !(ms > 5000.0F)) {
         FRAME[fIndex] = ms;
         fIndex = (fIndex + 1) % 128;
         if (fFilled < 128) {
            fFilled++;
         }

         if (ms > (float)PerfState.longFrameMs) {
            Counters.longFrame();
         }
      }
   }

   public static void tickSample(double ms) {
      if (!(ms <= 0.0) && !(ms > 5000.0)) {
         float v = (float)ms;
         TICK[tIndex] = v;
         tIndex = (tIndex + 1) % 128;
         if (tFilled < 128) {
            tFilled++;
         }
      }
   }

   public static void step() {
      if (++ticks >= 8) {
         ticks = 0;
         frameAvg = stats(FRAME, fFilled, true);
         tickAvg = stats(TICK, tFilled, false);
         tps = tickAvg <= 0.001F ? 20.0F : Math.min(20.0F, 1000.0F / tickAvg);
         Counters.commitSnapshot();
      }
   }

   private static float stats(float[] src, int n, boolean isFrame) {
      if (n == 0) {
         return 20.0F;
      } else {
         float sum = 0.0F;
         float max = -Float.MAX_VALUE;

         for (int i = 0; i < n; i++) {
            float v = src[i];
            sum += v;
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

         float avg = sum / (float)n;
         int p95i = Math.min(n - 1, (int)((float)n * 0.95F));
         float p95 = SORT[p95i];
         if (isFrame) {
            frameP95 = p95;
            frameMax = max;
         } else {
            tickP95 = p95;
            tickMax = max;
         }

         return avg;
      }
   }

   public static long tickNano() {
      long now = System.nanoTime();
      long d = lastNs == 0L ? 0L : now - lastNs;
      lastNs = now;
      return d;
   }

   public static float frameAvg() {
      return frameAvg;
   }

   public static float frameP95() {
      return frameP95;
   }

   public static float frameMax() {
      return frameMax;
   }

   public static float tickAvg() {
      return tickAvg;
   }

   public static float tickP95() {
      return tickP95;
   }

   public static float tickMax() {
      return tickMax;
   }

   public static float tps() {
      return tps;
   }

   public static float fps() {
      return frameAvg <= 0.001F ? 0.0F : 1000.0F / frameAvg;
   }

   public static int frameSamples() {
      return fFilled;
   }

   public static int tickSamples() {
      return tFilled;
   }

   public static void reset() {
      ticks = 0;
      tFilled = 0;
      fFilled = 0;
      tIndex = 0;
      fIndex = 0;
      lastNs = 0L;
      tickAvg = 20.0F;
      frameAvg = 20.0F;
      tickP95 = 25.0F;
      frameP95 = 25.0F;
      tickMax = 20.0F;
      frameMax = 20.0F;
      tps = 20.0F;
      Counters.longFramesTotal = 0L;
   }
}
