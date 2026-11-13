package dev.qwen.glowperf.forge;

import dev.qwen.glowperf.glow.client.ItemGlowClient;
import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.command.GpCommand;
import dev.qwen.glowperf.perf.command.MapCommand;
import dev.qwen.glowperf.perf.command.ModeCommand;
import dev.qwen.glowperf.perf.command.PerfCommand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "glowperf", value = {Dist.CLIENT})
public final class ForgeClientEvents {
   private ForgeClientEvents() {
   }

   @SubscribeEvent
   public static void onCommands(RegisterClientCommandsEvent event) {
      PerfCommand.register(event.getDispatcher());
      GpCommand.register(event.getDispatcher());
      MapCommand.register(event.getDispatcher());
      ModeCommand.register(event.getDispatcher());
   }

   @SubscribeEvent
   public static void onLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
         PerfClient.onRenderFrame();
      } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
         ItemGlowClient.onWorldRender(event.getPoseStack(), event.getFrustum(), event.getProjectionMatrix(), event.getPartialTick());
      }
   }

   @SubscribeEvent
   public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
      if (PerfClient.shouldCullLiving(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
      if (PerfClient.shouldCullPlayer(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.START) {
         PerfClient.onClientTick();
      }
   }
}
