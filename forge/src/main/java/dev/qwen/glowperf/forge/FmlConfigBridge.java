package dev.qwen.glowperf.forge;

import dev.qwen.glowperf.GlowPerfMod;
import dev.qwen.glowperf.platform.ConfigSpec;
import dev.qwen.glowperf.platform.ConfigValue;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

final class FmlConfigBridge {
   private final ModConfig.Type type;
   private final ConfigSpec commonSpec;
   private final ForgeConfigSpec forgeSpec;
   private final List<Binding> bindings = new ArrayList<>();
   private final Runnable reload;
   private volatile ModConfig handle;

   FmlConfigBridge(ModConfig.Type type, ConfigSpec commonSpec, Runnable reload) {
      this.type = type;
      this.commonSpec = commonSpec;
      this.reload = reload;
      ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

      for (ConfigValue<?> value : commonSpec.values()) {
         this.bindings.add(bind(builder, value));
      }

      this.forgeSpec = builder.build();
      ModLoadingContext.get().registerConfig(type, this.forgeSpec);
      commonSpec.bindSaver(this::save);
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onLoading);
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onReloading);
   }

   private void onLoading(ModConfigEvent.Loading event) {
      load(event.getConfig());
   }

   private void onReloading(ModConfigEvent.Reloading event) {
      load(event.getConfig());
   }

   private void load(ModConfig config) {
      if ("glowperf".equals(config.getModId()) && config.getType() == this.type && config.getSpec() == this.forgeSpec) {
         this.handle = config;

         for (Binding binding : this.bindings) {
            binding.load();
         }

         this.reload.run();
      }
   }

   private boolean save() {
      ModConfig config = this.handle;
      if (config == null) {
         return false;
      }

      try {
         for (Binding binding : this.bindings) {
            binding.push();
         }

         config.save();
         return true;
      } catch (Throwable error) {
         GlowPerfMod.LOG.warn("配置保存失败: {}", error.toString());
         return false;
      }
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static Binding bind(ForgeConfigSpec.Builder builder, ConfigValue<?> value) {
      builder.comment(value.comment());
      Object defaultValue = value.defaultValue();
      ForgeConfigSpec.ConfigValue forgeValue;
      if (defaultValue instanceof Boolean) {
         forgeValue = builder.define(value.path(), (Boolean)defaultValue);
      } else if (defaultValue instanceof Integer) {
         forgeValue = builder.defineInRange(value.path(), (Integer)defaultValue, value.min().intValue(), value.max().intValue());
      } else if (defaultValue instanceof Double) {
         forgeValue = builder.defineInRange(value.path(), (Double)defaultValue, value.min().doubleValue(), value.max().doubleValue());
      } else if (defaultValue instanceof String) {
         forgeValue = builder.define(value.path(), (String)defaultValue);
      } else if (defaultValue instanceof List) {
         forgeValue = builder.define(value.path(), (List<String>)defaultValue);
      } else {
         throw new IllegalArgumentException("不支持的配置类型: " + value.path());
      }

      return new Binding((ConfigValue<Object>)value, (ForgeConfigSpec.ConfigValue<Object>)forgeValue);
   }

   private static final class Binding {
      private final ConfigValue<Object> common;
      private final ForgeConfigSpec.ConfigValue<Object> forge;

      private Binding(ConfigValue<Object> common, ForgeConfigSpec.ConfigValue<Object> forge) {
         this.common = common;
         this.forge = forge;
      }

      private void load() {
         this.common.set(this.forge.get());
      }

      private void push() {
         this.forge.set(this.common.get());
      }
   }
}
