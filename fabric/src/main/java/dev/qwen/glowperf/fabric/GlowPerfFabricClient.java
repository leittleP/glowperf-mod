package dev.qwen.glowperf.fabric;

import dev.qwen.glowperf.glow.client.ItemGlowClient;
import dev.qwen.glowperf.glow.client.hud.FpsHud;
import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.client.hud.CoordHud;
import dev.qwen.glowperf.perf.client.hud.PerfHud;
import dev.qwen.glowperf.perf.client.map.MinimapHud;
import dev.qwen.glowperf.perf.command.GpCommand;
import dev.qwen.glowperf.perf.command.MapCommand;
import dev.qwen.glowperf.perf.command.ModeCommand;
import dev.qwen.glowperf.perf.command.PerfCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;

public final class GlowPerfFabricClient implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
         PerfCommand.register(dispatcher);
         GpCommand.register(dispatcher);
         MapCommand.register(dispatcher);
         ModeCommand.register(dispatcher);
      });
      ClientTickEvents.START_CLIENT_TICK.register(client -> PerfClient.onClientTick());
      HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
         Minecraft minecraft = Minecraft.getInstance();
         int width = minecraft.getWindow().getGuiScaledWidth();
         int height = minecraft.getWindow().getGuiScaledHeight();
         PerfHud.INSTANCE.render(graphics, tickDelta, width, height);
         CoordHud.INSTANCE.render(graphics, tickDelta, width, height);
         MinimapHud.INSTANCE.render(graphics, tickDelta, width, height);
         FpsHud.INSTANCE.render(graphics, tickDelta, width, height);
      });
      WorldRenderEvents.AFTER_ENTITIES.register(context -> PerfClient.onRenderFrame());
      WorldRenderEvents.LAST.register(
         context -> ItemGlowClient.onWorldRender(context.matrixStack(), context.frustum(), context.projectionMatrix(), context.tickDelta())
      );
   }
}
