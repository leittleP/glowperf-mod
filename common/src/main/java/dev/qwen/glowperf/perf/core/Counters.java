package dev.qwen.glowperf.perf.core;

public final class Counters {
   public static volatile int culledLiving;
   public static volatile int culledPlayers;
   public static volatile int cappedSpawns;
   public static volatile int frozenAi;
   public static volatile long longFramesTotal;
   public static volatile long trimmedChunksTotal;
   public static volatile long culledTotal;
   public static int shownCulled;
   public static int shownCapped;
   public static int shownFrozen;
   public static int shownPlayers;

   private Counters() {
   }

   public static void beginWindow() {
      culledLiving = 0;
      culledPlayers = 0;
      cappedSpawns = 0;
      frozenAi = 0;
   }

   public static void cullLivingOne() {
      culledLiving++;
      culledTotal++;
   }

   public static void cullPlayerOne() {
      culledPlayers++;
   }

   public static void capSpawnOne() {
      cappedSpawns++;
   }

   public static void freezeOne() {
      frozenAi++;
   }

   public static void longFrame() {
      longFramesTotal++;
   }

   public static void trimmed(int n) {
      trimmedChunksTotal += (long)n;
   }

   public static void commitSnapshot() {
      shownCulled = culledLiving;
      shownPlayers = culledPlayers;
      shownCapped = cappedSpawns;
      shownFrozen = frozenAi;
   }
}
