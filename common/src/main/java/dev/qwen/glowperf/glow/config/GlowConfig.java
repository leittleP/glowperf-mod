package dev.qwen.glowperf.glow.config;

import dev.qwen.glowperf.GlowPerfMod;
import dev.qwen.glowperf.glow.core.GlowState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dev.qwen.glowperf.platform.ConfigSpec;
import dev.qwen.glowperf.platform.ConfigSpec.BooleanValue;
import dev.qwen.glowperf.platform.ConfigSpec.Builder;
import dev.qwen.glowperf.platform.ConfigSpec.DoubleValue;
import dev.qwen.glowperf.platform.ConfigSpec.IntValue;
import dev.qwen.glowperf.platform.ConfigValue;

public final class GlowConfig {
   private static final Builder B = new Builder();
   public static final BooleanValue ENABLED = B.comment("光影总开关").define("master.enabled", true);
   public static final BooleanValue HAND = B.comment("手持/装备物品光晕").define("effects.hand", true);
   public static final BooleanValue DROP = B.comment("地面掉落物高亮描边").define("effects.drop", true);
   public static final BooleanValue LIGHTING = B.comment("物品动态受光高光").define("effects.lighting", true);
   public static final BooleanValue GUI = B.comment("背包/物品栏图标发光").define("effects.gui", true);
   public static final BooleanValue XRAY = B.comment("掉落物穿墙可见（关闭时会做视线遮挡检测，稍耗性能）").define("effects.xray", false);
   public static final DoubleValue STRENGTH = B.comment("光晕强度 0.0-2.0").defineInRange("appearance.strength", 1.0, 0.0, 2.0);
   public static final DoubleValue RADIUS = B.comment("光溢出半径（模糊迭代强度）1-8").defineInRange("appearance.radius", 4.0, 1.0, 8.0);
   public static final IntValue RANGE = B.comment("掉落物高亮距离（格）4-96").defineInRange("appearance.range", 32, 4, 96);
   public static final IntValue ACCENT = B.comment("强调色 RGB（默认青色 0x38E1FF）").defineInRange("appearance.accentColor", 3727871, 0, 16777215);
   public static final BooleanValue PER_ITEM_COLOR = B.comment("按物品着色（关闭则统一强调色）").define("appearance.perItemColor", true);
   public static final ConfigValue<String> TIER_MODE = B.comment("性能档位：AUTO/ULTRA/HIGH/BALANCED/LITE/OFF").define("performance.mode", "AUTO");
   public static final DoubleValue BUDGET_MS = B.comment("自动挡帧时间预算（毫秒），超过则自动降档").defineInRange("performance.frameBudgetMs", 20.0, 5.0, 100.0);
   public static final IntValue MAX_EMITTERS = B.comment("单帧最多参与光效的发光物数量上限").defineInRange("performance.maxEmitters", 160, 8, 512);
   public static final IntValue DOWNSCALE = B.comment("遮罩分辨率降采样倍率：1 全分辨率 2 半 4 四分之一 6 六分之一").defineInRange("performance.downscale", 4, 1, 8);
   public static final ConfigValue<String> RULE_MODE = B.comment("哪些物品发光：RARITY(稀有及以上)/ENCHANTED(有附魔光泽)/ALL(全部)/WHITELIST(仅白名单)")
      .define("rules.mode", "RARITY");
   public static final ConfigValue<List<? extends String>> WHITELIST = B.comment("白名单物品 id，例如 minecraft:diamond")
      .define("rules.whitelist", Collections.emptyList());
   public static final ConfigValue<List<? extends String>> BLACKLIST = B.comment("黑名单物品 id，优先于白名单与规则").define("rules.blacklist", Collections.emptyList());
   public static final BooleanValue VR_STYLE = B.comment("使用仿 VR 界面风格").define("ui.vrStyle", true);
   public static final BooleanValue FPS_HUD = B.comment("显示帧率面板").define("hud.fpsEnabled", true);
   public static final BooleanValue HUD_COMPACT = B.comment("帧率面板精简为一行").define("hud.compact", false);
   public static final IntValue HUD_X = B.comment("帧率面板 X 位置（像素）").defineInRange("hud.x", 6, -4096, 4096);
   public static final IntValue HUD_Y = B.comment("帧率面板 Y 位置（像素）").defineInRange("hud.y", 6, -4096, 4096);
   public static final DoubleValue HAND_FORWARD = B.comment("手持光晕前移距离（格）").defineInRange("hand.forward", 0.55, 0.05, 2.0);
   public static final DoubleValue HAND_RIGHT = B.comment("手持光晕右移距离（格）").defineInRange("hand.right", 0.32, -1.0, 1.0);
   public static final DoubleValue HAND_DOWN = B.comment("手持光晕下移距离（格）").defineInRange("hand.down", 0.28, -1.0, 1.0);
   public static final DoubleValue HAND_SIZE = B.comment("手持光晕半径（格）").defineInRange("hand.size", 0.3, 0.05, 2.0);
   public static final ConfigSpec SPEC = B.build();

   private GlowConfig() {
   }

   public static boolean save() {
      pushToSpec();
      boolean saved = SPEC.save();
      if (!saved) {
         GlowPerfMod.LOG.warn("配置保存失败: glowperf-client");
      }
      return saved;
   }

   public static void pushToSpec() {
      ENABLED.set(GlowState.enabled);
      HAND.set(GlowState.hand);
      DROP.set(GlowState.drop);
      LIGHTING.set(GlowState.lighting);
      GUI.set(GlowState.gui);
      XRAY.set(GlowState.xray);
      STRENGTH.set((double)GlowState.strength);
      RADIUS.set((double)GlowState.radius);
      RANGE.set(GlowState.range);
      ACCENT.set(GlowState.accent);
      PER_ITEM_COLOR.set(GlowState.perItemColor);
      TIER_MODE.set(GlowState.tierMode.name());
      BUDGET_MS.set((double)GlowState.budgetMs);
      MAX_EMITTERS.set(GlowState.maxEmitters);
      DOWNSCALE.set(GlowState.downscale);
      RULE_MODE.set(GlowState.ruleMode.name());
      WHITELIST.set(GlowState.whitelist());
      BLACKLIST.set(GlowState.blacklist());
      VR_STYLE.set(GlowState.vrStyle);
      FPS_HUD.set(GlowState.fpsHud);
      HUD_COMPACT.set(GlowState.hudCompact);
      HUD_X.set(GlowState.hudX);
      HUD_Y.set(GlowState.hudY);
      HAND_FORWARD.set((double)GlowState.handForward);
      HAND_RIGHT.set((double)GlowState.handRight);
      HAND_DOWN.set((double)GlowState.handDown);
      HAND_SIZE.set((double)GlowState.handSize);
   }

   public static List<String> mutable(List<? extends String> in) {
      return in == null ? new ArrayList<>() : new ArrayList<>(in);
   }
}
