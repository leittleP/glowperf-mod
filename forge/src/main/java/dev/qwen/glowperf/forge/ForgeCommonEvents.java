package dev.qwen.glowperf.forge;

import dev.qwen.glowperf.perf.PerfCommon;
import dev.qwen.glowperf.perf.client.map.MapEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "glowperf")
public final class ForgeCommonEvents {
   private ForgeCommonEvents() {
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.START) {
         PerfCommon.onServerTickStart();
      } else {
         PerfCommon.onServerTickEnd(event.getServer());
      }
   }

   @SubscribeEvent
   public static void onEntityJoin(EntityJoinLevelEvent event) {
      if (event.getLevel().isClientSide()) {
         return;
      }

      if (PerfCommon.onEntityJoin(event.getEntity(), event.loadedFromDisk())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onItemToss(ItemTossEvent event) {
      ItemStack stack = event.getEntity().getItem();
      if (!MapEvents.isProtectedBook(stack)) {
         return;
      }

      event.setCanceled(true);
      if (!event.getPlayer().level().isClientSide()) {
         event.getPlayer().getInventory().placeItemBackInInventory(stack.copy());
      }
   }

   @SubscribeEvent
   public static void onEntityLeave(EntityLeaveLevelEvent event) {
      PerfCommon.onEntityLeave(event.getEntity());
   }

   @SubscribeEvent
   public static void onLevelUnload(LevelEvent.Unload event) {
      if (event.getLevel() instanceof ServerLevel level) {
         PerfCommon.onLevelUnload(level);
      }
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      MapEvents.onLogin(event.getEntity());
   }
}
