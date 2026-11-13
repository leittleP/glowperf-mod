package dev.qwen.glowperf.glow.core;

public final class EmitterBuffer {
   public static final int CAP = 512;
   public static final int BLOB = 0;
   public static final int BAR = 1;
   public static final int SHEEN = 2;
   public static final double[] X = new double[512];
   public static final double[] Y = new double[512];
   public static final double[] Z = new double[512];
   public static final float[] R = new float[512];
   public static final float[] G = new float[512];
   public static final float[] B = new float[512];
   public static final float[] SIZE = new float[512];
   public static final float[] ALPHA = new float[512];
   public static final int[] SHAPE = new int[512];
   public static int count;

   private EmitterBuffer() {
   }

   public static void clear() {
      count = 0;
   }

   public static int add(double x, double y, double z, float r, float g, float b, float size, float alpha, int shape) {
      int i = count;
      if (i >= 512) {
         return -1;
      } else {
         X[i] = x;
         Y[i] = y;
         Z[i] = z;
         R[i] = r;
         G[i] = g;
         B[i] = b;
         SIZE[i] = size;
         ALPHA[i] = alpha;
         SHAPE[i] = shape;
         count = i + 1;
         return i;
      }
   }

   public static int addCapped(double x, double y, double z, float r, float g, float b, float size, float alpha, int shape, int cap) {
      return count >= cap ? -1 : add(x, y, z, r, g, b, size, alpha, shape);
   }

   public static void compact() {
      if (count > 512) {
         count = 512;
      }
   }
}
