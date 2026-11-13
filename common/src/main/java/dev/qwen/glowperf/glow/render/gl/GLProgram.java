package dev.qwen.glowperf.glow.render.gl;

import dev.qwen.glowperf.GlowPerfMod;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL20;

public final class GLProgram implements AutoCloseable {
   private final String name;
   private int program = -1;
   private final Map<String, Integer> uniforms = new HashMap<>();

   private GLProgram(String name) {
      this.name = name;
   }

   public static GLProgram create(String name, String vertexSrc, String fragmentSrc) {
      GLProgram p = new GLProgram(name);
      int vs = 0;
      int fs = 0;
      int prog = 0;

      Object t;
      try {
         vs = compile(35633, vertexSrc);
         fs = compile(35632, fragmentSrc);
         if (vs == 0 || fs == 0) {
            p.delete();
            return null;
         }

         prog = GL20.glCreateProgram();
         if (prog == 0) {
            p.delete();
            return null;
         }

         GL20.glAttachShader(prog, vs);
         GL20.glAttachShader(prog, fs);
         GL20.glLinkProgram(prog);
         if (GL20.glGetProgrami(prog, 35714) != 0) {
            p.program = prog;
            return p;
         }

         GlowPerfMod.LOG.warn("着色器 {} 链接失败: {}", name, GL20.glGetProgramInfoLog(prog));
         p.delete();
         t = null;
      } catch (Throwable var12) {
         GlowPerfMod.LOG.warn("着色器 {} 初始化异常: {}", name, var12.toString());
         if (prog != 0) {
            GL20.glDeleteProgram(prog);
         }

         p.program = -1;
         return null;
      } finally {
         if (vs != 0) {
            GL20.glDeleteShader(vs);
         }

         if (fs != 0) {
            GL20.glDeleteShader(fs);
         }
      }

      return (GLProgram)t;
   }

   private static int compile(int type, String src) {
      int shader = GL20.glCreateShader(type);
      if (shader == 0) {
         return 0;
      } else {
         GL20.glShaderSource(shader, src);
         GL20.glCompileShader(shader);
         if (GL20.glGetShaderi(shader, 35713) == 0) {
            GlowPerfMod.LOG.warn("着色器编译失败({}): {}", type == 35633 ? "vertex" : "fragment", GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
         } else {
            return shader;
         }
      }
   }

   public void use() {
      if (this.program > 0) {
         GL20.glUseProgram(this.program);
      }
   }

   public boolean ok() {
      return this.program > 0;
   }

   public int u(String name) {
      Integer cached = this.uniforms.get(name);
      if (cached != null) {
         return cached;
      } else {
         int loc = this.program > 0 ? GL20.glGetUniformLocation(this.program, name) : -1;
         this.uniforms.put(name, loc);
         return loc;
      }
   }

   public void f1(String n, float a) {
      int l = this.u(n);
      if (l >= 0) {
         GL20.glUniform1f(l, a);
      }
   }

   public void f2(String n, float a, float b) {
      int l = this.u(n);
      if (l >= 0) {
         GL20.glUniform2f(l, a, b);
      }
   }

   public void f4(String n, float a, float b, float c, float d) {
      int l = this.u(n);
      if (l >= 0) {
         GL20.glUniform4f(l, a, b, c, d);
      }
   }

   public void i1(String n, int a) {
      int l = this.u(n);
      if (l >= 0) {
         GL20.glUniform1i(l, a);
      }
   }

   public void delete() {
      if (this.program > 0) {
         GL20.glUseProgram(0);
         GL20.glDeleteProgram(this.program);
      }

      this.program = -1;
      this.uniforms.clear();
   }

   @Override
   public void close() {
      this.delete();
   }
}
