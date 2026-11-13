package dev.qwen.glowperf.glow.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.registries.BuiltInRegistries;

public final class GlowRules {
   private static final Map<Item, Integer> CACHE = new HashMap<>();
   private static final Set<String> WHITE = new HashSet<>();
   private static final Set<String> BLACK = new HashSet<>();
   private static List<String> whiteSrc;
   private static List<String> blackSrc;
   public static final int NONE = -1;

   private GlowRules() {
   }

   public static void invalidate() {
      CACHE.clear();
      whiteSrc = null;
   }

   private static void syncLists() {
      List<String> w = GlowState.whitelist();
      List<String> b = GlowState.blacklist();
      if (w != whiteSrc) {
         WHITE.clear();

         for (String s : w) {
            if (s != null) {
               WHITE.add(s.toLowerCase(Locale.ROOT));
            }
         }

         whiteSrc = w;
      }

      if (b != blackSrc) {
         BLACK.clear();

         for (String sx : b) {
            if (sx != null) {
               BLACK.add(sx.toLowerCase(Locale.ROOT));
            }
         }

         blackSrc = b;
      }
   }

   public static int colorFor(ItemStack stack) {
      if (stack.isEmpty()) {
         return -1;
      } else {
         Item item = stack.getItem();
         Integer cached = CACHE.get(item);
         if (cached != null) {
            return cached;
         } else {
            int result = compute(stack, item);
            CACHE.put(item, result);
            return result;
         }
      }
   }

   private static int compute(ItemStack stack, Item item) {
      syncLists();
      String id = keyOf(item);
      if (id == null) {
         return -1;
      } else if (!BLACK.contains(id) && !BLACK.contains("*")) {
         boolean whitelisted = WHITE.contains(id);
         switch (GlowState.ruleMode) {
            case WHITELIST:
               if (!whitelisted) {
                  return -1;
               }
            case ALL:
               break;
            case ENCHANTED:
               if (!whitelisted && !stack.hasFoil()) {
                  return -1;
               }
               break;
            case RARITY:
            default:
               if (!whitelisted && stack.getRarity() == Rarity.COMMON) {
                  return -1;
               }
         }

         if (!GlowState.perItemColor) {
            return GlowState.accent;
         } else {
            float hue = (float)((id.hashCode() & 134217727) % 3600) / 3600.0F;
            int own = hsv(hue, 0.5F, 1.0F);
            return blend(own, GlowState.accent, 0.45F);
         }
      } else {
         return -1;
      }
   }

   private static String keyOf(Item item) {
      ResourceLocation rl = BuiltInRegistries.ITEM.getKey(item);
      return rl == null ? null : rl.toString().toLowerCase(Locale.ROOT);
   }

   public static boolean toggleWhitelist(String itemId, boolean add) {
      String key = itemId.toLowerCase(Locale.ROOT);
      List<String> list = GlowState.whitelist();
      List<String> next = new ArrayList<>(list);
      boolean present = next.contains(key);
      if (add && present) {
         return false;
      } else if (!add && !present) {
         return false;
      } else {
         if (add) {
            next.add(key);
         } else {
            next.remove(key);
         }

         GlowState.setWhitelist(next);
         return true;
      }
   }

   public static int blend(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int r = (int)((float)ar + (float)(br - ar) * t);
      int g = (int)((float)ag + (float)(bg - ag) * t);
      int bl = (int)((float)ab + (float)(bb - ab) * t);
      return r << 16 | g << 8 | bl;
   }

   public static int hsv(float h, float s, float v) {
      int i = (int)Math.floor((double)(h * 6.0F));
      float f = h * 6.0F - (float)i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - f * s);
      float t = v * (1.0F - (1.0F - f) * s);
      float r;
      float g;
      float b;
      switch (i % 6) {
         case 0:
            r = v;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = v;
            b = p;
            break;
         case 2:
            r = p;
            g = v;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = v;
            break;
         case 4:
            r = t;
            g = p;
            b = v;
            break;
         default:
            r = v;
            g = p;
            b = q;
      }

      return (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
   }

   public static float norm(int channel) {
      return (float)(channel & 0xFF) / 255.0F;
   }

   public static float channel(int rgb, int shift) {
      return (float)(rgb >> shift & 0xFF) / 255.0F;
   }

   public static float r(int rgb) {
      return channel(rgb, 16);
   }

   public static float g(int rgb) {
      return channel(rgb, 8);
   }

   public static float b(int rgb) {
      return channel(rgb, 0);
   }

   public static int cacheSize() {
      return CACHE.size();
   }
}
