package dev.qwen.glowperf.perf.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class EntityRules {
   private static final String[] DEFAULT_EXEMPT = new String[]{
      "minecraft:armor_stand",
      "minecraft:item",
      "minecraft:experience_orb",
      "minecraft:ender_dragon",
      "minecraft:wither",
      "minecraft:villager",
      "minecraft:iron_golem",
      "minecraft:allay",
      "minecraft:chest_boat",
      "minecraft:chest_minecart",
      "minecraft:furnace_minecart",
      "minecraft:tnt",
      "minecraft:falling_block"
   };
   private static Set<String> exemptSet;
   private static Set<String> cullSet;
   private static List<String> exemptSrc;
   private static List<String> cullSrc;
   private static final int TABLE = 2048;
   private static final int[] FROZEN_ID = new int[2048];

   private EntityRules() {
   }

   public static void invalidate() {
      exemptSrc = null;
   }

   private static void sync() {
      List<String> ex = PerfState.exemptTypes();
      List<String> fc = PerfState.forceCull();
      if (ex != exemptSrc || exemptSet == null) {
         exemptSet = new HashSet<>();

         for (String s : DEFAULT_EXEMPT) {
            exemptSet.add(s);
         }

         for (String s : ex) {
            if (s != null) {
               exemptSet.add(s.toLowerCase(Locale.ROOT));
            }
         }

         exemptSrc = ex;
      }

      if (fc != cullSrc || cullSet == null) {
         cullSet = new HashSet<>();

         for (String sx : fc) {
            if (sx != null) {
               cullSet.add(sx.toLowerCase(Locale.ROOT));
            }
         }

         cullSrc = fc;
      }
   }

   private static String typeId(Entity e) {
      ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
      return rl == null ? "" : rl.toString().toLowerCase(Locale.ROOT);
   }

   public static boolean shouldCullLiving(Entity entity, double distSq, double boxSize) {
      if (!PerfState.enabled || !PerfState.cullLiving) {
         return false;
      } else if (entity instanceof Player) {
         return false;
      } else if (entity.isCurrentlyGlowing()) {
         return false;
      } else {
         sync();
         String id = typeId(entity);
         if (entity.hasCustomName() && PerfState.keepNamed) {
            return false;
         } else if (entity.isPassenger() && PerfState.keepMounted) {
            return false;
         } else if (cullSet.contains(id)) {
            return true;
         } else if (exemptSet.contains(id)) {
            return false;
         } else {
            int base = boxSize <= 0.7 ? PerfState.cullDistanceSmall : PerfState.cullDistance;
            double keep = (double)PerfState.distance(base);
            return distSq > keep * keep;
         }
      }
   }

   public static boolean shouldCullPlayer(Player player, Entity observer, double distSq) {
      if (!PerfState.enabled || !PerfState.cullPlayer) {
         return false;
      } else if (player == observer) {
         return false;
      } else if (player.isSpectator()) {
         return false;
      } else if (player.isCurrentlyGlowing()) {
         return false;
      } else {
         sync();
         if (exemptSet.contains("minecraft:player")) {
            return false;
         } else {
            double keep = (double)PerfState.distance(PerfState.cullPlayerDistance);
            return distSq > keep * keep;
         }
      }
   }

   public static boolean registerFreeze(int id) {
      int slot = id & 2047;

      for (int probe = 0; probe < 8; probe++) {
         int idx = slot + probe & 2047;
         if (FROZEN_ID[idx] == id) {
            return false;
         }

         if (FROZEN_ID[idx] == 0) {
            FROZEN_ID[idx] = id;
            return true;
         }
      }

      return false;
   }

   public static boolean unregisterFreeze(int id) {
      int slot = id & 2047;

      for (int probe = 0; probe < 8; probe++) {
         int idx = slot + probe & 2047;
         if (FROZEN_ID[idx] == id) {
            FROZEN_ID[idx] = 0;
            return true;
         }

         if (FROZEN_ID[idx] == 0) {
            return false;
         }
      }

      return false;
   }

   public static boolean isFrozen(int id) {
      int slot = id & 2047;

      for (int probe = 0; probe < 8; probe++) {
         int idx = slot + probe & 2047;
         if (FROZEN_ID[idx] == id) {
            return true;
         }

         if (FROZEN_ID[idx] == 0) {
            return false;
         }
      }

      return false;
   }

   public static void resetAllFrozen() {
      Arrays.fill(FROZEN_ID, 0);
   }

   public static int frozenCount() {
      int n = 0;

      for (int i = 0; i < 2048; i++) {
         if (FROZEN_ID[i] != 0) {
            n++;
         }
      }

      return n;
   }

   public static LivingEntity castLiving(Entity e) {
      return e instanceof LivingEntity ? (LivingEntity)e : null;
   }
}
