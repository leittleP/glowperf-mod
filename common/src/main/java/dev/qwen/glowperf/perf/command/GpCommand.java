package dev.qwen.glowperf.perf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.qwen.glowperf.glow.client.ItemGlowClient;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.glow.render.Tier;
import dev.qwen.glowperf.perf.SlowLog;
import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.core.Counters;
import dev.qwen.glowperf.perf.core.EntityRules;
import dev.qwen.glowperf.perf.core.FrameProbe;
import dev.qwen.glowperf.perf.core.MemWatch;
import dev.qwen.glowperf.perf.core.PerfState;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class GpCommand {
   private static final int KINDS = 64;
   private static final long[] PACK = new long[64];
   private static final int[] IDS = new int[64];
   private static final int TABLE = 256;
   private static final int[] SLOT = new int[256];

   private GpCommand() {
   }

   public static <S> void register(CommandDispatcher<S> dispatcher) {
      dispatcher.register(
         LiteralArgumentBuilder.<S>literal("gp")
            .executes(GpCommand::help)
            .then(LiteralArgumentBuilder.<S>literal("scan").executes(GpCommand::scan))
            .then(LiteralArgumentBuilder.<S>literal("top").executes(GpCommand::top))
            .then(LiteralArgumentBuilder.<S>literal("slow").executes(GpCommand::slow))
            .then(LiteralArgumentBuilder.<S>literal("posedit").executes(GpCommand::posedit))
            .then(LiteralArgumentBuilder.<S>literal("help").executes(GpCommand::help))
      );
   }

   private static int posedit(CommandContext<?> var0) {
      return MapCommand.openPosEditor(var0);
   }

   private static int scan(CommandContext<?> var0) {
      say(var0, "—— 光影性能体检 ——");
      say(
         var0,
         String.format("FPS %.0f ｜ 帧 %.1f ms ｜ P95 %.1f ms ｜ 长帧 %d 次", FrameProbe.fps(), FrameProbe.frameAvg(), FrameProbe.frameP95(), Counters.longFramesTotal)
      );
      say(
         var0,
         String.format(
            "TPS %.1f ｜ tick %.1f ms ｜ 内存 %d/%d MB（%.0f%%，趋势 %+d MB）",
            FrameProbe.tps(),
            FrameProbe.tickAvg(),
            MemWatch.usedMb(),
            MemWatch.maxMb(),
            MemWatch.ratio() * 100.0F,
            MemWatch.trendMb()
         )
      );
      say(
         var0,
         String.format(
            "性能档 %s ｜ 光影 %s ｜ 发光物 %d ｜ 本刻剔除 %d ｜ 冻AI %d",
            PerfState.effective.name(),
            GlowState.enabled ? GlowState.activeTier.name() : "关闭",
            GlowState.lastEmitterCount,
            Counters.shownCulled,
            EntityRules.frozenCount()
         )
      );
      String var1 = PerfClient.conflicts();
      if (!var1.isEmpty()) {
         say(var0, "检测到的相关模组：");

         for (String var5 : var1.split("\n")) {
            if (!var5.isBlank()) {
               say(var0, "   " + var5);
            }
         }
      }

      float var6 = FrameProbe.tps();
      float var7 = FrameProbe.frameP95();
      float var8 = PerfState.budgetMs;
      if (var6 < 19.0F) {
         say(var0, "判断：TPS 偏低，瓶颈在逻辑侧（实体/方块实体 tick）。建议 /perf top 找出刷怪来源，或考虑开 /perf noai on。");
      } else if (var7 > var8 * 1.5F) {
         say(var0, "判断：TPS 正常但帧时间尾部偏高，瓶颈在渲染侧。建议 /perf distance 调小，或 /performance optimization severe。");
      } else if (MemWatch.ratio() > PerfState.memSoftLimit) {
         say(var0, "判断：堆占用偏高。建议 /performance optimization severe 收紧各项，并把启动器内存调到 3-4 GB（不是越大越好）。");
      } else {
         say(var0, "判断：当前没有明显瓶颈，保持现状即可。");
      }

      return 1;
   }

   private static int top(CommandContext<?> var0) {
      Minecraft var1 = Minecraft.getInstance();
      if (var1.level == null) {
         fail(var0, "需要先进入世界");
         return 0;
      } else {
         Arrays.fill(SLOT, 0);

         for (int var2 = 0; var2 < 64; var2++) {
            IDS[var2] = 0;
            PACK[var2] = 0L;
         }

         int var12 = 0;
         int var3 = 0;

         for (Entity var5 : var1.level.entitiesForRendering()) {
            var3++;
            EntityType var6 = var5.getType();
            int var7 = System.identityHashCode(var6) & 0xFF;
            int var8 = -1;

            for (int var9 = 0; var9 < 6; var9++) {
               int var10 = var7 + var9 & 0xFF;
               if (SLOT[var10] == 0) {
                  if (var12 < 64) {
                     SLOT[var10] = var12 + 1;
                     IDS[var12] = System.identityHashCode(var6);
                     PACK[var12] = 0L;
                     var8 = var12++;
                  }
                  break;
               }

               if (IDS[SLOT[var10] - 1] == System.identityHashCode(var6)) {
                  var8 = SLOT[var10] - 1;
                  break;
               }
            }

            if (var8 >= 0) {
               PACK[var8] = PACK[var8] + 4294967296L;
            }
         }

         if (var3 == 0) {
            say(var0, "当前没有加载中的实体");
            return 1;
         } else {
            long[] var13 = Arrays.copyOf(PACK, var12);
            Arrays.sort(var13);
            say(var0, "加载实体共 " + var3 + " 个，按类型排名（前 " + Math.min(10, var12) + "）：");
            int var14 = 0;

            for (int var15 = var12 - 1; var15 >= 0 && var14 < 10; var14++) {
               long var16 = var13[var15];
               int var17 = (int)(var16 >> 32);
               int var18 = (int)var16;
               String var11 = nameByIdHash(var18);
               say(var0, String.format("   %5d  %s", var17, var11));
               var15--;
            }

            return 1;
         }
      }
   }

   private static String nameByIdHash(int var0) {
      for (EntityType var2 : BuiltInRegistries.ENTITY_TYPE) {
         if (System.identityHashCode(var2) == var0) {
            ResourceLocation var3 = BuiltInRegistries.ENTITY_TYPE.getKey(var2);
            return var3 == null ? "未知类型" : var3.toString();
         }
      }

      return "未知类型";
   }

   private static int slow(CommandContext<?> var0) {
      String[] var1 = SlowLog.recent(8);
      if (var1.length == 0) {
         say(var0, "暂无卡顿快照（说明近期没出现超过阈值的长帧）");
         return 1;
      } else {
         say(var0, "最近 " + var1.length + " 次长帧快照（阈值 " + PerfState.longFrameMs + " ms）：");

         for (String var5 : var1) {
            say(var0, "   " + var5);
         }

         return 1;
      }
   }

   private static int help(CommandContext<?> var0) {
      say(var0, "/gp scan ｜ /gp top ｜ /gp slow ｜ /gp posedit");
      say(var0, "体检 ｜ 实体来源排名 ｜ 卡顿快照 ｜ HUD位置编辑");
      say(var0, "光影用 /light and shadow …，优化用 /performance optimization …，小地图用 /open the map");
      return 1;
   }

   private static void say(CommandContext<?> var0, String var1) {
      ClientCommandSupport.success(Component(var1));
   }

   private static Component Component(String var0) {
      return Component.literal("[光影性能] ").withStyle(ChatFormatting.AQUA).append(Component.literal(var0));
   }

   private static void fail(CommandContext<?> var0, String var1) {
      ClientCommandSupport.failure(Component.literal("[光影性能] " + var1).withStyle(ChatFormatting.RED));
   }
}
