package dev.qwen.glowperf.perf.client.map;

public final class MapState {
   public static volatile boolean open = false;
   public static volatile double panX = 0.0;
   public static volatile double panZ = 0.0;
   public static volatile double scale = 3.0;
   public static volatile boolean stereo = false;
   public static volatile boolean cave = false;
   /** 手动切换的洞穴模式（自动检测关闭时生效）。 */
   public static volatile boolean caveManual = false;
   public static volatile String dim = "";
   public static volatile int markerTick = 0;

   private MapState() {
   }

   public static void resetPan() {
      panX = 0.0;
      panZ = 0.0;
   }

   public static void clampScale() {
      if (scale < 0.5) {
         scale = 0.5;
      } else if (scale > 16.0) {
         scale = 16.0;
      }
   }
}
