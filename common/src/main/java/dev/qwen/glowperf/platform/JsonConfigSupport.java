package dev.qwen.glowperf.platform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qwen.glowperf.GlowPerfMod;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonConfigSupport {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   private JsonConfigSupport() {
   }

   public static void bind(ConfigSpec spec, Path file, Runnable reload) {
      load(spec, file);
      reload.run();
      spec.bindSaver(() -> save(spec, file));
   }

   private static void load(ConfigSpec spec, Path file) {
      if (!Files.isRegularFile(file)) {
         return;
      }

      try (Reader reader = Files.newBufferedReader(file)) {
         JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

         for (ConfigValue<?> value : spec.values()) {
            JsonElement json = root.get(value.path());
            if (json != null) {
               set(value, read(value, json));
            }
         }
      } catch (Throwable error) {
         GlowPerfMod.LOG.warn("配置读取失败: {} ({})", file, error.toString());
      }
   }

   private static Object read(ConfigValue<?> value, JsonElement json) {
      Object defaultValue = value.defaultValue();
      if (defaultValue instanceof Boolean) {
         return json.getAsBoolean();
      } else if (defaultValue instanceof Integer) {
         return json.getAsInt();
      } else if (defaultValue instanceof Double) {
         return json.getAsDouble();
      } else if (defaultValue instanceof String) {
         return json.getAsString();
      } else if (defaultValue instanceof List) {
         List<String> result = new ArrayList<>();

         for (JsonElement element : json.getAsJsonArray()) {
            result.add(element.getAsString());
         }

         return result;
      } else {
         throw new IllegalArgumentException("不支持的配置类型: " + value.path());
      }
   }

   private static boolean save(ConfigSpec spec, Path file) {
      try {
         Files.createDirectories(file.getParent());
         JsonObject root = new JsonObject();

         for (ConfigValue<?> value : spec.values()) {
            root.add(value.path(), GSON.toJsonTree(value.get()));
         }

         try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(root, writer);
         }

         return true;
      } catch (Throwable error) {
         GlowPerfMod.LOG.warn("配置保存失败: {} ({})", file, error.toString());
         return false;
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void set(ConfigValue value, Object newValue) {
      value.set(newValue);
   }
}
