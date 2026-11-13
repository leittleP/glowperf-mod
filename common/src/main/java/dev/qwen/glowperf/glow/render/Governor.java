package dev.qwen.glowperf.glow.render;

import dev.qwen.glowperf.glow.core.GlowState;

public final class Governor {
   private static final int UP_CONFIRM_FRAMES = 180;
   private static final int COOLDOWN_FRAMES = 90;
   private static Tier current = Tier.HIGH;
   private static int stableFrames;
   private static int cooldown;

   private Governor() {
   }

   public static Tier current() {
      return current;
   }

   public static void reset() {
      current = Tier.HIGH;
      stableFrames = 0;
      cooldown = 0;
   }

   public static void tick() {
      float p95 = FpsSampler.p95Ms();
      float budget = GlowState.budgetMs;
      if (budget < 1.0F) {
         budget = 20.0F;
      }

      if (cooldown > 0) {
         cooldown--;
      }

      if (p95 > budget * 1.6F) {
         if (current != Tier.OFF && current != Tier.LITE || p95 > budget * 3.0F) {
            Tier next = current.weaker();
            if (next != current) {
               current = next;
               stableFrames = 0;
               cooldown = 90;
            }
         }
      } else {
         if (cooldown == 0 && p95 < budget * 0.65F) {
            stableFrames++;
            if (stableFrames >= 180) {
               Tier next = current.stronger();
               if (next != current) {
                  current = next;
                  cooldown = 90;
               }

               stableFrames = 0;
            }
         } else if (p95 > budget) {
            stableFrames = 0;
         }
      }
   }

   public static String describe() {
      return current.name();
   }
}
