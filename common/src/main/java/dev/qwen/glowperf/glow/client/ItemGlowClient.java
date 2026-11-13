package dev.qwen.glowperf.glow.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.qwen.glowperf.GlowPerfMod;
import dev.qwen.glowperf.glow.config.GlowConfig;
import dev.qwen.glowperf.glow.core.EmitterBuffer;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.glow.render.EmitterCollector;
import dev.qwen.glowperf.glow.render.FpsSampler;
import dev.qwen.glowperf.glow.render.GlowRenderer;
import dev.qwen.glowperf.glow.render.Governor;
import dev.qwen.glowperf.glow.render.Tier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public final class ItemGlowClient {
   private static final Matrix4f VIEW = new Matrix4f();
   private static long lastStageNs;

   private ItemGlowClient() {
   }

   static void chat(Minecraft mc, String msg) {
      if (mc.player != null) {
         mc.player.displayClientMessage(Component.literal(msg), true);
      }
   }

   public static void onWorldRender(PoseStack pose, Frustum frustum, Matrix4f projection, float partialTick) {
      long now = System.nanoTime();
      if (lastStageNs != 0L) {
         float ms = (float)(now - lastStageNs) / 1000000.0F;
         if (ms > 0.0F && ms < 5000.0F) {
            FpsSampler.sample(ms);
         }
      }

      lastStageNs = now;
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null && !mc.options.hideGui) {
         if (GlowState.tierMode == Tier.AUTO) {
            Governor.tick();
            GlowState.activeTier = Governor.current();
         } else {
            GlowState.activeTier = GlowState.tierMode;
         }

         if (!GlowState.bloomActive()) {
            EmitterBuffer.clear();
            GlowState.lastEmitterCount = 0;
            GlowState.lastBlurPasses = 0;
         } else {
            try {
               VIEW.set(RenderSystem.getModelViewStack().last().pose());
               EmitterCollector.collect(mc, frustum, VIEW);
               GlowRenderer.renderWorldPipeline(mc, VIEW, projection, pose, partialTick);
            } catch (Throwable error) {
               GlowState.glAvailable = false;
               GlowPerfMod.LOG.warn("物品光影：本帧渲染异常，已临时停用光效: {}", error.toString());
            }
         }
      } else {
         EmitterBuffer.clear();
      }
   }

   public static boolean saveConfig() {
      GlowConfig.pushToSpec();
      return GlowConfig.save();
   }
}
