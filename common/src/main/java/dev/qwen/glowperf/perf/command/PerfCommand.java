package dev.qwen.glowperf.perf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qwen.glowperf.GlowPerfMod;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.client.hud.PerfHud;
import dev.qwen.glowperf.perf.client.ui.PerfConfigScreen;
import dev.qwen.glowperf.perf.config.PerfConfig;
import dev.qwen.glowperf.perf.core.Counters;
import dev.qwen.glowperf.perf.core.EntityRules;
import dev.qwen.glowperf.perf.core.FrameProbe;
import dev.qwen.glowperf.perf.core.GovernorLite;
import dev.qwen.glowperf.perf.core.MemWatch;
import dev.qwen.glowperf.perf.core.PerfState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.ClickEvent.Action;

public final class PerfCommand {
   private static <S> SuggestionProvider<S> onOffSuggestions() {
      return (ctx, b) -> {
         b.suggest("on");
         b.suggest("off");
         return CompletableFuture.completedFuture(b.build());
      };
   }

   private PerfCommand() {
   }

   public static <S> void register(CommandDispatcher<S> dispatcher) {
      dispatcher.register(
         LiteralArgumentBuilder.<S>literal("perf")
            .executes(ctx -> status(ctx))
            .then(LiteralArgumentBuilder.<S>literal("help").executes(ctx -> help(ctx)))
            .then(LiteralArgumentBuilder.<S>literal("on").executes(ctx -> master(ctx, true)))
            .then(LiteralArgumentBuilder.<S>literal("off").executes(ctx -> master(ctx, false)))
            .then(mod("cull", "远处生物渲染剔除", v -> PerfState.cullLiving = v))
            .then(mod("player", "远处玩家渲染剔除", v -> PerfState.cullPlayer = v))
            .then(mod("spawn", "新生物生成护栏", v -> PerfState.spawnCap = v))
            .then(mod("noai", "远处闲置 AI 冻结", v -> PerfState.idleNoAi = v, () -> EntityRules.resetAllFrozen()))
            .then(mod("mem", "堆内存看护", v -> PerfState.memWatch = v))
            .then(mod("probe", "帧与 tick 诊断", v -> PerfState.probe = v))
            .then(mod("panel", "性能面板", v -> PerfState.panel = v))
            .then(mod("compact", "紧凑面板", v -> PerfState.panelCompact = v))
            .then(num("distance", 16, 256, v -> PerfState.cullDistance = v, "生物保留距离"))
            .then(num("distanceSmall", 8, 256, v -> PerfState.cullDistanceSmall = v, "小体型保留距离"))
            .then(num("playerDistance", 32, 256, v -> PerfState.cullPlayerDistance = v, "玩家保留距离"))
            .then(num("cap", 16, 512, v -> PerfState.spawnCapCount = v, "生物数量上限"))
            .then(num("noaiDistance", 32, 256, v -> PerfState.idleNoAiDistance = v, "AI 冻结距离"))
            .then(num("budget", 5, 200, v -> PerfState.budgetMs = (float)v, "帧预算"))
            .then(num("longFrame", 20, 2000, v -> PerfState.longFrameMs = v, "长帧阈值"))
            .then(LiteralArgumentBuilder.<S>literal("move").then(arg("x", (ctx, v) -> moveX(ctx, v))))
            .then(LiteralArgumentBuilder.<S>literal("movey").then(arg("y", (ctx, v) -> moveY(ctx, v))))
            .then(LiteralArgumentBuilder.<S>literal("allow").then(arg("type", (ctx, v) -> allow(ctx, v, true))))
            .then(LiteralArgumentBuilder.<S>literal("disallow").then(arg("type", (ctx, v) -> allow(ctx, v, false))))
            .then(LiteralArgumentBuilder.<S>literal("gc").executes(ctx -> gc(ctx)))
            .then(LiteralArgumentBuilder.<S>literal("stats").executes(ctx -> stats(ctx)))
            .then(LiteralArgumentBuilder.<S>literal("conflicts").executes(ctx -> conflicts(ctx)))
            .then(LiteralArgumentBuilder.<S>literal("settings").executes(ctx -> openPanel(ctx)))
            .then(LiteralArgumentBuilder.<S>literal("save").executes(ctx -> save(ctx)))
            .then(LiteralArgumentBuilder.<S>literal("reload").executes(ctx -> reload(ctx)))
            .then(LiteralArgumentBuilder.<S>literal("reset").executes(ctx -> reset(ctx)))
      );
   }

   private static <S> LiteralArgumentBuilder<S> mod(String name, String cn, PerfCommand.BoolSetter setter) {
      return mod(name, cn, setter, null);
   }

   private static <S> LiteralArgumentBuilder<S> mod(String name, String cn, PerfCommand.BoolSetter setter, PerfCommand.Runnable0 after) {
      return LiteralArgumentBuilder.<S>literal(name).then(arg("state", (ctx, v) -> {
         Boolean b = parseBool(v);
         if (b == null) {
            return 0;
         } else {
            setter.set(b);
            if (after != null) {
               after.run();
            }

            ok(ctx, cn + (b ? " 已开启" : " 已关闭"));
            return 1;
         }
      }));
   }

   private static <S> LiteralArgumentBuilder<S> num(String name, int lo, int hi, PerfCommand.IntSetter setter, String cn) {
      return LiteralArgumentBuilder.<S>literal(name).then(arg("value", (ctx, v) -> {
         Integer n = parseInt(v, lo, hi);
         if (n == null) {
            return 0;
         } else {
            setter.set(n);
            ok(ctx, cn + " = " + n);
            return 1;
         }
      }));
   }

   private static <S> RequiredArgumentBuilder<S, String> arg(String name, PerfCommand.Act act) {
      return RequiredArgumentBuilder.<S, String>argument(name, StringArgumentType.string())
         .suggests(PerfCommand.<S>onOffSuggestions())
         .executes(ctx -> act.run(ctx, StringArgumentType.getString(ctx, name)));
   }

   private static Boolean parseBool(String v) {
      String s = v.toLowerCase(Locale.ROOT);
      if (s.equals("on") || s.equals("true") || s.equals("1") || s.equals("开")) {
         return Boolean.TRUE;
      } else if (!s.equals("off") && !s.equals("false") && !s.equals("0") && !s.equals("关")) {
         fail("只能填 on / off，收到：" + v);
         return null;
      } else {
         return Boolean.FALSE;
      }
   }

   private static Integer parseInt(String v, int lo, int hi) {
      try {
         int n = Integer.parseInt(v.trim());
         if (n >= lo && n <= hi) {
            return n;
         } else {
            fail(String.format("取值范围 %d ~ %d", lo, hi));
            return null;
         }
      } catch (NumberFormatException var5) {
         fail("这不是整数：" + v);
         return null;
      }
   }

   private static int master(CommandContext<?> ctx, boolean on) {
      PerfState.enabled = on;
      if (!on) {
         PerfState.effective = PerfState.Preset.OFF;
         EntityRules.resetAllFrozen();
         PerfClient.applyGlowPublic(0);
      } else {
         PerfState.applyPreset();
      }

      ok(ctx, "性能优化" + (on ? " 已开启" : " 已关闭"));
      return 1;
   }

   private static int moveX(CommandContext<?> ctx, String v) {
      Integer x = parseInt(v, -4096, 4096);
      if (x == null) {
         return 0;
      } else {
         PerfState.panelX = x;
         ok(ctx, "面板 X = " + x + "（Y 用 /perf moveY <值>）");
         return 1;
      }
   }

   private static int moveY(CommandContext<?> ctx, String v) {
      Integer y = parseInt(v, -4096, 4096);
      if (y == null) {
         return 0;
      } else {
         PerfState.panelY = y;
         ok(ctx, "面板 Y = " + y);
         return 1;
      }
   }

   private static int allow(CommandContext<?> ctx, String typeId, boolean add) {
      String id = typeId.toLowerCase(Locale.ROOT);
      if (!id.contains(":")) {
         id = "minecraft:" + id;
      }

      List<String> list = new ArrayList<>(PerfState.exemptTypes());
      boolean changed = add ? list.add(id) : list.remove(id);
      if (!changed) {
         ok(ctx, (add ? "已在豁免表：" : "不在豁免表：") + id);
         return 1;
      } else {
         PerfState.setExempt(list);
         ok(ctx, (add ? "已加入永不剔除：" : "已移出豁免表：") + id);
         return 1;
      }
   }

   private static int gc(CommandContext<?> ctx) {
      long freed = MemWatch.hintGc();
      ok(ctx, String.format("已发出回收提示，释放约 %d MB（JVM 自行决定，不保证即时）", Math.max(0L, freed)));
      return 1;
   }

   private static int stats(CommandContext<?> ctx) {
      ok(ctx, PerfHud.statsLine());
      ok(
         ctx,
         String.format(
            "优化 %s（设置 %s）｜ 剔除渲染累计 %d ｜ 拦生成本刻 %d ｜ 冻AI %d ｜ 光影：%s",
            PerfState.effective.label,
            PerfState.preset.label,
            Counters.culledTotal,
            Counters.shownCapped,
            EntityRules.frozenCount(),
            PerfState.glowMode.label() + (PerfState.glowApplied == 1 ? "（柔光）" : (PerfState.glowApplied == 2 ? "（已关）" : ""))
         )
      );
      return 1;
   }

   private static int conflicts(CommandContext<?> ctx) {
      PerfClient.resetConflictsScan();
      String text = PerfClient.conflicts();

      for (String line : text.split("\n")) {
         if (!line.isBlank()) {
            ClientCommandSupport.success(Component.literal("  " + line).withStyle(ChatFormatting.GRAY));
         }
      }

      return 1;
   }

   private static int openPanel(CommandContext<?> ctx) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         fail("该指令需要客户端环境");
         return 0;
      } else {
         mc.setScreen(new PerfConfigScreen(mc.screen));
         return 1;
      }
   }

   private static int save(CommandContext<?> ctx) {
      ok(ctx, PerfConfig.save() ? "已写入 config/perfcore.toml" : "已记录（配置句柄未就绪）");
      return 1;
   }

   private static int reload(CommandContext<?> ctx) {
      PerfState.readFromConfig();
      ok(ctx, "已从配置重载（当前优化 " + PerfState.effective.label + "）");
      return 1;
   }

   private static int reset(CommandContext<?> ctx) {
      PerfState.resetDefaults();
      GovernorLite.reset();
      FrameProbe.reset();
      EntityRules.resetAllFrozen();
      MemWatch.clearWarn();
      ok(ctx, "已恢复默认（未写盘，/perf save 才保存）");
      return 1;
   }

   private static int status(CommandContext<?> ctx) {
      ok(ctx, PerfState.enabled ? "性能核心运行中：优化 " + PerfState.effective.label + "（设置 " + PerfState.preset.label + "）" : "性能核心已关闭，/perf on 开启");
      return 1;
   }

   private static int help(CommandContext<?> ctx) {
      MutableComponent head = Component.literal("性能核心 · 指令").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(3727871)).withUnderlined(true));
      ClientCommandSupport.success(head);
      String[][] rows = new String[][]{
         {"/perf on|off", "总开关"},
         {"/perf cull|player|spawn|noai|mem|probe|panel on|off", "分项开关（后三项激进项默认关）"},
         {"/performance optimization off|mild|moderate|severe|tmatter|auto", "优化档位（另有 t matter 写法）"},
         {"/light and shadow open|off|auto", "光影开关 / 自动"},
         {"/perf distance 16-256 · distanceSmall 8-256", "生物渲染保留距离"},
         {"/perf cap 16-512 · noaiDistance 32-256", "生成上限 / AI 冻结距离"},
         {"/perf budget 5-200 · longFrame 20-2000", "帧预算 / 长帧阈值"},
         {"/perf allow <实体类型id>", "加入永不剔除豁免表"},
         {"/perf move <x> · movey <y> · stats · conflicts", "面板位置 / 读数 / 冲突扫描"},
         {"/perf gc · settings · save · reload · reset", "回收提示 / 控制台 / 存盘 / 重载 / 默认"}
      };

      for (String[] r : rows) {
         String cmd = r[0];
         String desc = r[1];
         ClientCommandSupport.success(
            Component.literal("  " + cmd)
               .withStyle(
                  Style.EMPTY.withColor(TextColor.fromRgb(14087679)).withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, cmd.split(" ")[0] + " "))
               )
         );
         ClientCommandSupport.success(Component.literal("      → " + desc).withStyle(ChatFormatting.DARK_AQUA));
      }

      return rows.length;
   }

   public static boolean persist() {
      try {
         PerfConfig.pushToSpec();
         return PerfConfig.save();
      } catch (Throwable var1) {
         GlowPerfMod.LOG.warn("落盘失败: {}", var1.toString());
         return false;
      }
   }

   private static void ok(CommandContext<?> ctx, String msg) {
      ClientCommandSupport.success(Component.literal("[性能核心] ").withStyle(ChatFormatting.AQUA).append(Component.literal(msg)));
      PerfClient.persist();
   }

   private static void fail(String msg) {
      ClientCommandSupport.failure(Component.literal("[性能核心] " + msg).withStyle(ChatFormatting.RED));
   }

   static {
      if ("glowperf".isEmpty()) {
         throw new ExceptionInInitializerError();
      }
   }

   private interface Act {
      int run(CommandContext<?> var1, String var2);
   }

   private interface BoolSetter {
      void set(boolean var1);
   }

   private interface IntSetter {
      void set(int var1);
   }

   private interface Runnable0 {
      void run();
   }
}
