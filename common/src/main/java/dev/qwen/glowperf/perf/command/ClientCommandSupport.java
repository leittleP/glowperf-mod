package dev.qwen.glowperf.perf.command;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class ClientCommandSupport {
   private ClientCommandSupport() {
   }

   static void success(Component message) {
      display(message);
   }

   static void failure(Component message) {
      display(message);
   }

   private static void display(Component message) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.displayClientMessage(message, false);
      }
   }
}
