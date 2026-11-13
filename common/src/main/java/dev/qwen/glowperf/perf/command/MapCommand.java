package dev.qwen.glowperf.perf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.client.map.MapDraw;
import dev.qwen.glowperf.perf.client.map.MapScreen;
import dev.qwen.glowperf.perf.client.map.MapState;
import dev.qwen.glowperf.perf.client.map.PosEditorScreen;
import dev.qwen.glowperf.perf.client.map.Teleport;
import dev.qwen.glowperf.perf.client.map.Waypoint;
import dev.qwen.glowperf.perf.client.map.WaypointStore;
import dev.qwen.glowperf.perf.core.PerfState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 小地图指令：/open the map、/close the map、/minimap、/gp posedit。
 */
public final class MapCommand {
   private MapCommand() {
   }

   public static <S> void register(CommandDispatcher<S> dispatcher) {
      dispatcher.register(
         LiteralArgumentBuilder.<S>literal("open").then(LiteralArgumentBuilder.<S>literal("the").then(LiteralArgumentBuilder.<S>literal("map").executes(MapCommand::openMap)))
      );
      dispatcher.register(
         LiteralArgumentBuilder.<S>literal("close").then(LiteralArgumentBuilder.<S>literal("the").then(LiteralArgumentBuilder.<S>literal("map").executes(MapCommand::closeMap)))
      );
      dispatcher.register(LiteralArgumentBuilder.<S>literal("open").then(LiteralArgumentBuilder.<S>literal("map").executes(MapCommand::openMap)));
      dispatcher.register(LiteralArgumentBuilder.<S>literal("close").then(LiteralArgumentBuilder.<S>literal("map").executes(MapCommand::closeMap)));
      LiteralArgumentBuilder<S> minimap = LiteralArgumentBuilder.<S>literal("minimap").executes(c -> setMinimap(!PerfState.minimap));
      minimap.then(LiteralArgumentBuilder.<S>literal("on").executes(c -> setMinimap(true)));
      minimap.then(LiteralArgumentBuilder.<S>literal("off").executes(c -> setMinimap(false)));
      minimap.then(LiteralArgumentBuilder.<S>literal("toggle").executes(c -> setMinimap(!PerfState.minimap)));
      dispatcher.register(minimap);
      LiteralArgumentBuilder<S> wp = LiteralArgumentBuilder.<S>literal("wp");
      wp.then(
         LiteralArgumentBuilder.<S>literal("add")
            .executes(c -> addWaypoint(c, null))
            .then(RequiredArgumentBuilder.<S, String>argument("name", StringArgumentType.greedyString()).executes(c -> addWaypoint(c, StringArgumentType.getString(c, "name"))))
      );
      wp.then(
         LiteralArgumentBuilder.<S>literal("del")
            .then(RequiredArgumentBuilder.<S, String>argument("name", StringArgumentType.greedyString()).executes(c -> delWaypoint(c, StringArgumentType.getString(c, "name"))))
      );
      wp.then(
         LiteralArgumentBuilder.<S>literal("tp")
            .then(RequiredArgumentBuilder.<S, String>argument("name", StringArgumentType.greedyString()).executes(c -> tpWaypoint(c, StringArgumentType.getString(c, "name"))))
      );
      wp.then(LiteralArgumentBuilder.<S>literal("list").executes(MapCommand::listWaypoints));
      dispatcher.register(wp);
   }

   private static int addWaypoint(CommandContext<?> ctx, String name) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         fail("需要先进入世界");
         return 0;
      }

      String nm = name == null || name.isBlank() ? "路点" + (WaypointStore.all().size() + 1) : name.trim();
      Waypoint w = Waypoint.here(mc.player, mc.level, nm);
      if (!WaypointStore.add(w)) {
         fail("路点已达上限 " + WaypointStore.MAX + " 个");
         return 0;
      }

      WaypointStore.persist();
      ok(ctx, "已添加路点 " + w.pretty());
      return 1;
   }

   private static int delWaypoint(CommandContext<?> ctx, String name) {
      if (!WaypointStore.remove(name)) {
         fail("没有叫「" + name + "」的路点");
         return 0;
      }

      WaypointStore.persist();
      ok(ctx, "已删除路点 " + name);
      return 1;
   }

   private static int listWaypoints(CommandContext<?> ctx) {
      if (WaypointStore.all().isEmpty()) {
         ok(ctx, "暂无路点，用 /wp add <名字> 添加");
         return 1;
      }

      ok(ctx, "共有 " + WaypointStore.all().size() + " 个路点，/wp tp <名字> 传送：");
      for (Waypoint w : WaypointStore.all()) {
         ClientCommandSupport.success(Component.literal(" · " + w.pretty()));
      }

      return 1;
   }

   private static int tpWaypoint(CommandContext<?> ctx, String name) {
      Waypoint w = WaypointStore.find(name);
      Minecraft mc = Minecraft.getInstance();
      if (w == null) {
         fail("没有叫「" + name + "」的路点");
         return 0;
      }

      String dim = mc.level == null ? "overworld" : MapDraw.dimName(mc.level);
      if (!w.dim.equals(dim)) {
         fail("该路点在 " + w.dim + "，请先到对应维度");
         return 0;
      }

      Teleport.to(w.x, w.y, w.z);
      return 1;
   }

   private static int openMap(CommandContext<?> ctx) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         fail("需要先进入世界");
         return 0;
      }

      MapState.open = true;
      MapState.resetPan();
      mc.setScreen(new MapScreen());
      ok(ctx, "小地图已打开");
      return 1;
   }

   private static int closeMap(CommandContext<?> ctx) {
      Minecraft mc = Minecraft.getInstance();
      MapState.open = false;
      if (mc.screen instanceof MapScreen) {
         mc.setScreen(null);
      }

      ok(ctx, "小地图已关闭");
      return 1;
   }

   private static int setMinimap(boolean on) {
      PerfState.minimap = on;
      PerfClient.persist();
      ClientCommandSupport.success(
         Component.literal("[光影性能] " + (on ? "常驻小地图已开启" : "常驻小地图已关闭")).withStyle(ChatFormatting.AQUA)
      );

      return 1;
   }

   public static int openPosEditor(CommandContext<?> ctx) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         fail("需要先进入世界");
         return 0;
      }

      mc.setScreen(new PosEditorScreen());
      return 1;
   }

   private static void ok(CommandContext<?> ctx, String msg) {
      ClientCommandSupport.success(Component.literal("[光影性能] ").withStyle(ChatFormatting.AQUA).append(Component.literal(msg)));
   }

   private static void fail(String msg) {
      ClientCommandSupport.failure(Component.literal("[光影性能] " + msg).withStyle(ChatFormatting.RED));
   }
}
