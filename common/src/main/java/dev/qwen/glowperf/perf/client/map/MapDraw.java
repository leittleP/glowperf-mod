package dev.qwen.glowperf.perf.client.map;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * 地图绘制的小工具：维度/群系名称、标记点、玩家箭头。
 */
public final class MapDraw {
   private MapDraw() {
   }

   public static String dimName(ClientLevel level) {
      try {
         String s = level.dimension().location().toString();
         return s.startsWith("minecraft:") ? s.substring("minecraft:".length()) : s;
      } catch (Throwable t) {
         return "?";
      }
   }

   public static String biomeName(ClientLevel level, BlockPos pos) {
      try {
         Holder<Biome> holder = level.getBiome(pos);
         if (holder == null) {
            return "?";
         }

         Registry<Biome> reg = level.registryAccess().registryOrThrow(Registries.BIOME);
         ResourceLocation id = reg.getKey(holder.value());
         if (id == null) {
            return "?";
         }

         String s = id.toString();
         return s.startsWith("minecraft:") ? s.substring("minecraft:".length()) : s;
      } catch (Throwable t) {
         return "?";
      }
   }

   public static void markers(
      GuiGraphics g, List<EntityScanner.Marker> markers, double cx, double cz, int centerX, int centerY, double scale, int clipX, int clipY, int clipW, int clipH
   ) {
      if (markers == null) {
         return;
      }

      for (EntityScanner.Marker m : markers) {
         double px = (double)centerX + (m.x - cx) * scale;
         double py = (double)centerY + (m.z - cz) * scale;
         if (px < (double)(clipX - 4) || px > (double)(clipX + clipW + 4) || py < (double)(clipY - 4) || py > (double)(clipY + clipH + 4)) {
            continue;
         }

         int r = m.color == EntityScanner.COLOR_SPAWNER ? 5 : 3;
         g.fill((int)px - r, (int)py - r, (int)px + r + 1, (int)py + r + 1, m.color);
         g.fill((int)px - r, (int)py - r, (int)px + r + 1, (int)py - r + 1, -1);
      }
   }

   public static void waypoints(
      GuiGraphics g,
      net.minecraft.client.gui.Font font,
      List<Waypoint> list,
      String dim,
      double cx,
      double cz,
      int centerX,
      int centerY,
      double scale,
      int clipX,
      int clipY,
      int clipW,
      int clipH,
      boolean names
   ) {
      if (list == null || dim == null) {
         return;
      }

      for (Waypoint w : list) {
         if (!dim.equals(w.dim)) {
            continue;
         }

         double px = (double)centerX + (w.x - cx) * scale;
         double py = (double)centerY + (w.z - cz) * scale;
         if (px < (double)(clipX - 6) || px > (double)(clipX + clipW + 40) || py < (double)(clipY - 6) || py > (double)(clipY + clipH + 6)) {
            continue;
         }

         g.fill((int)px - 4, (int)py - 4, (int)px + 5, (int)py + 5, -16777216);
         g.fill((int)px - 3, (int)py - 3, (int)px + 4, (int)py + 4, w.color);
         if (names && font != null) {
            g.drawString(font, w.name, (int)px + 6, (int)py - 4, w.color);
         }
      }
   }

   public static void arrow(GuiGraphics g, int centerX, int centerY, float yawDeg) {
      g.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, -1);
      g.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, -16777216);
      double rad = Math.toRadians((double)yawDeg);
      int tipX = centerX + (int)(Math.sin(rad) * 8.0);
      int tipY = centerY - (int)(Math.cos(rad) * 8.0);
      g.fill(tipX - 1, tipY - 1, tipX + 2, tipY + 2, -256);
   }

   public static void caveTint(GuiGraphics g, int x, int y, int w, int h) {
      g.fill(x, y, x + w, y + h, 1426063360);
   }

   public static int safeY(ClientLevel level, double wx, double wz, LocalPlayer p, boolean cave) {
      int playerY = p == null ? 64 : (int)Math.floor(p.getY());
      if (cave || level == null || !"overworld".equals(dimName(level))) {
         return playerY;
      }

      try {
         return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, (int)Math.floor(wx), (int)Math.floor(wz));
      } catch (Throwable t) {
         return playerY;
      }
   }
}
