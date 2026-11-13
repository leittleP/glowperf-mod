package dev.qwen.glowperf.perf.client.hud;

import dev.qwen.glowperf.perf.client.ui.VrTheme;
import dev.qwen.glowperf.perf.core.PerfState;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

public final class CoordHud {
   public static final CoordHud INSTANCE = new CoordHud();
   private static final String[] DIRS = new String[]{"南", "西南", "西", "西北", "北", "东北", "东", "东南"};
   private static final int LINE = 11;
   private static final int LINES = 5;
   private static final int PAD = 6;
   private static String l1 = "坐标  --";
   private static String l2 = "";
   private static String l3 = "";
   private static String l4 = "";
   private static String l5 = "";
   private static int panelW = 150;
   private static int counter;

   private CoordHud() {
   }

   public void render(GuiGraphics g, float partialTick, int width, int height) {
      if (PerfState.coords) {
         Minecraft var6 = Minecraft.getInstance();
         if (!var6.options.hideGui) {
            ClientLevel var7 = var6.level;
            LocalPlayer var8 = var6.player;
            if (var7 != null && var8 != null) {
               if (++counter >= 3) {
                  counter = 0;
                  refresh(var6, var7, var8);
               }

               byte var9 = 63;
               int var10 = Math.max(2, Math.min(PerfState.coordsX, width - panelW - 2));
               int var11 = Math.max(2, Math.min(PerfState.coordsY, height - var9 - 2));
               VrTheme.panel(g, var10 - 6, var11 - 6, panelW, var9);
               VrTheme.scanline(g, var10 - 6, var11 - 6, panelW, var9);
               Font var12 = var6.font;
               g.drawString(var12, l1, var10, var11 - 1, -13049345);
               g.drawString(var12, l2, var10, var11 + 11 - 1, -2689537);
               g.drawString(var12, l3, var10, var11 + 22 - 1, -2689537);
               g.drawString(var12, l4, var10, var11 + 33 - 1, -7358785);
               g.drawString(var12, l5, var10, var11 + 44 - 1, -7358785);
            }
         }
      }
   }

   private static void refresh(Minecraft var0, ClientLevel var1, LocalPlayer var2) {
      double var3 = var2.getX();
      double var5 = var2.getY();
      double var7 = var2.getZ();
      BlockPos var9 = var2.blockPosition();
      int var10 = var9.getX();
      int var11 = var9.getY();
      int var12 = var9.getZ();
      int var13 = Math.floorDiv(var10, 16);
      int var14 = Math.floorDiv(var12, 16);
      int var15 = Math.floorMod(var10, 16);
      int var16 = Math.floorMod(var12, 16);
      float var17 = var2.getYRot();
      float var18 = var2.getXRot();
      int var19 = Math.floorMod(Math.round(var17 / 45.0F), 8);
      Vec3 var20 = var2.getDeltaMovement();
      double var21 = Math.sqrt(var20.x * var20.x + var20.z * var20.z) * 20.0;
      String var23 = var1.dimension().location().toString();
      String var24 = biomeName(var1, var9);
      l1 = String.format(Locale.ROOT, "坐标  %.2f / %.2f / %.2f", var3, var5, var7);
      l2 = String.format(Locale.ROOT, "方块  %d, %d, %d ｜ 区块 %d, %d (块内 %d, %d)", var10, var11, var12, var13, var14, var15, var16);
      l3 = String.format(Locale.ROOT, "朝向  %s  %.0f° ｜ 俯仰 %.0f°", DIRS[var19], var17, var18);
      l4 = String.format(Locale.ROOT, "维度  %s ｜ 群系 %s", shortId(var23), shortId(var24));
      l5 = String.format(Locale.ROOT, "换算  %s ｜ 速度 %.1f m/s", portalText(var23, var3, var7), var21);
      Font var25 = var0.font;
      int var26 = Math.max(Math.max(var25.width(l1), var25.width(l2)), Math.max(Math.max(var25.width(l3), var25.width(l4)), var25.width(l5)));
      panelW = Math.min(340, var26 + 12 + 4);
   }

   private static String biomeName(ClientLevel var0, BlockPos var1) {
      try {
         Holder var2 = var0.getBiome(var1);
         if (var2 == null) {
            return "?";
         } else {
            Registry var3 = var0.registryAccess().registryOrThrow(Registries.BIOME);
            ResourceLocation var4 = var3.getKey((Biome)var2.value());
            return var4 == null ? "?" : var4.toString();
         }
      } catch (Throwable var5) {
         return "?";
      }
   }

   private static String shortId(String var0) {
      return var0.startsWith("minecraft:") ? var0.substring("minecraft:".length()) : var0;
   }

   private static String portalText(String var0, double var1, double var3) {
      if ("minecraft:the_nether".equals(var0)) {
         return String.format(Locale.ROOT, "主世界 %.1f / %.1f", var1 * 8.0, var3 * 8.0);
      } else {
         return "minecraft:overworld".equals(var0) ? String.format(Locale.ROOT, "下界 %.1f / %.1f", var1 / 8.0, var3 / 8.0) : "无对应维度";
      }
   }

   public static void reset() {
      counter = 0;
      l1 = "坐标  --";
      l2 = "";
      l3 = "";
      l4 = "";
      l5 = "";
      panelW = 150;
   }
}
