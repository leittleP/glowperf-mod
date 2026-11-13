package dev.qwen.glowperf.perf.config;

import dev.qwen.glowperf.GlowPerfMod;
import dev.qwen.glowperf.perf.core.PerfState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import dev.qwen.glowperf.platform.ConfigSpec;
import dev.qwen.glowperf.platform.ConfigSpec.BooleanValue;
import dev.qwen.glowperf.platform.ConfigSpec.Builder;
import dev.qwen.glowperf.platform.ConfigSpec.DoubleValue;
import dev.qwen.glowperf.platform.ConfigSpec.IntValue;
import dev.qwen.glowperf.platform.ConfigValue;

public final class PerfConfig {
   private static final Builder B = new Builder();
   public static final BooleanValue ENABLED = B.comment("性能优化总开关").define("master.enabled", true);
   public static final ConfigValue<String> PRESET = B.comment("优化档位：OFF/MILD/MODERATE/SEVERE/NO_MATTER（AUTO 会按帧时间在 轻度~无所谓 之间自动收放）").define("tier", "AUTO");
   public static final DoubleValue BUDGET_MS = B.comment("帧时间预算（毫秒），P95 超过即收紧").defineInRange("frameBudgetMs", 25.0, 5.0, 200.0);
   public static final DoubleValue TAIL_RATIO = B.comment("尾部延迟容忍倍率（P95 超预算的该倍数才动，避免小幅抖动误伤画质/行为）").defineInRange("tailRatio", 1.6, 1.1, 6.0);
   public static final BooleanValue CULL_LIVING = B.comment("远处生物渲染剔除").define("modules.cullLiving", true);
   public static final IntValue CULL_DISTANCE = B.comment("生物渲染保留距离（格），超出则不再绘制模型").defineInRange("modules.cullDistance", 64, 16, 256);
   public static final IntValue CULL_DISTANCE_SMALL = B.comment("小体型生物的更短剔除距离（格）").defineInRange("modules.cullDistanceSmall", 28, 8, 256);
   public static final BooleanValue CULL_MOUNT_ALWAYS = B.comment("骑乘中的生物永不剔除（避免自己的马突然消失）").define("modules.keepMounted", true);
   public static final BooleanValue CULL_NAMED_ALWAYS = B.comment("有名字标签的生物永不剔除").define("modules.keepNamed", true);
   public static final BooleanValue MEM_WATCH = B.comment("堆内存看护与空闲裁剪").define("modules.memWatch", true);
   public static final DoubleValue MEM_SOFT_LIMIT = B.comment("堆占用比例超过该值开始收紧（0.5-0.95）").defineInRange("modules.memSoftLimit", 0.8, 0.4, 0.97);
   public static final BooleanValue MEM_WARN = B.comment("内存吃紧时在游戏内提示（只提示一次一轮）").define("modules.memWarn", true);
   public static final BooleanValue PROBE = B.comment("帧时间 / tick 时长 / 长帧诊断").define("modules.probe", true);
   public static final IntValue LONG_FRAME_MS = B.comment("超过该毫秒数计为一次长帧（卡顿尖峰）").defineInRange("longFrameMs", 80, 20, 2000);
   public static final BooleanValue PANEL = B.comment("显示性能面板").define("hud.enabled", true);
   public static final BooleanValue PANEL_COMPACT = B.comment("面板精简为一行").define("hud.compact", false);
   public static final IntValue PANEL_X = B.comment("面板 X").defineInRange("hud.x", 6, -4096, 4096);
   public static final IntValue PANEL_Y = B.comment("面板 Y").defineInRange("hud.y", 60, -4096, 4096);
   public static final BooleanValue COORDS = B.comment("详细坐标面板：X/Y/Z、方块与区块、朝向、维度、群系、主世界/下界换算与移动速度").define("hud.coords", true);
   public static final IntValue COORDS_X = B.comment("坐标面板 X").defineInRange("hud.coordsX", 6, -4096, 4096);
   public static final IntValue COORDS_Y = B.comment("坐标面板 Y").defineInRange("hud.coordsY", 142, -4096, 4096);
   public static final ConfigValue<String> GLOW_MODE = B.comment("光影：OPEN(始终开，默认) / AUTO(掉帧时先降光影) / LITE(柔光档) / OFF(关) / IGNORE(不干预)").define("glow.mode", "OPEN");
   public static final BooleanValue GLOW_RESTORE = B.comment("恢复后把档位交还给光影自己的自动挡").define("glow.restore", true);
   public static final BooleanValue CULL_PLAYER = B.comment("[激进·默认关] 远处其他玩家渲染剔除（联机中可能影响 PvP 观感）").define("aggressive.cullPlayer", false);
   public static final IntValue CULL_PLAYER_DISTANCE = B.comment("其他玩家渲染保留距离（格）").defineInRange("aggressive.cullPlayerDistance", 96, 32, 256);
   public static final BooleanValue SPAWN_CAP = B.comment("[激进·默认关] 新生物生成数量护栏。1.20.1 事件里拿不到生成原因，无法区分刷怪笼，因此开启后可能影响刷怪塔产量——已默认豁免存档加载的生物")
      .define("aggressive.spawnCap", false);
   public static final IntValue SPAWN_CAP_COUNT = B.comment("同一玩家周围活体生物上限").defineInRange("aggressive.spawnCapCount", 96, 16, 512);
   public static final BooleanValue IDLE_NO_AI = B.comment("[激进·默认关] 远处闲置生物 AI 冻结（真实降 tick 负载，但会停农场产出）").define("aggressive.idleNoAi", false);
   public static final IntValue IDLE_NO_AI_DISTANCE = B.comment("AI 冻结距离（格）").defineInRange("aggressive.idleNoAiDistance", 72, 32, 256);
   public static final ConfigValue<String> EXEMPT_TYPES = B.comment("永不剔除与永不冻结的实体类型 id，逗号分隔，例：minecraft:villager,minecraft:iron_golem")
      .define("aggressive.exemptTypes", "");
   public static final ConfigValue<String> FORCE_CULL = B.comment("总是允许剔除的实体类型 id，逗号分隔，优先级低于上面的豁免表").define("aggressive.forceCull", "");
   public static final BooleanValue MAP = B.comment("小地图：总开关（/open the map 打开）").define("map.enabled", true);
   public static final BooleanValue MAP_MARKERS = B.comment("小地图：显示实体标记（掉落物/刷怪笼/敌对/友好）").define("map.markers", true);
   public static final BooleanValue MAP_CAVE = B.comment("小地图：Y<0 自动切换洞穴模式").define("map.caveAuto", true);
   public static final BooleanValue MAP_3D = B.comment("小地图：立体模式默认").define("map.stereo", false);
   public static final IntValue MAP_X = B.comment("小地图屏幕 X").defineInRange("map.x", 8, -4096, 4096);
   public static final IntValue MAP_Y = B.comment("小地图屏幕 Y").defineInRange("map.y", 8, -4096, 4096);
   public static final IntValue MAP_SIZE = B.comment("小地图边长（像素）").defineInRange("map.size", 220, 80, 512);
   public static final BooleanValue MAP_HUD = B.comment("常驻角落小地图开关（/minimap on|off）").define("map.hud", true);
   public static final IntValue MAP_HUD_X = B.comment("常驻小地图屏幕 X").defineInRange("map.hudX", 8, -4096, 4096);
   public static final IntValue MAP_HUD_Y = B.comment("常驻小地图屏幕 Y").defineInRange("map.hudY", 18, -4096, 4096);
   public static final IntValue MAP_HUD_SIZE = B.comment("常驻小地图边长（像素）").defineInRange("map.hudSize", 144, 64, 512);
   public static final ConfigValue<String> WAYPOINTS = B.comment("小地图路点数据（由 /wp 自动维护，请勿手改）").define("map.waypoints", "");
   public static final ConfigSpec SPEC = B.build();

   private PerfConfig() {
   }

   public static boolean save() {
      pushToSpec();
      boolean saved = SPEC.save();
      if (!saved) {
         GlowPerfMod.LOG.warn("配置保存失败: glowperf-common");
      }
      return saved;
   }

   public static List<String> parseCsv(String var0) {
      ArrayList var1 = new ArrayList();
      if (var0 != null && !var0.isBlank()) {
         for (String var5 : var0.split(",")) {
            String var6 = var5.trim().toLowerCase(Locale.ROOT);
            if (!var6.isEmpty() && !var1.contains(var6)) {
               var1.add(var6);
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   public static void pushToSpec() {
      ENABLED.set(PerfState.enabled);
      PRESET.set(PerfState.preset.name());
      BUDGET_MS.set((double)PerfState.budgetMs);
      TAIL_RATIO.set((double)PerfState.tailRatio);
      CULL_LIVING.set(PerfState.cullLiving);
      CULL_DISTANCE.set(PerfState.cullDistance);
      CULL_DISTANCE_SMALL.set(PerfState.cullDistanceSmall);
      CULL_MOUNT_ALWAYS.set(PerfState.keepMounted);
      CULL_NAMED_ALWAYS.set(PerfState.keepNamed);
      MEM_WATCH.set(PerfState.memWatch);
      MEM_SOFT_LIMIT.set((double)PerfState.memSoftLimit);
      MEM_WARN.set(PerfState.memWarn);
      PROBE.set(PerfState.probe);
      LONG_FRAME_MS.set(PerfState.longFrameMs);
      PANEL.set(PerfState.panel);
      PANEL_COMPACT.set(PerfState.panelCompact);
      PANEL_X.set(PerfState.panelX);
      PANEL_Y.set(PerfState.panelY);
      COORDS.set(PerfState.coords);
      COORDS_X.set(PerfState.coordsX);
      COORDS_Y.set(PerfState.coordsY);
      GLOW_MODE.set(PerfState.glowMode.name());
      GLOW_RESTORE.set(PerfState.glowRestore);
      CULL_PLAYER.set(PerfState.cullPlayer);
      CULL_PLAYER_DISTANCE.set(PerfState.cullPlayerDistance);
      SPAWN_CAP.set(PerfState.spawnCap);
      SPAWN_CAP_COUNT.set(PerfState.spawnCapCount);
      IDLE_NO_AI.set(PerfState.idleNoAi);
      IDLE_NO_AI_DISTANCE.set(PerfState.idleNoAiDistance);
      EXEMPT_TYPES.set(PerfState.exemptCsv());
      FORCE_CULL.set(PerfState.forceCullCsv());
      MAP.set(PerfState.mapEnabled);
      MAP_MARKERS.set(PerfState.mapMarkers);
      MAP_CAVE.set(PerfState.mapCaveAuto);
      MAP_3D.set(PerfState.mapStereo);
      MAP_X.set(PerfState.mapX);
      MAP_Y.set(PerfState.mapY);
      MAP_SIZE.set(PerfState.mapSize);
      MAP_HUD.set(PerfState.minimap);
      MAP_HUD_X.set(PerfState.minimapX);
      MAP_HUD_Y.set(PerfState.minimapY);
      MAP_HUD_SIZE.set(PerfState.minimapSize);
      WAYPOINTS.set(PerfState.waypointsCsv);
   }
}
