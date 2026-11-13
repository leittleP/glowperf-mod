package dev.qwen.glowperf.platform;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class LoaderPlatform {
   private static volatile Provider provider = new Provider() {
      public boolean isModLoaded(String modId) {
         return false;
      }

      public int loadedModCount() {
         return 0;
      }

      public Path configDirectory() {
         return Paths.get("config");
      }
   };

   private LoaderPlatform() {
   }

   public static void initialize(Provider implementation) {
      provider = Objects.requireNonNull(implementation, "implementation");
   }

   public static boolean isModLoaded(String modId) {
      return provider.isModLoaded(modId);
   }

   public static int loadedModCount() {
      return provider.loadedModCount();
   }

   public static Path configDirectory() {
      return provider.configDirectory();
   }

   public interface Provider {
      boolean isModLoaded(String modId);

      int loadedModCount();

      Path configDirectory();
   }
}
