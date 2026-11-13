package dev.qwen.glowperf.perf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.core.GovernorLite;
import dev.qwen.glowperf.perf.core.PerfState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 光影与性能优化档位的指令：
 * /light and shadow open|off|auto
 * /performance optimization off|mild|moderate|severe|t matter|auto
 */
public final class ModeCommand {
   private ModeCommand() {
   }

   public static <S> void register(CommandDispatcher<S> dispatcher) {
      dispatcher.register(
         LiteralArgumentBuilder.<S>literal("light")
            .then(
               LiteralArgumentBuilder.<S>literal("and")
                  .then(
                     LiteralArgumentBuilder.<S>literal("shadow")
                        .then(LiteralArgumentBuilder.<S>literal("open").executes(c -> setGlow(c, PerfState.GlowMode.ON)))
                        .then(LiteralArgumentBuilder.<S>literal("off").executes(c -> setGlow(c, PerfState.GlowMode.OFF)))
                        .then(LiteralArgumentBuilder.<S>literal("auto").executes(c -> setGlow(c, PerfState.GlowMode.AUTO)))
                        .then(
                           LiteralArgumentBuilder.<S>literal("fps")
                              .then(LiteralArgumentBuilder.<S>literal("on").executes(c -> setFps(c, true)))
                              .then(LiteralArgumentBuilder.<S>literal("off").executes(c -> setFps(c, false)))
                        )
                        .then(LiteralArgumentBuilder.<S>literal("settings").executes(c -> openGlowSettings()))
                  )
            )
      );
      LiteralArgumentBuilder<S> opt = LiteralArgumentBuilder.<S>literal("optimization")
         .then(LiteralArgumentBuilder.<S>literal("off").executes(c -> setLevel(c, PerfState.Preset.OFF)))
         .then(LiteralArgumentBuilder.<S>literal("mild").executes(c -> setLevel(c, PerfState.Preset.MILD)))
         .then(LiteralArgumentBuilder.<S>literal("moderate").executes(c -> setLevel(c, PerfState.Preset.MODERATE)))
         .then(LiteralArgumentBuilder.<S>literal("severe").executes(c -> setLevel(c, PerfState.Preset.SEVERE)))
         .then(LiteralArgumentBuilder.<S>literal("auto").executes(c -> setLevel(c, PerfState.Preset.AUTO)))
         .then(LiteralArgumentBuilder.<S>literal("tmatter").executes(c -> setLevel(c, PerfState.Preset.NO_MATTER)))
         .then(
            LiteralArgumentBuilder.<S>literal("t").then(LiteralArgumentBuilder.<S>literal("matter").executes(c -> setLevel(c, PerfState.Preset.NO_MATTER)))
         );
      dispatcher.register(LiteralArgumentBuilder.<S>literal("performance").then(opt));
   }

   private static int setFps(CommandContext<?> ctx, boolean on) {
      dev.qwen.glowperf.glow.core.GlowState.fpsHud = on;
      dev.qwen.glowperf.glow.config.GlowConfig.pushToSpec();
      dev.qwen.glowperf.glow.config.GlowConfig.save();
      say(ctx, on ? "光影帧率面板：显示" : "光影帧率面板：隐藏");
      return 1;
   }

   private static int openGlowSettings() {
      net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
      mc.setScreen(new dev.qwen.glowperf.glow.client.ui.GlowConfigScreen(null));
      return 1;
   }

   private static int setGlow(CommandContext<?> ctx, PerfState.GlowMode mode) {
      PerfState.glowMode = mode;
      PerfClient.applyGlowPublic(mode == PerfState.GlowMode.OFF ? 2 : (mode == PerfState.GlowMode.LITE ? 1 : 0));
      PerfClient.persist();
      say(ctx, "光影：" + mode.label() + (mode == PerfState.GlowMode.AUTO ? "（按帧时间自动开关）" : ""));
      return 1;
   }

   private static int setLevel(CommandContext<?> ctx, PerfState.Preset preset) {
      PerfState.preset = preset;
      PerfState.applyPreset();
      PerfClient.persist();
      String now = preset == PerfState.Preset.AUTO
         ? "自动（当前生效 " + GovernorLite.describe() + "）"
         : preset.label + "（当前生效 " + PerfState.effective.label + "）";
      say(ctx, "优化档位：" + now);
      return 1;
   }

   private static void say(CommandContext<?> ctx, String text) {
      ClientCommandSupport.success(Component.literal(text).withStyle(ChatFormatting.AQUA));
   }
}
