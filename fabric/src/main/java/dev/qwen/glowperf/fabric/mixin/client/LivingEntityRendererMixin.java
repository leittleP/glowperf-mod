package dev.qwen.glowperf.fabric.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.qwen.glowperf.perf.client.PerfClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
   @Inject(
      method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void glowperf$cullEntity(
      LivingEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      CallbackInfo ci
   ) {
      boolean cull = entity instanceof Player player ? PerfClient.shouldCullPlayer(player) : PerfClient.shouldCullLiving(entity);
      if (cull) {
         ci.cancel();
      }
   }
}
