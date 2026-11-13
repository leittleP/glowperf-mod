package dev.qwen.glowperf.perf.client;

import dev.qwen.glowperf.GlowPerfMod;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.glow.render.Tier;
import dev.qwen.glowperf.perf.SlowLog;
import dev.qwen.glowperf.perf.config.PerfConfig;
import dev.qwen.glowperf.perf.core.Counters;
import dev.qwen.glowperf.perf.core.EntityRules;
import dev.qwen.glowperf.perf.core.FrameProbe;
import dev.qwen.glowperf.perf.core.GovernorLite;
import dev.qwen.glowperf.perf.core.MemWatch;
import dev.qwen.glowperf.perf.core.PerfState;
import dev.qwen.glowperf.platform.LoaderPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PerfClient {
   private static long lastFrameNs;
   private static double camX;
   private static double camY;
   private static double camZ;
   private static long camStamp = -1L;
   private static String conflicts = "";
   private static ClientLevel lastLevel;
   private static int protectTicks;
   private static int glowHoldTicks;

   private PerfClient() {
   }

   public static void onRenderFrame() {
      long now = System.nanoTime();
      float frameMs = 0.0F;
      if (lastFrameNs != 0L) {
         frameMs = (float)(now - lastFrameNs) / 1000000.0F;
         if (frameMs > 0.0F && frameMs < 5000.0F) {
            FrameProbe.frame(frameMs);
            if (frameMs > (float)PerfState.longFrameMs) {
               SlowLog.capture(frameMs, FrameProbe.tps());
            }
         }
      }

      lastFrameNs = now;
      FrameProbe.step();
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != lastLevel) {
         lastLevel = level;
         protectTicks = 60;
         EntityRules.resetAllFrozen();
         conflicts = "";
      }

      if (protectTicks > 0) {
         protectTicks--;
      }

      if (PerfState.enabled && PerfState.preset == PerfState.Preset.AUTO) {
         GovernorLite.tick();
         PerfState.effective = GovernorLite.current();
      } else if (PerfState.enabled) {
         PerfState.effective = PerfState.preset;
      } else {
         PerfState.effective = PerfState.Preset.OFF;
      }

      GlowState.hudSuppressed = PerfState.enabled && PerfState.panel;
      controlGlow();
   }

   private static void controlGlow() {
      if (PerfState.glowMode != PerfState.GlowMode.IGNORE) {
         if (PerfState.glowMode == PerfState.GlowMode.OFF) {
            applyGlow(2);
         } else if (PerfState.glowMode == PerfState.GlowMode.LITE) {
            applyGlow(1);
         } else if (PerfState.glowMode == PerfState.GlowMode.ON) {
            if (PerfState.glowApplied != 0) {
               applyGlow(0);
            }
         } else if (!PerfState.enabled) {
            if (PerfState.glowApplied != 0) {
               applyGlow(0);
            }
         } else {
            float p95 = FrameProbe.frameP95();
            float budget = PerfState.budgetMs;
            float tail = budget * Math.max(1.1F, PerfState.tailRatio);
            int target;
            if (protectTicks > 0) {
               target = 2;
            } else if (p95 > tail * 1.6F || MemWatch.ratio() > 0.93F) {
               target = 2;
            } else if (p95 > tail || MemWatch.ratio() > PerfState.memSoftLimit) {
               target = 1;
            } else if (p95 < budget * 0.7F && MemWatch.ratio() < PerfState.memSoftLimit - 0.08F) {
               target = 0;
            } else {
               target = PerfState.glowApplied;
            }

            if (target != PerfState.glowApplied) {
               if (target > PerfState.glowApplied) {
                  applyGlow(target);
                  glowHoldTicks = 0;
               } else if (++glowHoldTicks >= 120) {
                  applyGlow(target);
                  glowHoldTicks = 0;
               }
            }
         }
      }
   }

   public static void applyGlowPublic(int level) {
      applyGlow(level);
   }

   private static void applyGlow(int level) {
      switch (level) {
         case 1:
            GlowState.enabled = true;
            GlowState.tierMode = Tier.LITE;
            GlowState.activeTier = Tier.LITE;
            break;
         case 2:
            GlowState.enabled = false;
            break;
         default:
            GlowState.enabled = true;
            if (PerfState.glowRestore) {
               GlowState.tierMode = Tier.AUTO;
               GlowState.applyTier();
            }
      }

      PerfState.glowApplied = level;
   }

   public static boolean shouldCullLiving(LivingEntity entity) {
      if (PerfState.enabled && PerfState.cullLiving && PerfState.effective != PerfState.Preset.OFF) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            try {
               refreshCam(mc);
               double distance = entity.distanceToSqr(camX, camY, camZ);
               AABB box = entity.getBoundingBox();
               if (EntityRules.shouldCullLiving(entity, distance, Math.max(box.getXsize(), box.getZsize()))) {
                  Counters.cullLivingOne();
                  return true;
               }
            } catch (Throwable error) {
               disableRenderModules(error);
            }
         }
      }

      return false;
   }

   public static boolean shouldCullPlayer(Player player) {
      if (PerfState.enabled && PerfState.cullPlayer && PerfState.effective != PerfState.Preset.OFF) {
         Minecraft mc = Minecraft.getInstance();

         try {
            refreshCam(mc);
            double distance = player.distanceToSqr(camX, camY, camZ);
            Entity cameraEntity = mc.getCameraEntity();
            if (EntityRules.shouldCullPlayer(player, cameraEntity, distance)) {
               Counters.cullPlayerOne();
               return true;
            }
         } catch (Throwable error) {
            disableRenderModules(error);
         }
      }

      return false;
   }

   private static void disableRenderModules(Throwable error) {
      if (PerfState.cullLiving || PerfState.cullPlayer) {
         GlowPerfMod.LOG.warn("渲染剔除模块异常，已自我停用: {}", error.toString());
         PerfState.cullLiving = false;
         PerfState.cullPlayer = false;
      }
   }

   private static void refreshCam(Minecraft mc) {
      long frame = lastFrameNs;
      if (frame != camStamp) {
         camStamp = frame;
         Camera camera = mc.gameRenderer.getMainCamera();
         Vec3 position = camera.getPosition();
         camX = position.x;
         camY = position.y;
         camZ = position.z;
      }
   }

   public static void onClientTick() {
      Minecraft mc = Minecraft.getInstance();
      String warning = MemWatch.pollPendingWarn();
      if (warning != null && mc.player != null) {
         mc.player.displayClientMessage(Component.literal("[光影性能] " + warning).withStyle(ChatFormatting.YELLOW), false);
      }
   }

   static void say(Minecraft mc, String message) {
      if (mc.player != null) {
         mc.player.displayClientMessage(Component.literal("[光影性能] " + message).withStyle(ChatFormatting.AQUA), true);
      }
   }

   public static void persist() {
      PerfConfig.save();
   }

   public static String conflicts() {
      if (!conflicts.isEmpty()) {
         return conflicts;
      } else {
         StringBuilder found = new StringBuilder();
         String[][] known = new String[][]{
            {"entityculling", "Entity Culling（实体渲染剔除）", "与本模组 cull 功能重叠；建议二选一，或把本模组 distance 调大避免重复剔除"},
            {"dynamic_fps", "Dynamic FPS（后台降帧）", "互补，可共存"},
            {"seamless_loading_screen", "Seamless Loading Screen", "互补，可共存"},
            {"embeddium", "Embeddium（接管渲染管线）", "渲染剔除仍有效；区块类优化交给它"},
            {"rubidium", "Rubidium（接管渲染管线）", "同上"},
            {"sodium", "Sodium 移植（接管渲染管线）", "同上"},
            {"oculus", "Oculus/Iris 移植（光影）", "与本模组光效叠加开销，建议只留一套光影"},
            {"optifine", "OptiFine（大量改写渲染）", "可能吞掉部分 Forge 渲染事件，剔除或光效可能不生效"},
            {"celerity", "Celerity（OptiFine 兼容层）", "同上"},
            {"lithium", "Lithium（tick 优化）", "与 AI 冻结功能重叠，建议二选一"},
            {"canary", "Canary（tick 优化）", "同上"},
            {"ferritecore", "FerriteCore（内存优化）", "功能互补，可共存"},
            {"connectormod", "Sinytra Connector（Fabric 模组桥接）", "桥接层会改写事件分发，若光效不生效先单独验证本模组"}
         };

         for (String[] mod : known) {
            try {
               if (LoaderPlatform.isModLoaded(mod[0])) {
                  found.append(mod[0]).append(" → ").append(mod[1]).append("；").append(mod[2]).append("\n");
               }
            } catch (Throwable ignored) {
            }
         }

         int count = 0;

         try {
            count = LoaderPlatform.loadedModCount();
         } catch (Throwable ignored) {
         }

         conflicts = "已加载模组 " + count + " 个\n" + (found.length() == 0 ? "未发现已知重叠/冲突模组" : found.toString());
         return conflicts;
      }
   }

   public static void resetConflictsScan() {
      conflicts = "";
   }

   public static int protectedTicks() {
      return protectTicks;
   }
}
