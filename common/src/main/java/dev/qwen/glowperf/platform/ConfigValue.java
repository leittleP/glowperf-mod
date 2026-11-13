package dev.qwen.glowperf.platform;

public class ConfigValue<T> {
   private final String path;
   private final T defaultValue;
   private final String comment;
   private final Number min;
   private final Number max;
   private volatile T value;

   ConfigValue(String path, T defaultValue, String comment, Number min, Number max) {
      this.path = path;
      this.defaultValue = defaultValue;
      this.comment = comment;
      this.min = min;
      this.max = max;
      this.value = defaultValue;
   }

   public T get() {
      return this.value;
   }

   public void set(T value) {
      this.value = value;
   }

   public String path() {
      return this.path;
   }

   public T defaultValue() {
      return this.defaultValue;
   }

   public String comment() {
      return this.comment;
   }

   public Number min() {
      return this.min;
   }

   public Number max() {
      return this.max;
   }
}
