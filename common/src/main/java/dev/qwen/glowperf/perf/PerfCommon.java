package dev.qwen.glowperf.perf;

import dev.qwen.glowperf.perf.core.Counters;
import dev.qwen.glowperf.perf.core.EntityRules;
import dev.qwen.glowperf.perf.core.FrameProbe;
import dev.qwen.glowperf.perf.core.MemWatch;
import dev.qwen.glowperf.perf.core.PerfState;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public final class PerfCommon {
   private static int mobCount;
   private static int sweepTick;
   private static long tickStartNs;

   private PerfCommon() {
   }

   public static void onServerTickStart() {
      tickStartNs = System.nanoTime();
   }

   public static void onServerTickEnd(MinecraftServer server) {
      if (tickStartNs != 0L) {
         FrameProbe.tickSample((double)(System.nanoTime() - tickStartNs) / 1000000.0);
         tickStartNs = 0L;
      }

      if (PerfState.memWatch) {
         MemWatch.tickWindow();
      }

      if (PerfState.idleNoAi && PerfState.enabled && ++sweepTick >= 40) {
         sweepTick = 0;
         sweepIdleAi(server);
      }
   }

   public static boolean onEntityJoin(Entity entity, boolean loadedFromDisk) {
      if (!(entity instanceof Mob)) {
         return false;
      } else {
         if (!loadedFromDisk && onFreshEntitySpawn(entity)) {
            return true;
         }

         onEntityLoad(entity);
         return false;
      }
   }

   public static boolean onFreshEntitySpawn(Entity entity) {
      if (entity instanceof Mob && PerfState.enabled && PerfState.spawnCap && !(entity instanceof Player) && mobCount >= PerfState.spawnCapCount) {
         Counters.capSpawnOne();
         return true;
      } else {
         return false;
      }
   }

   public static void onEntityLoad(Entity entity) {
      if (entity instanceof Mob) {
         mobCount++;
      }
   }

   public static void onEntityLeave(Entity entity) {
      if (entity instanceof Mob && mobCount > 0) {
         mobCount--;
      }
   }

   public static void onLevelUnload() {
      mobCount = 0;
      EntityRules.resetAllFrozen();
   }

   public static int trackedMobs() {
      return mobCount;
   }

   private static void sweepIdleAi(MinecraftServer server) {
      if (server != null) {
         int dist = PerfState.distance(PerfState.idleNoAiDistance);
         double dist2 = (double)dist * (double)dist;

         for (ServerLevel level : server.getAllLevels()) {
            List<? extends Player> players = level.players();
            int np = players.size();
            if (np != 0) {
               AABB box = new AABB(
                  players.get(0).getX() - (double)dist * 2.5,
                  players.get(0).getY() - 32.0,
                  players.get(0).getZ() - (double)dist * 2.5,
                  players.get(0).getX() + (double)dist * 2.5,
                  players.get(0).getY() + 32.0,
                  players.get(0).getZ() + (double)dist * 2.5
               );
               List<Mob> mobs = level.getEntities(EntityTypeTest.forClass(Mob.class), box, mob -> !mob.isRemoved());

               for (int i = 0; i < mobs.size(); i++) {
                  Mob mob = mobs.get(i);
                  if ((!mob.isPassenger() || !PerfState.keepMounted) && (!mob.hasCustomName() || !PerfState.keepNamed)) {
                     double nearest = Double.MAX_VALUE;

                     for (int p = 0; p < np; p++) {
                        double distance = mob.distanceToSqr((Entity)players.get(p));
                        if (distance < nearest) {
                           nearest = distance;
                        }
                     }

                     if (nearest > dist2) {
                        if (!mob.isNoAi() && EntityRules.registerFreeze(mob.getId())) {
                           mob.setNoAi(true);
                           Counters.freezeOne();
                        }
                     } else if (EntityRules.unregisterFreeze(mob.getId())) {
                        mob.setNoAi(false);
                     }
                  }
               }
            }
         }
      }
   }
}
