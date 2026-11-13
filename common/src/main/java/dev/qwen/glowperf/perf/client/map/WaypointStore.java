package dev.qwen.glowperf.perf.client.map;

import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.config.PerfConfig;
import dev.qwen.glowperf.perf.core.PerfState;
import java.util.ArrayList;
import java.util.List;

/**
 * 路点集合：内存列表 + 配置持久化（存成一段 CSV）。
 */
public final class WaypointStore {
   public static final int MAX = 64;
   private static final List<Waypoint> LIST = new ArrayList<>();

   private WaypointStore() {
   }

   public static List<Waypoint> all() {
      return LIST;
   }

   public static Waypoint find(String name) {
      if (name == null) {
         return null;
      }

      for (Waypoint w : LIST) {
         if (w.name.equalsIgnoreCase(name.trim())) {
            return w;
         }
      }

      return null;
   }

   /** 同名路点会被覆盖。返回 false 表示已达上限。 */
   public static boolean add(Waypoint w) {
      if (w == null) {
         return false;
      }

      remove(w.name);
      if (LIST.size() >= MAX) {
         return false;
      }

      LIST.add(w);
      return true;
   }

   public static boolean remove(String name) {
      if (name == null) {
         return false;
      }

      return LIST.removeIf(w -> w.name.equalsIgnoreCase(name.trim()));
   }

   public static void clear() {
      LIST.clear();
   }

   public static String save() {
      StringBuilder sb = new StringBuilder();

      for (Waypoint w : LIST) {
         if (sb.length() > 0) {
            sb.append(';');
         }

         sb.append(w.encode());
      }

      return sb.toString();
   }

   public static void load(String csv) {
      LIST.clear();
      if (csv == null || csv.isBlank()) {
         return;
      }

      for (String part : csv.split(";")) {
         Waypoint w = Waypoint.decode(part.trim());
         if (w != null && LIST.size() < MAX) {
            LIST.add(w);
         }
      }
   }

   public static void persist() {
      PerfState.waypointsCsv = save();
      PerfConfig.WAYPOINTS.set(PerfState.waypointsCsv);
      PerfClient.persist();
   }
}
