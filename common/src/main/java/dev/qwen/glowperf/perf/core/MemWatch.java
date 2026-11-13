package dev.qwen.glowperf.perf.core;

import dev.qwen.glowperf.GlowPerfMod;

public final class MemWatch {
   private static final Runtime RT = Runtime.getRuntime();
   private static float lastRatio;
   private static long lastUsedMb;
   private static long lastTrendMb;
   private static long lastTrendAt;
   private static boolean warned;
   private static String pendingWarn;
   private static int windowCounter;
   private static long gcCountHint;
   private static long lastGcAt;

   private MemWatch() {
   }

   public static long usedBytes() {
      return RT.totalMemory() - RT.freeMemory();
   }

   public static long maxBytes() {
      return RT.maxMemory();
   }

   public static long usedMb() {
      return usedBytes() >> 20;
   }

   public static long maxMb() {
      return RT.maxMemory() >> 20;
   }

   public static float usedRatio() {
      long max = RT.maxMemory();
      return max <= 0L ? 0.0F : (float)usedBytes() / (float)max;
   }

   public static void tickWindow() {
      if (++windowCounter >= 16) {
         windowCounter = 0;
         long used = usedMb();
         lastTrendMb = used - lastUsedMb;
         lastUsedMb = used;
         lastRatio = usedRatio();
         lastTrendAt = System.currentTimeMillis();
         boolean over = lastRatio > PerfState.memSoftLimit;
         if (over && !warned && PerfState.memWarn) {
            warned = true;
            pendingWarn = String.format("堆占用 %.0f%%（%d/%d MB），已自动收紧性能档位；建议加大 JVM 内存分配", lastRatio * 100.0F, used, maxMb());
         } else if (lastRatio < PerfState.memSoftLimit - 0.1F) {
            warned = false;
         }
      }
   }

   public static String pollPendingWarn() {
      String s = pendingWarn;
      pendingWarn = null;
      return s;
   }

   public static long hintGc() {
      long before = usedMb();
      lastGcAt = System.currentTimeMillis();
      gcCountHint++;

      try {
         System.gc();
      } catch (Throwable var3) {
         GlowPerfMod.LOG.debug("GC 提示异常: {}", var3.toString());
      }

      return before - usedMb();
   }

   public static float ratio() {
      return lastRatio;
   }

   public static long trendMb() {
      return lastTrendMb;
   }

   public static long trendAgeMs() {
      return System.currentTimeMillis() - lastTrendAt;
   }

   public static long gcHints() {
      return gcCountHint;
   }

   public static boolean isWarned() {
      return warned;
   }

   public static void clearWarn() {
      warned = false;
   }
}
