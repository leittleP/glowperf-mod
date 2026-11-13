package dev.qwen.glowperf.fabric.mixin.server;

import dev.qwen.glowperf.perf.PerfCommon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
   @Inject(method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
   private void glowperf$guardFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (PerfCommon.onFreshEntitySpawn(entity)) {
         cir.setReturnValue(false);
      }
   }
}
