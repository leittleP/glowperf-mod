package dev.qwen.glowperf.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class ConfigSpec {
   private final List<ConfigValue<?>> values;
   private volatile BooleanSupplier saver = () -> false;

   private ConfigSpec(List<ConfigValue<?>> values) {
      this.values = Collections.unmodifiableList(new ArrayList<>(values));
   }

   public List<ConfigValue<?>> values() {
      return this.values;
   }

   public void bindSaver(BooleanSupplier saver) {
      this.saver = saver;
   }

   public boolean save() {
      try {
         return this.saver.getAsBoolean();
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static final class Builder {
      private final List<ConfigValue<?>> values = new ArrayList<>();
      private String comment = "";

      public Builder comment(String comment) {
         this.comment = comment;
         return this;
      }

      public BooleanValue define(String path, boolean defaultValue) {
         BooleanValue value = new BooleanValue(path, defaultValue, this.comment);
         this.values.add(value);
         return value;
      }

      public <T> ConfigValue<T> define(String path, T defaultValue) {
         ConfigValue<T> value = new ConfigValue<>(path, defaultValue, this.comment, null, null);
         this.values.add(value);
         return value;
      }

      public IntValue defineInRange(String path, int defaultValue, int min, int max) {
         IntValue value = new IntValue(path, defaultValue, this.comment, min, max);
         this.values.add(value);
         return value;
      }

      public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
         DoubleValue value = new DoubleValue(path, defaultValue, this.comment, min, max);
         this.values.add(value);
         return value;
      }

      public ConfigSpec build() {
         return new ConfigSpec(this.values);
      }
   }

   public static final class BooleanValue extends ConfigValue<Boolean> {
      private BooleanValue(String path, boolean defaultValue, String comment) {
         super(path, defaultValue, comment, null, null);
      }
   }

   public static final class IntValue extends ConfigValue<Integer> {
      private IntValue(String path, int defaultValue, String comment, int min, int max) {
         super(path, defaultValue, comment, min, max);
      }
   }

   public static final class DoubleValue extends ConfigValue<Double> {
      private DoubleValue(String path, double defaultValue, String comment, double min, double max) {
         super(path, defaultValue, comment, min, max);
      }
   }
}
