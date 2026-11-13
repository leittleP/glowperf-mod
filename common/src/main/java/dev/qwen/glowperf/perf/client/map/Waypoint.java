package dev.qwen.glowperf.perf.client.map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/**
 * 路点：记录某个维度的坐标，可在地图上显示并一键传送。
 */
public final class Waypoint {
   private static final int[] PALETTE = new int[]{
      0xFFFFC04D, 0xFF4DD2FF, 0xFFFF6FD8, 0xFF7CFF6B, 0xFFFF8A5C, 0xFFB08CFF, 0xFFFFE066, 0xFF5CFFD6
   };
   public final String dim;
   public final String name;
   public final double x;
   public final double y;
   public final double z;
   public final int color;

   public Waypoint(String dim, double x, double y, double z, String name, int color) {
      this.dim = dim == null ? "overworld" : dim;
      this.x = x;
      this.y = y;
      this.z = z;
      this.name = name == null || name.isBlank() ? "路点" : name.trim();
      this.color = color == 0 ? palette(this.name) : color;
   }

   public static Waypoint here(LocalPlayer player, ClientLevel level, String name) {
      String dim = level == null ? "overworld" : MapDraw.dimName(level);
      double x = player == null ? 0.0 : player.getX();
      double y = player == null ? 64.0 : player.getY();
      double z = player == null ? 0.0 : player.getZ();
      return new Waypoint(dim, x, y, z, name, 0);
   }

   public static int palette(String name) {
      int h = name == null ? 0 : name.hashCode();
      return PALETTE[Math.abs(h) % PALETTE.length];
   }

   public String encode() {
      return this.dim + "|" + this.x + "|" + this.y + "|" + this.z + "|" + esc(this.name) + "|" + this.color;
   }

   public static Waypoint decode(String s) {
      if (s == null) {
         return null;
      }

      String[] p = s.split("\\|", -1);
      if (p.length < 6) {
         return null;
      }

      try {
         return new Waypoint(
            p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]), unesc(p[4]), Integer.parseInt(p[5])
         );
      } catch (Throwable t) {
         return null;
      }
   }

   private static String esc(String s) {
      return s.replace("%", "%25").replace("|", "%7C").replace(";", "%3B");
   }

   private static String unesc(String s) {
      return s.replace("%7C", "|").replace("%3B", ";").replace("%25", "%");
   }

   public String pretty() {
      return this.name + " (" + this.dim + " " + (int)this.x + ", " + (int)this.y + ", " + (int)this.z + ")";
   }
}
