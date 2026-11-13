package dev.qwen.glowperf.glow.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.qwen.glowperf.GlowPerfMod;
import dev.qwen.glowperf.glow.core.EmitterBuffer;
import dev.qwen.glowperf.glow.core.GlowRules;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.glow.render.gl.GLProgram;
import dev.qwen.glowperf.glow.render.gl.GlTargets;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class GlowRenderer {
   private static GLProgram emitter;
   private static GLProgram blur;
   private static GLProgram composite;
   private static final GlTargets TARGETS = new GlTargets();
   private static int vao;
   private static boolean initialized;
   private static boolean disabled;
   private static final Matrix4f VIEW = new Matrix4f();
   private static final Matrix4f MVP = new Matrix4f();
   private static final Vector4f PROBE = new Vector4f();
   private static float time;

   private GlowRenderer() {
   }

   public static boolean ready() {
      if (disabled) {
         return false;
      } else if (initialized) {
         return true;
      } else {
         try {
            emitter = GLProgram.create(
               "iglow_emitter",
               "#version 150\nout vec2 uv;\nvoid main(){\n  vec2 p = vec2(float(gl_VertexID & 1), float((gl_VertexID & 2) >> 1));\n  uv = p;\n  gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n}\n",
               "#version 150\nin vec2 uv;\nuniform vec4 Tint;      // rgb * intensity\nuniform int Shape;\nuniform float Softness; // 条状/高光的柔边比例\nuniform float Time;\nout vec4 fragColor;\nvoid main(){\n  vec2 c = uv * 2.0 - 1.0;\n  float a;\n  if (Shape == 0) {\n    float d = length(c);\n    a = exp(-d * d * 2.7) * smoothstep(1.0, 0.62, d);\n  } else if (Shape == 1) {\n    float m = max(abs(c.x), abs(c.y));\n    a = smoothstep(1.0, 1.0 - clamp(Softness, 0.05, 0.95), m);\n  } else {\n    float sweep = sin(uv.x * 6.28318 - Time);\n    float band = smoothstep(0.35, 1.0, sweep);\n    float vert = exp(-c.y * c.y * 3.2);\n    float edge = smoothstep(1.0, 0.45, abs(c.x));\n    a = band * vert * edge;\n  }\n  fragColor = vec4(Tint.rgb, 1.0) * clamp(a * Tint.a, 0.0, 4.0);\n}\n"
            );
            blur = GLProgram.create(
               "iglow_blur",
               "#version 150\nout vec2 uv;\nvoid main(){\n  vec2 p = vec2(float(gl_VertexID & 1), float((gl_VertexID & 2) >> 1));\n  uv = p;\n  gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n}\n",
               "#version 150\nin vec2 uv;\nuniform sampler2D Tex;\nuniform vec2 Texel;    // 1.0 / inSize\nuniform vec2 BlurDir;\nuniform float Radius;\nout vec4 fragColor;\nvoid main(){\n  vec2 o = BlurDir * Texel * Radius;\n  vec4 s = texture(Tex, uv) * 0.2270270270;\n  s += texture(Tex, uv + o * 1.0) * 0.1945945946;\n  s += texture(Tex, uv - o * 1.0) * 0.1945945946;\n  s += texture(Tex, uv + o * 2.0) * 0.1216216216;\n  s += texture(Tex, uv - o * 2.0) * 0.1216216216;\n  fragColor = s;\n}\n"
            );
            composite = GLProgram.create(
               "iglow_composite",
               "#version 150\nout vec2 uv;\nvoid main(){\n  vec2 p = vec2(float(gl_VertexID & 1), float((gl_VertexID & 2) >> 1));\n  uv = p;\n  gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n}\n",
               "#version 150\nin vec2 uv;\nuniform sampler2D Tex;\nuniform float Strength;\nuniform vec2 Fade;     // 边缘衰减（屏幕比例）\nout vec4 fragColor;\nvoid main(){\n  vec4 c = texture(Tex, uv);\n  float lum = max(max(c.r, c.g), c.b);\n  vec3 sat = mix(vec3(lum), c.rgb, 1.18);\n  vec2 q = abs(uv * 2.0 - 1.0);\n  float edge = (1.0 - smoothstep(1.0 - Fade.x, 1.0, q.x))\n             * (1.0 - smoothstep(1.0 - Fade.y, 1.0, q.y));\n  fragColor = vec4(sat * Strength * edge, 1.0);\n}\n"
            );
            if (emitter != null && blur != null && composite != null && emitter.ok() && blur.ok() && composite.ok()) {
               vao = GL30.glGenVertexArrays();
               initialized = true;
               GlowPerfMod.LOG.info("物品光影：自研辉光管线就绪");
               return true;
            } else {
               disabled = true;
               GlowState.glAvailable = false;
               GlowPerfMod.LOG.warn("物品光影：着色器不可用，已自动停用光效渲染（不影响游戏运行）");
               return false;
            }
         } catch (Throwable var1) {
            disabled = true;
            GlowState.glAvailable = false;
            GlowPerfMod.LOG.warn("物品光影：管线初始化失败，已自动停用: {}", var1.toString());
            return false;
         }
      }
   }

   public static void renderWorldPipeline(Minecraft mc, Matrix4f viewIn, Matrix4f projection, PoseStack levelPose, float partialTick) {
      if (ready() && EmitterBuffer.count != 0) {
         int screenW = mc.getWindow().getWidth();
         int screenH = mc.getWindow().getHeight();
         Tier tier = GlowState.activeTier;
         int ds = tier.effectiveDownscale(GlowState.downscale);
         if (TARGETS.ensure(screenW, screenH, ds)) {
            int w = TARGETS.width();
            int h = TARGETS.height();
            time += 0.012F + partialTick * 0.004F;
            Camera cam = mc.gameRenderer.getMainCamera();
            double camX = cam.getPosition().x;
            double camY = cam.getPosition().y;
            double camZ = cam.getPosition().z;
            VIEW.set(viewIn);
            MVP.set(projection);
            MVP.mul(VIEW);
            MVP.mul(levelPose.last().pose());
            long t0 = System.nanoTime();
            TARGETS.mask.bindWrite(false);
            GL30.glBindVertexArray(vao);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(1, 1);
            RenderSystem.clear(16384, false);
            emitter.use();
            emitter.f1("Time", time);
            emitter.f1("Softness", 0.55F);
            float range = (float)GlowState.range;
            int drawn = 0;

            for (int i = 0; i < EmitterBuffer.count; i++) {
               double dx = EmitterBuffer.X[i] - camX;
               double dy = EmitterBuffer.Y[i] - camY;
               double dz = EmitterBuffer.Z[i] - camZ;
               if (!(dx * dx + dy * dy + dz * dz > (double)(range * range)) && projectToTarget(i, w, h, camX, camY, camZ)) {
                  float cx = PROBE.x;
                  float cy = PROBE.y;
                  float r = PROBE.z;
                  int size = (int)(r * 2.0F);
                  if (size >= 2) {
                     int x0 = clampi((int)(cx - r), 0, w);
                     int y0 = clampi((int)(cy - r), 0, h);
                     int vw = clampi(x0 + size, x0, w) - x0;
                     int vh = clampi(y0 + size, y0, h) - y0;
                     if (vw > 0 && vh > 0) {
                        float a = EmitterBuffer.ALPHA[i] * GlowState.strength * tier.strengthScale;
                        if (!(a <= 0.004F)) {
                           RenderSystem.viewport(x0, y0, vw, vh);
                           emitter.f4("Tint", EmitterBuffer.R[i], EmitterBuffer.G[i], EmitterBuffer.B[i], a);
                           emitter.i1("Shape", EmitterBuffer.SHAPE[i]);
                           GL11.glDrawArrays(4, 0, 3);
                           drawn++;
                        }
                     }
                  }
               }
            }

            GlowState.lastEmitterCount = drawn;
            int iterations = tier.effectiveBlurIterations();
            GlowState.lastBlurPasses = iterations * 2;
            if (iterations > 0) {
               blur.use();
               float blurRadius = GlowState.radius * 0.55F + 1.0F;
               int maskTex = TARGETS.mask.getColorTextureId();
               int swapTex = TARGETS.swap.getColorTextureId();

               for (int it = 0; it < iterations; it++) {
                  TARGETS.swap.bindWrite(false);
                  RenderSystem.viewport(0, 0, w, h);
                  RenderSystem.clear(16384, false);
                  bindTex(maskTex);
                  blur.f1("Radius", blurRadius);
                  blur.f2("Texel", 1.0F / (float)w, 1.0F / (float)h);
                  blur.f2("BlurDir", 1.0F, 0.0F);
                  blur.i1("Tex", 0);
                  GL11.glDrawArrays(4, 0, 3);
                  TARGETS.mask.bindWrite(false);
                  RenderSystem.viewport(0, 0, w, h);
                  RenderSystem.clear(16384, false);
                  bindTex(swapTex);
                  blur.f2("BlurDir", 0.0F, 1.0F);
                  GL11.glDrawArrays(4, 0, 3);
               }
            }

            mc.getMainRenderTarget().bindWrite(true);
            composite.use();
            bindTex(TARGETS.mask.getColorTextureId());
            composite.i1("Tex", 0);
            composite.f1("Strength", GlowState.activeTier.strengthScale);
            composite.f2("Fade", 0.03F, 0.03F);
            GL11.glDrawArrays(4, 0, 3);
            GL30.glBindVertexArray(0);
            restoreState(mc);
            GlowState.lastPipelineMs = (float)(System.nanoTime() - t0) / 1000000.0F;
         }
      }
   }

   public static void drawScreenHalo(Minecraft mc, float guiCx, float guiCy, float radiusGui, int rgb, float alpha, int shape) {
      if (ready() && !(alpha <= 0.004F) && !(radiusGui <= 0.5F)) {
         double scale = mc.getWindow().getGuiScale();
         int fw = mc.getWindow().getWidth();
         int fh = mc.getWindow().getHeight();
         float cx = (float)((double)guiCx * scale);
         float cy = (float)((double)guiCy * scale);
         float r = (float)((double)radiusGui * scale);
         int size = (int)(r * 2.0F);
         if (size >= 2 && !(cx + r < 0.0F) && !(cx - r > (float)fw) && !(cy + r < 0.0F) && !(cy - r > (float)fh)) {
            int x0 = clampi((int)(cx - r), 0, fw);
            int y0 = clampi(fh - (int)(cy + r), 0, fh);
            int vw = clampi(x0 + size, x0, fw) - x0;
            int vh = clampi(y0 + size, y0, fh) - y0;
            if (vw > 0 && vh > 0) {
               time += 0.004F;
               GL30.glBindVertexArray(vao);
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.disableCull();
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(1, 1);
               RenderSystem.viewport(x0, y0, vw, vh);
               emitter.use();
               emitter.f4("Tint", GlowRules.channel(rgb, 16), GlowRules.channel(rgb, 8), GlowRules.channel(rgb, 0), alpha * GlowState.strength);
               emitter.i1("Shape", shape);
               emitter.f1("Softness", shape == 1 ? 0.42F : 0.6F);
               emitter.f1("Time", time);
               GL11.glDrawArrays(4, 0, 3);
               GL30.glBindVertexArray(0);
               restoreState(mc);
            }
         }
      }
   }

   private static void bindTex(int texId) {
      RenderSystem.activeTexture(33984);
      RenderSystem.bindTexture(texId);
   }

   private static void restoreState(Minecraft mc) {
      try {
         RenderSystem.activeTexture(33984);
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(770, 771);
         RenderSystem.colorMask(true, true, true, true);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
      } catch (Throwable var2) {
      }
   }

   private static boolean projectToTarget(int i, int w, int h, double camX, double camY, double camZ) {
      MVP.transform((float)EmitterBuffer.X[i], (float)EmitterBuffer.Y[i], (float)EmitterBuffer.Z[i], 1.0F, PROBE);
      if (PROBE.w <= 1.0E-4F) {
         return false;
      } else {
         float ndcx = PROBE.x / PROBE.w;
         float ndcy = PROBE.y / PROBE.w;
         float ndcz = PROBE.z / PROBE.w;
         if (!(ndcx < -1.4F) && !(ndcx > 1.4F) && !(ndcy < -1.4F) && !(ndcy > 1.4F) && !(ndcz < -1.2F) && !(ndcz > 1.6F)) {
            float s = EmitterBuffer.SIZE[i];
            MVP.transform(
               (float)(EmitterBuffer.X[i] + (double)(VIEW.m00() * s)),
               (float)(EmitterBuffer.Y[i] + (double)(VIEW.m01() * s)),
               (float)(EmitterBuffer.Z[i] + (double)(VIEW.m02() * s)),
               1.0F,
               PROBE
            );
            if (PROBE.w <= 1.0E-4F) {
               return false;
            } else {
               float baseX = (ndcx * 0.5F + 0.5F) * (float)w;
               float offX = (PROBE.x / PROBE.w * 0.5F + 0.5F) * (float)w;
               float radius = Math.abs(offX - baseX);
               if (!(radius < 1.0F) && !(radius > (float)w)) {
                  PROBE.set(baseX, (ndcy * 0.5F + 0.5F) * (float)h, Math.min(radius, (float)w * 0.85F), 1.0F);
                  return true;
               } else {
                  return false;
               }
            }
         } else {
            return false;
         }
      }
   }

   private static int clampi(int v, int lo, int hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   public static void dispose() {
      if (emitter != null) {
         emitter.delete();
         emitter = null;
      }

      if (blur != null) {
         blur.delete();
         blur = null;
      }

      if (composite != null) {
         composite.delete();
         composite = null;
      }

      TARGETS.dispose();
      if (vao != 0) {
         GL30.glDeleteVertexArrays(vao);
         vao = 0;
      }

      initialized = false;
      disabled = false;
   }

   public static int targetWidth() {
      return TARGETS.width();
   }

   public static int targetHeight() {
      return TARGETS.height();
   }

   public static boolean isDisabled() {
      return disabled;
   }
}
