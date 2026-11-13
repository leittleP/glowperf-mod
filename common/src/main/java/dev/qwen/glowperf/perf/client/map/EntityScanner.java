package dev.qwen.glowperf.perf.client.map;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class EntityScanner {
   public static final int COLOR_ITEM = -22016;
   public static final int COLOR_SPAWNER = -5635841;
   public static final int COLOR_HOSTILE = -53200;
   public static final int COLOR_FRIENDLY = -13566112;
   public static final int COLOR_OTHER = -5197648;

   private EntityScanner() {
   }

   public static List<EntityScanner.Marker> scan(ClientLevel var0, double var1, double var3, double var5) {
      ArrayList var7 = new ArrayList();
      if (var0 == null) {
         return var7;
      } else {
         double var8 = var5 * var5;

         for (Entity var11 : var0.entitiesForRendering()) {
            double var12 = var11.getX() - var1;
            double var14 = var11.getZ() - var3;
            if (!(var12 * var12 + var14 * var14 > var8)) {
               int var16;
               String var17;
               if (var11 instanceof ItemEntity) {
                  var16 = -22016;
                  var17 = "掉落物";
               } else if (var11 instanceof Enemy) {
                  var16 = -53200;
                  var17 = "敌对";
               } else if (!(var11 instanceof Animal) && !(var11 instanceof WaterAnimal) && !(var11 instanceof AmbientCreature)) {
                  var16 = -5197648;
                  var17 = "实体";
               } else {
                  var16 = -13566112;
                  var17 = "友好";
               }

               var7.add(new EntityScanner.Marker(var11.getX(), var11.getY(), var11.getZ(), var16, var17));
            }
         }

         int var24 = (int)Math.floor((var1 - var5) / 16.0);
         int var25 = (int)Math.floor((var1 + var5) / 16.0);
         int var26 = (int)Math.floor((var3 - var5) / 16.0);
         int var13 = (int)Math.floor((var3 + var5) / 16.0);

         for (int var27 = var24; var27 <= var25; var27++) {
            for (int var15 = var26; var15 <= var13; var15++) {
               LevelChunk var28 = var0.getChunk(var27, var15);
               if (var28 != null) {
                  for (BlockEntity var18 : var28.getBlockEntities().values()) {
                     if (var18 instanceof SpawnerBlockEntity) {
                        BlockPos var19 = var18.getBlockPos();
                        double var20 = (double)var19.getX() + 0.5 - var1;
                        double var22 = (double)var19.getZ() + 0.5 - var3;
                        if (var20 * var20 + var22 * var22 <= var8) {
                           var7.add(
                              new EntityScanner.Marker(
                                 (double)var19.getX() + 0.5, (double)var19.getY(), (double)var19.getZ() + 0.5, -5635841, "刷怪笼"
                              )
                           );
                        }
                     }
                  }
               }
            }
         }

         return var7;
      }
   }

   public static final class Marker {
      public final double x;
      public final double y;
      public final double z;
      public final int color;
      public final String label;

      public Marker(double var1, double var3, double var5, int var7, String var8) {
         this.x = var1;
         this.y = var3;
         this.z = var5;
         this.color = var7;
         this.label = var8;
      }
   }
}
