package dev.qwen.glowperf.perf.client.map;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * 传送工具：模组自身不再做任何权限限制，直接走原版 /tp。
 * 当当前世界未开启作弊（客户端命令树里看不到 tp）时，给出「开启作弊」的提示。
 */
public final class Teleport {
   private static String lastHint = "";
   private static long lastHintMs;

   private Teleport() {
   }

   /** 客户端命令树里是否存在 /tp 节点（服务器会按权限过滤命令树，作弊关掉时没有该节点）。 */
   public static boolean available() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.player.connection == null) {
         return false;
      }

      try {
         return mc.player.connection.getCommands() != null && mc.player.connection.getCommands().getRoot().getChild("tp") != null;
      } catch (Throwable t) {
         return false;
      }
   }

   public static void to(double x, double y, double z) {
      if (!ensure()) {
         return;
      }

      send("tp " + trim(x) + " " + trim(y) + " " + trim(z));
   }

   public static void toPlayer(String name) {
      if (name == null || name.isBlank() || !ensure()) {
         return;
      }

      send("tp " + name);
   }

   private static boolean ensure() {
      LocalPlayer p = Minecraft.getInstance().player;
      if (p == null) {
         return false;
      }

      if (!available()) {
         hint();
         return false;
      }

      return true;
   }

   private static void send(String cmd) {
      LocalPlayer p = Minecraft.getInstance().player;
      if (p == null || p.connection == null) {
         return;
      }

      try {
         p.connection.sendCommand(cmd);
      } catch (Throwable t) {
      }
   }

   private static void hint() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      if (p == null) {
         return;
      }

      String msg = "[光影性能] 传送需要开启作弊：单人世界创建时勾选「允许作弊」，或按 ESC → 对局域网开放 → 允许作弊；服务器需管理员权限。";
      long now = System.currentTimeMillis();
      if (msg.equals(lastHint) && now - lastHintMs < 3000L) {
         return;
      }

      lastHint = msg;
      lastHintMs = now;
      try {
         p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.YELLOW), false);
      } catch (Throwable t) {
      }
   }

   private static String trim(double v) {
      return v == Math.floor(v) ? String.valueOf((long)v) : String.format(Locale.ROOT, "%.2f", v);
   }
}
