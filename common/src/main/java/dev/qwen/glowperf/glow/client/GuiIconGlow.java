package dev.qwen.glowperf.glow.client;

import dev.qwen.glowperf.glow.core.GlowRules;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.glow.render.GlowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class GuiIconGlow {
   public static final GuiIconGlow INSTANCE = new GuiIconGlow();

   private GuiIconGlow() {
   }

   public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
      if (GlowState.haloActive() && GlowState.gui && !stack.isEmpty()) {
         int color = GlowRules.colorFor(stack);
         if (color == -1) {
            return false;
         } else {
            Minecraft mc = Minecraft.getInstance();
            float cx = (float)x + 8.0F;
            float cy = (float)y + 8.0F;
            GlowRenderer.drawScreenHalo(mc, cx, cy, 13.5F, color, 0.42F, 0);
            if (GlowState.lighting) {
               GlowRenderer.drawScreenHalo(mc, cx, cy, 9.5F, color, 0.26F, 2);
            }

            return true;
         }
      } else {
         return false;
      }
   }
}
