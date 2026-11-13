package dev.qwen.glowperf.glow.render;

public final class Glsl {
   public static final String VS_FULL = "#version 150\nout vec2 uv;\nvoid main(){\n  vec2 p = vec2(float(gl_VertexID & 1), float((gl_VertexID & 2) >> 1));\n  uv = p;\n  gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n}\n";
   public static final String FS_EMITTER = "#version 150\nin vec2 uv;\nuniform vec4 Tint;      // rgb * intensity\nuniform int Shape;\nuniform float Softness; // 条状/高光的柔边比例\nuniform float Time;\nout vec4 fragColor;\nvoid main(){\n  vec2 c = uv * 2.0 - 1.0;\n  float a;\n  if (Shape == 0) {\n    float d = length(c);\n    a = exp(-d * d * 2.7) * smoothstep(1.0, 0.62, d);\n  } else if (Shape == 1) {\n    float m = max(abs(c.x), abs(c.y));\n    a = smoothstep(1.0, 1.0 - clamp(Softness, 0.05, 0.95), m);\n  } else {\n    float sweep = sin(uv.x * 6.28318 - Time);\n    float band = smoothstep(0.35, 1.0, sweep);\n    float vert = exp(-c.y * c.y * 3.2);\n    float edge = smoothstep(1.0, 0.45, abs(c.x));\n    a = band * vert * edge;\n  }\n  fragColor = vec4(Tint.rgb, 1.0) * clamp(a * Tint.a, 0.0, 4.0);\n}\n";
   public static final String FS_BLUR = "#version 150\nin vec2 uv;\nuniform sampler2D Tex;\nuniform vec2 Texel;    // 1.0 / inSize\nuniform vec2 BlurDir;\nuniform float Radius;\nout vec4 fragColor;\nvoid main(){\n  vec2 o = BlurDir * Texel * Radius;\n  vec4 s = texture(Tex, uv) * 0.2270270270;\n  s += texture(Tex, uv + o * 1.0) * 0.1945945946;\n  s += texture(Tex, uv - o * 1.0) * 0.1945945946;\n  s += texture(Tex, uv + o * 2.0) * 0.1216216216;\n  s += texture(Tex, uv - o * 2.0) * 0.1216216216;\n  fragColor = s;\n}\n";
   public static final String FS_COMPOSITE = "#version 150\nin vec2 uv;\nuniform sampler2D Tex;\nuniform float Strength;\nuniform vec2 Fade;     // 边缘衰减（屏幕比例）\nout vec4 fragColor;\nvoid main(){\n  vec4 c = texture(Tex, uv);\n  float lum = max(max(c.r, c.g), c.b);\n  vec3 sat = mix(vec3(lum), c.rgb, 1.18);\n  vec2 q = abs(uv * 2.0 - 1.0);\n  float edge = (1.0 - smoothstep(1.0 - Fade.x, 1.0, q.x))\n             * (1.0 - smoothstep(1.0 - Fade.y, 1.0, q.y));\n  fragColor = vec4(sat * Strength * edge, 1.0);\n}\n";

   private Glsl() {
   }
}
