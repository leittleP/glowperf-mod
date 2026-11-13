package dev.qwen.glowperf.forge;

import dev.qwen.glowperf.platform.LoaderPlatform;
import java.nio.file.Path;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

final class FmlLoaderPlatform implements LoaderPlatform.Provider {
   public boolean isModLoaded(String modId) {
      return ModList.get().isLoaded(modId);
   }

   public int loadedModCount() {
      return ModList.get().getMods().size();
   }

   public Path configDirectory() {
      return FMLPaths.CONFIGDIR.get();
   }
}
