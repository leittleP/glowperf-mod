package dev.qwen.glowperf.fabric.mixin.common;

import dev.qwen.glowperf.perf.client.map.MapEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDropMixin {
   @Inject(
      method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void glowperf$blockProtectedBook(ItemStack stack, boolean dropAround, CallbackInfoReturnable<ItemEntity> cir) {
      if (!MapEvents.isProtectedBook(stack) || MapEvents.isInternalDrop()) {
         return;
      }

      Player self = (Player)(Object)this;
      if (!self.level().isClientSide) {
         MapEvents.returnBook(self, stack);
      }

      cir.setReturnValue(null);
   }
}
