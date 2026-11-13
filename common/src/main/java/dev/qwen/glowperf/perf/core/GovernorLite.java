package dev.qwen.glowperf.perf.core;

public final class GovernorLite {
   private static final int RELAX_CONFIRM = 240;
   private static final int COOLDOWN = 60;
   private static PerfState.Preset current = PerfState.Preset.MILD;
   private static int stable;
   private static int cooldown;

   private GovernorLite() {
   }

   public static PerfState.Preset current() {
      return current;
   }

   public static void reset() {
      current = PerfState.Preset.MILD;
      stable = 0;
      cooldown = 0;
   }

   public static void tick() {
      if (PerfState.preset != PerfState.Preset.AUTO) {
         current = PerfState.preset;
      } else {
         float p95 = FrameProbe.frameP95();
         float budget = PerfState.budgetMs;
         float tail = budget * Math.max(1.1F, PerfState.tailRatio);
         float heapRatio = MemWatch.usedRatio();
         if (cooldown > 0) {
            cooldown--;
         }

         boolean pressureFromMemory = PerfState.memWatch && heapRatio > PerfState.memSoftLimit;
         boolean pressureFromFrame = p95 > tail;
         if (!pressureFromFrame && !pressureFromMemory) {
            if (cooldown == 0 && !pressureFromMemory && p95 < budget * 0.62F) {
               if (++stable >= 240) {
                  PerfState.Preset next = current.looser();
                  if (next != current) {
                     current = next;
                     cooldown = 60;
                     PerfState.effective = current;
                  }

                  stable = 0;
               }
            } else if (p95 > budget) {
               stable = 0;
            }
         } else {
            PerfState.Preset next = current.tighter();
            if (next != current) {
               current = next;
               stable = 0;
               cooldown = 60;
               PerfState.effective = current;
            }
         }
      }
   }

   public static String describe() {
      return current.label;
   }
}
