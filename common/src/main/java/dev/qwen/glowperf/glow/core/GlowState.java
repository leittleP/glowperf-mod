package dev.qwen.glowperf.glow.core;

import dev.qwen.glowperf.glow.config.GlowConfig;
import dev.qwen.glowperf.glow.render.Governor;
import dev.qwen.glowperf.glow.render.Tier;
import java.util.ArrayList;
import java.util.List;

public final class GlowState {
   public static volatile boolean enabled = true;
   public static volatile boolean hand = true;
   public static volatile boolean drop = true;
   public static volatile boolean lighting = true;
   public static volatile boolean gui = true;
   public static volatile boolean xray = false;
   public static volatile float strength = 1.0F;
   public static volatile float radius = 4.0F;
   public static volatile int range = 32;
   public static volatile int accent = 3727871;
   public static volatile boolean perItemColor = true;
   public static volatile Tier tierMode = Tier.AUTO;
   public static volatile float budgetMs = 20.0F;
   public static volatile int maxEmitters = 160;
   public static volatile int downscale = 4;
   public static volatile Tier activeTier = Tier.HIGH;
   public static volatile boolean glAvailable = true;
   public static volatile GlowState.RuleMode ruleMode = GlowState.RuleMode.RARITY;
   private static volatile List<String> white = new ArrayList<>();
   private static volatile List<String> black = new ArrayList<>();
   public static volatile boolean vrStyle = true;
   public static volatile boolean fpsHud = true;
   public static volatile boolean hudCompact = false;
   public static volatile int hudX = 6;
   public static volatile int hudY = 6;
   public static volatile float handForward = 0.55F;
   public static volatile float handRight = 0.32F;
   public static volatile float handDown = 0.28F;
   public static volatile float handSize = 0.3F;
   public static volatile boolean hudSuppressed;
   public static volatile int lastEmitterCount;
   public static volatile int lastBlurPasses;
   public static volatile float lastPipelineMs;

   private GlowState() {
   }

   public static List<String> whitelist() {
      return white;
   }

   public static List<String> blacklist() {
      return black;
   }

   public static void setWhitelist(List<String> v) {
      white = v;
      GlowRules.invalidate();
   }

   public static void setBlacklist(List<String> v) {
      black = v;
      GlowRules.invalidate();
   }

   public static void readFromConfig() {
      enabled = (Boolean)GlowConfig.ENABLED.get();
      hand = (Boolean)GlowConfig.HAND.get();
      drop = (Boolean)GlowConfig.DROP.get();
      lighting = (Boolean)GlowConfig.LIGHTING.get();
      gui = (Boolean)GlowConfig.GUI.get();
      xray = (Boolean)GlowConfig.XRAY.get();
      strength = ((Double)GlowConfig.STRENGTH.get()).floatValue();
      radius = ((Double)GlowConfig.RADIUS.get()).floatValue();
      range = (Integer)GlowConfig.RANGE.get();
      accent = (Integer)GlowConfig.ACCENT.get();
      perItemColor = (Boolean)GlowConfig.PER_ITEM_COLOR.get();
      tierMode = Tier.parse((String)GlowConfig.TIER_MODE.get());
      budgetMs = ((Double)GlowConfig.BUDGET_MS.get()).floatValue();
      maxEmitters = (Integer)GlowConfig.MAX_EMITTERS.get();
      downscale = (Integer)GlowConfig.DOWNSCALE.get();
      ruleMode = GlowState.RuleMode.parse((String)GlowConfig.RULE_MODE.get());
      white = GlowConfig.mutable((List<? extends String>)GlowConfig.WHITELIST.get());
      black = GlowConfig.mutable((List<? extends String>)GlowConfig.BLACKLIST.get());
      vrStyle = (Boolean)GlowConfig.VR_STYLE.get();
      fpsHud = (Boolean)GlowConfig.FPS_HUD.get();
      hudCompact = (Boolean)GlowConfig.HUD_COMPACT.get();
      hudX = (Integer)GlowConfig.HUD_X.get();
      hudY = (Integer)GlowConfig.HUD_Y.get();
      handForward = ((Double)GlowConfig.HAND_FORWARD.get()).floatValue();
      handRight = ((Double)GlowConfig.HAND_RIGHT.get()).floatValue();
      handDown = ((Double)GlowConfig.HAND_DOWN.get()).floatValue();
      handSize = ((Double)GlowConfig.HAND_SIZE.get()).floatValue();
      applyTier();
      GlowRules.invalidate();
   }

   public static void applyTier() {
      activeTier = tierMode == Tier.AUTO ? Governor.current() : tierMode;
   }

   public static boolean bloomActive() {
      return enabled && glAvailable && activeTier != Tier.OFF && activeTier.pipelineEnabled && (hand || drop);
   }

   public static boolean haloActive() {
      return enabled && glAvailable && activeTier != Tier.OFF;
   }

   public static void toggleMaster() {
      enabled = !enabled;
      if (!enabled) {
         activeTier = Tier.OFF;
      } else {
         applyTier();
      }
   }

   public static void resetDefaults() {
      enabled = true;
      hand = true;
      drop = true;
      lighting = true;
      gui = true;
      xray = false;
      strength = 1.0F;
      radius = 4.0F;
      range = 32;
      accent = 3727871;
      perItemColor = true;
      tierMode = Tier.AUTO;
      budgetMs = 20.0F;
      maxEmitters = 160;
      downscale = 4;
      ruleMode = GlowState.RuleMode.RARITY;
      white = new ArrayList<>();
      black = new ArrayList<>();
      vrStyle = true;
      fpsHud = true;
      hudCompact = false;
      hudX = 6;
      hudY = 6;
      handForward = 0.55F;
      handRight = 0.32F;
      handDown = 0.28F;
      handSize = 0.3F;
      applyTier();
      GlowRules.invalidate();
   }

   public static enum RuleMode {
      RARITY,
      ENCHANTED,
      ALL,
      WHITELIST;

      public static GlowState.RuleMode parse(String s) {
         if (s == null) {
            return RARITY;
         } else {
            for (GlowState.RuleMode m : values()) {
               if (m.name().equalsIgnoreCase(s.trim())) {
                  return m;
               }
            }

            return RARITY;
         }
      }
   }
}
