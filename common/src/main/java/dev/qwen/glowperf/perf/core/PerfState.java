package dev.qwen.glowperf.perf.core;

import dev.qwen.glowperf.perf.client.map.WaypointStore;
import dev.qwen.glowperf.perf.config.PerfConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PerfState {
   public static volatile boolean enabled = true;
   public static volatile PerfState.Preset preset = PerfState.Preset.AUTO;
   public static volatile PerfState.Preset effective = PerfState.Preset.MILD;
   public static volatile float budgetMs = 25.0F;
   public static volatile float tailRatio = 1.6F;
   public static volatile boolean cullLiving = true;
   public static volatile int cullDistance = 64;
   public static volatile int cullDistanceSmall = 28;
   public static volatile boolean keepMounted = true;
   public static volatile boolean keepNamed = true;
   public static volatile boolean memWatch = true;
   public static volatile float memSoftLimit = 0.8F;
   public static volatile boolean memWarn = true;
   public static volatile boolean probe = true;
   public static volatile int longFrameMs = 80;
   public static volatile boolean panel = true;
   public static volatile boolean panelCompact = false;
   public static volatile int panelX = 6;
   public static volatile int panelY = 60;
   public static volatile boolean coords = true;
   public static volatile int coordsX = 6;
   public static volatile int coordsY = 142;
   public static volatile PerfState.GlowMode glowMode = PerfState.GlowMode.ON;
   public static volatile boolean glowRestore = true;
   public static volatile int glowApplied;
   public static volatile boolean cullPlayer = false;
   public static volatile int cullPlayerDistance = 96;
   public static volatile boolean spawnCap = false;
   public static volatile int spawnCapCount = 96;
   public static volatile boolean idleNoAi = false;
   public static volatile int idleNoAiDistance = 72;
   private static volatile List<String> exempt = new ArrayList<>();
   private static volatile List<String> forceCull = new ArrayList<>();
   private static volatile String exemptCsv = "";
   private static volatile String forceCullCsv = "";
   public static volatile boolean mapEnabled = true;
   public static volatile boolean mapMarkers = true;
   public static volatile boolean mapCaveAuto = true;
   public static volatile boolean mapStereo = false;
   public static volatile int mapX = 8;
   public static volatile int mapY = 8;
   public static volatile int mapSize = 220;
   public static volatile boolean minimap = true;
   public static volatile int minimapX = 8;
   public static volatile int minimapY = 18;
   public static volatile int minimapSize = 144;
   public static volatile String waypointsCsv = "";

   private PerfState() {
   }

   public static List<String> exemptTypes() {
      return exempt;
   }

   public static List<String> forceCull() {
      return forceCull;
   }

   public static String exemptCsv() {
      return exemptCsv;
   }

   public static String forceCullCsv() {
      return forceCullCsv;
   }

   public static void setExempt(List<String> var0) {
      exempt = new ArrayList<>(var0);
      exemptCsv = String.join(", ", exempt);
      EntityRules.invalidate();
   }

   public static void setForceCull(List<String> var0) {
      forceCull = new ArrayList<>(var0);
      forceCullCsv = String.join(", ", forceCull);
      EntityRules.invalidate();
   }

   public static int distance(int var0) {
      return Math.round((float)var0 * effective.keepScale);
   }

   public static void readFromConfig() {
      enabled = (Boolean)PerfConfig.ENABLED.get();
      preset = PerfState.Preset.parse((String)PerfConfig.PRESET.get());
      budgetMs = ((Double)PerfConfig.BUDGET_MS.get()).floatValue();
      tailRatio = ((Double)PerfConfig.TAIL_RATIO.get()).floatValue();
      cullLiving = (Boolean)PerfConfig.CULL_LIVING.get();
      cullDistance = (Integer)PerfConfig.CULL_DISTANCE.get();
      cullDistanceSmall = (Integer)PerfConfig.CULL_DISTANCE_SMALL.get();
      keepMounted = (Boolean)PerfConfig.CULL_MOUNT_ALWAYS.get();
      keepNamed = (Boolean)PerfConfig.CULL_NAMED_ALWAYS.get();
      memWatch = (Boolean)PerfConfig.MEM_WATCH.get();
      memSoftLimit = ((Double)PerfConfig.MEM_SOFT_LIMIT.get()).floatValue();
      memWarn = (Boolean)PerfConfig.MEM_WARN.get();
      probe = (Boolean)PerfConfig.PROBE.get();
      longFrameMs = (Integer)PerfConfig.LONG_FRAME_MS.get();
      panel = (Boolean)PerfConfig.PANEL.get();
      panelCompact = (Boolean)PerfConfig.PANEL_COMPACT.get();
      panelX = (Integer)PerfConfig.PANEL_X.get();
      panelY = (Integer)PerfConfig.PANEL_Y.get();
      coords = (Boolean)PerfConfig.COORDS.get();
      coordsX = (Integer)PerfConfig.COORDS_X.get();
      coordsY = (Integer)PerfConfig.COORDS_Y.get();
      glowMode = PerfState.GlowMode.parse((String)PerfConfig.GLOW_MODE.get());
      glowRestore = (Boolean)PerfConfig.GLOW_RESTORE.get();
      cullPlayer = (Boolean)PerfConfig.CULL_PLAYER.get();
      cullPlayerDistance = (Integer)PerfConfig.CULL_PLAYER_DISTANCE.get();
      spawnCap = (Boolean)PerfConfig.SPAWN_CAP.get();
      spawnCapCount = (Integer)PerfConfig.SPAWN_CAP_COUNT.get();
      idleNoAi = (Boolean)PerfConfig.IDLE_NO_AI.get();
      idleNoAiDistance = (Integer)PerfConfig.IDLE_NO_AI_DISTANCE.get();
      exemptCsv = (String)PerfConfig.EXEMPT_TYPES.get();
      forceCullCsv = (String)PerfConfig.FORCE_CULL.get();
      exempt = PerfConfig.parseCsv(exemptCsv);
      forceCull = PerfConfig.parseCsv(forceCullCsv);
      mapEnabled = (Boolean)PerfConfig.MAP.get();
      mapMarkers = (Boolean)PerfConfig.MAP_MARKERS.get();
      mapCaveAuto = (Boolean)PerfConfig.MAP_CAVE.get();
      mapStereo = (Boolean)PerfConfig.MAP_3D.get();
      mapX = (Integer)PerfConfig.MAP_X.get();
      mapY = (Integer)PerfConfig.MAP_Y.get();
      mapSize = (Integer)PerfConfig.MAP_SIZE.get();
      minimap = (Boolean)PerfConfig.MAP_HUD.get();
      minimapX = (Integer)PerfConfig.MAP_HUD_X.get();
      minimapY = (Integer)PerfConfig.MAP_HUD_Y.get();
      minimapSize = (Integer)PerfConfig.MAP_HUD_SIZE.get();
      waypointsCsv = (String)PerfConfig.WAYPOINTS.get();
      WaypointStore.load(waypointsCsv);
      EntityRules.invalidate();
      applyPreset();
   }

   public static void applyPreset() {
      effective = preset == PerfState.Preset.AUTO ? GovernorLite.current() : preset;
   }

   public static void resetDefaults() {
      enabled = true;
      preset = PerfState.Preset.AUTO;
      effective = PerfState.Preset.MILD;
      budgetMs = 25.0F;
      tailRatio = 1.6F;
      cullLiving = true;
      cullDistance = 64;
      cullDistanceSmall = 28;
      keepMounted = true;
      keepNamed = true;
      memWatch = true;
      memSoftLimit = 0.8F;
      memWarn = true;
      probe = true;
      longFrameMs = 80;
      panel = true;
      panelCompact = false;
      panelX = 6;
      panelY = 60;
      coords = true;
      coordsX = 6;
      coordsY = 142;
      glowMode = PerfState.GlowMode.ON;
      glowRestore = true;
      glowApplied = 0;
      cullPlayer = false;
      cullPlayerDistance = 96;
      spawnCap = false;
      spawnCapCount = 96;
      idleNoAi = false;
      idleNoAiDistance = 72;
      exempt = new ArrayList<>();
      forceCull = new ArrayList<>();
      exemptCsv = "";
      forceCullCsv = "";
      mapEnabled = true;
      mapMarkers = true;
      mapCaveAuto = true;
      mapStereo = false;
      mapX = 8;
      mapY = 8;
      mapSize = 220;
      minimap = true;
      minimapX = 8;
      minimapY = 18;
      minimapSize = 144;
      EntityRules.invalidate();
   }

   public static void toggleMaster() {
      enabled = !enabled;
      if (!enabled) {
         effective = PerfState.Preset.OFF;
         EntityRules.resetAllFrozen();
      } else {
         applyPreset();
      }
   }

   public static enum GlowMode {
      ON,
      AUTO,
      LITE,
      OFF,
      IGNORE;

      public static PerfState.GlowMode parse(String s) {
         if (s == null) {
            return ON;
         } else {
            String v = s.trim().toUpperCase(Locale.ROOT);
            if (v.equals("OPEN") || v.equals("ON")) {
               return ON;
            }
            if (v.equals("AUTO")) {
               return AUTO;
            }
            if (v.equals("LITE")) {
               return LITE;
            }
            if (v.equals("OFF") || v.equals("CLOSE")) {
               return OFF;
            }
            if (v.equals("IGNORE")) {
               return IGNORE;
            }

            return ON;
         }
      }

      public String label() {
         switch (this) {
            case ON:
               return "开启";
            case AUTO:
               return "自动";
            case LITE:
               return "柔光";
            case OFF:
               return "关闭";
            default:
               return "不干预";
         }
      }
   }

   public static enum Preset {
      OFF("关闭", 1.0F),
      MILD("轻度", 0.9F),
      MODERATE("中度", 0.75F),
      SEVERE("重度", 0.58F),
      NO_MATTER("无所谓", 0.42F),
      AUTO("自动", 1.0F);

      public final String label;
      public final float keepScale;
      public static final PerfState.Preset[] LADDER = new PerfState.Preset[]{
         PerfState.Preset.NO_MATTER, PerfState.Preset.SEVERE, PerfState.Preset.MODERATE, PerfState.Preset.MILD, PerfState.Preset.OFF
      };

      private Preset(String label, float keepScale) {
         this.label = label;
         this.keepScale = keepScale;
      }

      public static PerfState.Preset parse(String s) {
         if (s == null) {
            return AUTO;
         } else {
            String v = s.trim().toLowerCase(Locale.ROOT);
            if (v.equals("off") || v.equals("close") || v.equals("safe") || v.equals("关闭")) {
               return OFF;
            }

            if (v.equals("mild") || v.equals("light") || v.equals("轻度")) {
               return MILD;
            }

            if (v.equals("moderate") || v.equals("medium") || v.equals("中度")) {
               return MODERATE;
            }

            if (v.equals("severe") || v.equals("aggressive") || v.equals("heavy") || v.equals("重度")) {
               return SEVERE;
            }

            if (v.equals("tmatter") || v.equals("t matter") || v.equals("nomatter") || v.equals("no_matter") || v.equals("whatever") || v.equals("无所谓")) {
               return NO_MATTER;
            }

            return AUTO;
         }
      }

      public PerfState.Preset tighter() {
         switch (this) {
            case OFF:
               return MILD;
            case MILD:
               return MODERATE;
            case MODERATE:
               return SEVERE;
            default:
               return NO_MATTER;
         }
      }

      public PerfState.Preset looser() {
         switch (this) {
            case NO_MATTER:
               return SEVERE;
            case SEVERE:
               return MODERATE;
            case MODERATE:
               return MILD;
            default:
               return OFF;
         }
      }
   }
}
