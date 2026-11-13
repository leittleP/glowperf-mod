package dev.qwen.glowperf.fabric.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.qwen.glowperf.glow.client.GuiIconGlow;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
   @Inject(
      method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
      at = @At("HEAD")
   )
   private void glowperf$renderIconGlow(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
      if (GuiIconGlow.INSTANCE.render((GuiGraphics)(Object)this, font, stack, x, y)) {
         RenderSystem.enableDepthTest();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
      }
   }
}
