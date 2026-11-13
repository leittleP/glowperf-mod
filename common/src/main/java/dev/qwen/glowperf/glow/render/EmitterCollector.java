package dev.qwen.glowperf.glow.render;

import dev.qwen.glowperf.glow.core.EmitterBuffer;
import dev.qwen.glowperf.glow.core.GlowRules;
import dev.qwen.glowperf.glow.core.GlowState;
import java.util.Arrays;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class EmitterCollector {
   private static final int TABLE = 256;
   private static final int[] IDS = new int[256];
   private static final byte[] VIS = new byte[256];
   private static final int[] STAMP = new int[256];
   private static int frameGroup;
   private static final AABB BOX = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

   private EmitterCollector() {
   }

   public static void collect(Minecraft mc, Frustum frustum, Matrix4f view) {
      EmitterBuffer.clear();
      if (GlowState.enabled && GlowState.activeTier != Tier.OFF) {
         int cap = GlowState.activeTier.effectiveCap(GlowState.maxEmitters);
         if (cap > 0) {
            if (GlowState.hand) {
               collectHand(mc, view, cap);
            }

            if (GlowState.drop) {
               collectDrops(mc, frustum, cap);
            }
         }
      }
   }

   private static void collectHand(Minecraft mc, Matrix4f view, int cap) {
      LocalPlayer player = mc.player;
      if (player != null && !mc.options.hideGui) {
         Camera cam = mc.gameRenderer.getMainCamera();
         Vec3 eye = cam.getPosition();
         Vector3f lv = cam.getLookVector();
         double lx = (double)lv.x;
         double ly = (double)lv.y;
         double lz = (double)lv.z;
         double rx = (double)view.m00();
         double ry = (double)view.m01();
         double rz = (double)view.m02();
         double ux = (double)view.m10();
         double uy = (double)view.m11();
         double uz = (double)view.m12();
         addAtAnchor(mc, player.getMainHandItem(), cap, eye, lx, ly, lz, rx, ry, rz, ux, uy, uz, 1.0F);
         addAtAnchor(mc, player.getOffhandItem(), cap, eye, lx, ly, lz, rx, ry, rz, ux, uy, uz, -1.0F);
      }
   }

   private static void addAtAnchor(
      Minecraft mc,
      ItemStack stack,
      int cap,
      Vec3 eye,
      double lx,
      double ly,
      double lz,
      double rx,
      double ry,
      double rz,
      double ux,
      double uy,
      double uz,
      float side
   ) {
      if (!stack.isEmpty()) {
         int color = GlowRules.colorFor(stack);
         if (color != -1) {
            float r = GlowRules.r(color);
            float g = GlowRules.g(color);
            float b = GlowRules.b(color);
            double fx = eye.x + lx * (double)GlowState.handForward + rx * (double)GlowState.handRight * (double)side - ux * (double)GlowState.handDown;
            double fy = eye.y + ly * (double)GlowState.handForward + ry * (double)GlowState.handRight * (double)side - uy * (double)GlowState.handDown;
            double fz = eye.z + lz * (double)GlowState.handForward + rz * (double)GlowState.handRight * (double)side - uz * (double)GlowState.handDown;
            float size = GlowState.handSize;
            EmitterBuffer.addCapped(fx, fy, fz, r, g, b, size, 0.95F, 0, cap);
            if (GlowState.lighting && GlowState.activeTier != Tier.LITE) {
               EmitterBuffer.addCapped(fx, fy, fz, r, g, b, size * 1.15F, 0.55F, 2, cap);
            }
         }
      }
   }

   private static void collectDrops(Minecraft mc, Frustum frustum, int cap) {
      ClientLevel level = mc.level;
      if (level != null) {
         LocalPlayer player = mc.player;
         frameGroup++;
         double px = player == null ? 0.0 : player.getX();
         double py = player == null ? 0.0 : player.getY();
         double pz = player == null ? 0.0 : player.getZ();
         double range = (double)GlowState.range;
         double range2 = range * range;
         int bracketBudget = Math.min(20, cap / 8);

         for (Entity e : level.entitiesForRendering()) {
            if (EmitterBuffer.count >= cap) {
               break;
            }

            if (e instanceof ItemEntity item) {
               double dx = item.getX() - px;
               double dy = item.getY() - py;
               double dz = item.getZ() - pz;
               double d2 = dx * dx + dy * dy + dz * dz;
               if (!(d2 > range2)) {
                  ItemStack stack = item.getItem();
                  int color = GlowRules.colorFor(stack);
                  if (color != -1) {
                     AABB box = item.getBoundingBox();
                     if ((frustum == null || frustum.isVisible(box)) && (GlowState.xray || player == null || visibleNow(item, player))) {
                        float r = GlowRules.r(color);
                        float g = GlowRules.g(color);
                        float b = GlowRules.b(color);
                        Vec3 ctr = box.getCenter();
                        float cx = (float)ctr.x;
                        float cy = (float)ctr.y;
                        float cz = (float)ctr.z;
                        float radius = Math.max(0.3F, (float)(Math.max(box.getXsize(), box.getZsize()) * 0.85));
                        float bob = 0.75F + 0.25F * Mth.sin((float)item.tickCount * 0.12F);
                        EmitterBuffer.add((double)cx, (double)cy, (double)cz, r, g, b, radius, 0.75F * bob, 0);
                        if (d2 < 144.0 && bracketBudget > 0) {
                           bracketBudget--;
                           float e2 = radius * 1.35F;
                           float cs = radius * 0.28F;
                           addCorner(cx - e2, cy + e2 * 0.6F, cz - e2, r, g, b, cs, cap);
                           addCorner(cx + e2, cy + e2 * 0.6F, cz - e2, r, g, b, cs, cap);
                           addCorner(cx - e2, cy + e2 * 0.6F, cz + e2, r, g, b, cs, cap);
                           addCorner(cx + e2, cy + e2 * 0.6F, cz + e2, r, g, b, cs, cap);
                        }

                        if (GlowState.lighting) {
                           EmitterBuffer.addCapped((double)cx, (double)cy, (double)cz, r, g, b, radius * 0.7F, 0.3F, 2, cap);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void addCorner(float x, float y, float z, float r, float g, float b, float s, int cap) {
      EmitterBuffer.addCapped((double)x, (double)y, (double)z, r, g, b, s, 0.9F, 0, cap);
   }

   private static boolean visibleNow(ItemEntity item, LocalPlayer player) {
      int id = item.getId();
      int slot = id & 0xFF;

      for (int probe = 0; probe < 8; probe++) {
         int idx = slot + probe & 0xFF;
         if (IDS[idx] == id && STAMP[idx] / 3 == frameGroup / 3) {
            return VIS[idx] != 0;
         }

         if (IDS[idx] == 0 || STAMP[idx] / 3 != frameGroup / 3) {
            boolean v = player.hasLineOfSight(item);
            IDS[idx] = id;
            VIS[idx] = (byte)(v ? 1 : 0);
            STAMP[idx] = frameGroup;
            return v;
         }
      }

      return true;
   }

   public static void resetCache() {
      Arrays.fill(IDS, 0);
      Arrays.fill(STAMP, 0);
      Arrays.fill(VIS, (byte)1);
      frameGroup = 0;
   }
}
