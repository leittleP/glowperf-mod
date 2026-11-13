package dev.qwen.glowperf.forge;

import dev.qwen.glowperf.glow.client.GuiIconGlow;
import dev.qwen.glowperf.glow.client.hud.FpsHud;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.perf.client.hud.CoordHud;
import dev.qwen.glowperf.perf.client.hud.PerfHud;
import dev.qwen.glowperf.perf.client.map.MinimapHud;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "glowperf", bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public final class ForgeClientSetup {
   private ForgeClientSetup() {
   }

   @SubscribeEvent
   public static void onOverlays(RegisterGuiOverlaysEvent event) {
      event.registerAboveAll("glowperf_panel", (gui, graphics, partialTick, width, height) -> PerfHud.INSTANCE.render(graphics, partialTick, width, height));
      event.registerAboveAll("glowperf_coords", (gui, graphics, partialTick, width, height) -> CoordHud.INSTANCE.render(graphics, partialTick, width, height));
      event.registerAboveAll("glowperf_minimap", (gui, graphics, partialTick, width, height) -> MinimapHud.INSTANCE.render(graphics, partialTick, width, height));
      event.registerAboveAll("itemglow_fps", (gui, graphics, partialTick, width, height) -> FpsHud.INSTANCE.render(graphics, partialTick, width, height));
   }

   @SubscribeEvent
   public static void onDecorations(RegisterItemDecorationsEvent event) {
      BuiltInRegistries.ITEM.forEach(item -> event.register(item, GuiIconGlow.INSTANCE::render));
   }

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(GlowState::readFromConfig);
   }
}
