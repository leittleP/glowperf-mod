package dev.qwen.glowperf.perf;

import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.perf.core.Counters;
import dev.qwen.glowperf.perf.core.MemWatch;
import java.util.Arrays;

public final class SlowLog {
   private static final int CAP = 24;
   private static final String[] LOG = new String[24];
   private static final long[] STAMP = new long[24];
   private static int index;
   private static int filled;
   private static long lastWriteMs;

   private SlowLog() {
   }

   public static void capture(float frameMs, float tps) {
      long now = System.currentTimeMillis();
      if (now - lastWriteMs >= 250L) {
         lastWriteMs = now;
         LOG[index] = String.format(
            "帧 %.0f ms ｜ TPS %.1f ｜ 堆 %d MB ｜ 剔除 %d ｜ 发光 %d ｜ 冻AI %d",
            frameMs,
            tps,
            MemWatch.usedMb(),
            Counters.shownCulled,
            GlowState.lastEmitterCount,
            Counters.shownFrozen
         );
         STAMP[index] = now;
         index = (index + 1) % 24;
         if (filled < 24) {
            filled++;
         }
      }
   }

   public static String[] recent(int limit) {
      int n = Math.min(limit, filled);
      String[] out = new String[n];

      for (int i = 0; i < n; i++) {
         int idx = (index - 1 - i + 48) % 24;
         long age = (System.currentTimeMillis() - STAMP[idx]) / 1000L;
         out[i] = String.format("%d 秒前 · %s", age, LOG[idx]);
      }

      return out;
   }

   public static int size() {
      return filled;
   }

   public static void reset() {
      index = 0;
      filled = 0;
      lastWriteMs = 0L;
      Arrays.fill(LOG, null);
   }
}
