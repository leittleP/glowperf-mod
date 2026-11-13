package dev.qwen.glowperf.glow.client.ui;

import dev.qwen.glowperf.glow.client.ItemGlowClient;
import dev.qwen.glowperf.glow.client.hud.FpsHud;
import dev.qwen.glowperf.glow.core.GlowRules;
import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.glow.render.FpsSampler;
import dev.qwen.glowperf.glow.render.Governor;
import dev.qwen.glowperf.glow.render.Tier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GlowConfigScreen extends Screen {
   private static final int TOGGLE = 0;
   private static final int SLIDER = 1;
   private static final int CYCLE = 2;
   private final Screen parent;
   private final List<GlowConfigScreen.Row> rows = new ArrayList<>();
   private int panelX;
   private int panelY;
   private int panelW;
   private int panelH;
   private int dragging = -1;
   private int toastTimer;
   private String toast = "";
   private static final int B_SAVE = -2;
   private static final int B_RESET = -3;
   private static final int B_CLOSE = -4;
   private final GlowConfigScreen.Regions buttonRegions = new GlowConfigScreen.Regions();

   public GlowConfigScreen(Screen parent) {
      super(Component.translatable("screen.itemglow.settings"));
      this.parent = parent;
   }

   protected void init() {
      this.rows.clear();
      this.rows.add(new GlowConfigScreen.Row(0, 0, "光影总开关"));
      this.rows.add(new GlowConfigScreen.Row(1, 0, "手持 / 装备光晕"));
      this.rows.add(new GlowConfigScreen.Row(2, 0, "掉落物高亮描边"));
      this.rows.add(new GlowConfigScreen.Row(3, 0, "动态受光高光"));
      this.rows.add(new GlowConfigScreen.Row(4, 0, "图标发光（背包/箱子）"));
      this.rows.add(new GlowConfigScreen.Row(5, 0, "掉落物穿墙可见"));
      this.rows.add(new GlowConfigScreen.Row(6, 0, "帧率面板"));
      this.rows.add(new GlowConfigScreen.Row(7, 1, "光晕强度", 0.0F, 2.0F));
      this.rows.add(new GlowConfigScreen.Row(8, 1, "溢出半径", 1.0F, 8.0F));
      this.rows.add(new GlowConfigScreen.Row(9, 1, "高亮距离(格)", 4.0F, 96.0F));
      this.rows.add(new GlowConfigScreen.Row(10, 1, "发光物数量上限", 8.0F, 512.0F));
      this.rows.add(new GlowConfigScreen.Row(11, 1, "遮罩降采样(1/N)", 1.0F, 8.0F));
      this.rows.add(new GlowConfigScreen.Row(12, 1, "自动挡帧预算(ms)", 5.0F, 100.0F));
      this.rows.add(new GlowConfigScreen.Row(13, 2, "性能档位"));
      this.rows.add(new GlowConfigScreen.Row(14, 2, "发光规则"));
      this.rows.add(new GlowConfigScreen.Row(15, 2, "强调色"));
      this.panelW = Math.min(260, this.width - 40);
      this.panelH = 34 + this.rows.size() * 24 + 40;
      this.panelX = (this.width - this.panelW) / 2;
      this.panelY = Math.max(16, (this.height - this.panelH) / 2);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fill(0, 0, this.width, this.height, -1476130286);
      VrTheme.panel(g, this.panelX, this.panelY, this.panelW, this.panelH);
      VrTheme.scanline(g, this.panelX, this.panelY, this.panelW, this.panelH);
      int titleY = this.panelY + 10;
      g.drawCenteredString(this.font, VrTheme.title(), this.width / 2, titleY, -13049345);
      g.drawString(this.font, FpsHud.statsLine(), this.panelX + 10, titleY + 12, FpsSampler.fps() >= 60.0F ? -11010128 : -15253);
      int y = this.panelY + 42;

      for (GlowConfigScreen.Row row : this.rows) {
         row.y = y;
         int labelColor = this.hovered(row, mouseX, mouseY) ? -1 : -2689537;
         g.drawString(this.font, row.label, this.panelX + 10, y + 2, labelColor);
         if (row.kind == 0) {
            VrTheme.toggle(g, this.panelX + this.panelW - 46, y, this.getBool(row.id), "");
         } else if (row.kind == 1) {
            float v = this.getFloat(row.id);
            float r01 = (v - row.min) / Math.max(1.0E-4F, row.max - row.min);
            int sx = this.panelX + this.panelW - 110;
            row.hitX = sx;
            row.hitW = 96;
            VrTheme.slider(g, sx, y + 1, 96, r01, "", this.valueText(row, v));
         } else {
            row.hitX = this.panelX + this.panelW - 118;
            row.hitW = 108;
            g.drawString(this.font, this.cycleText(row.id), row.hitX, y + 2, this.hovered(row, mouseX, mouseY) ? -45149 : -13049345);
         }

         y += 24;
      }

      int by = this.panelY + this.panelH - 24;
      this.button(g, "保存", -2, this.panelX + 10, by, mouseX, mouseY);
      this.button(g, "默认", -3, this.panelX + 66, by, mouseX, mouseY);
      this.button(g, "关闭", -4, this.panelX + this.panelW - 60, by, mouseX, mouseY);
      if (this.toastTimer > 0) {
         this.toastTimer--;
         int w = this.font.width(this.toast) + 14;
         VrTheme.panel(g, this.width / 2 - w / 2, this.panelY + this.panelH + 6, w, 18, -871951585);
         g.drawCenteredString(this.font, this.toast, this.width / 2, this.panelY + this.panelH + 11, -11010128);
      }
   }

   private void button(GuiGraphics g, String text, int id, int x, int y, int mx, int my) {
      int w = this.font.width(text) + 16;
      boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + 14;
      g.fill(x, y, x + w, y + 14, hov ? -2009538049 : 1073741824);
      g.fill(x, y, x + w, y + 1, hov ? -13049345 : 859365887);
      g.fill(x, y + 13, x + w, y + 14, hov ? -13049345 : 859365887);
      g.drawString(this.font, text, x + 8, y + 3, hov ? -1 : -2689537);
      GlowConfigScreen.ButtonRegion reg = new GlowConfigScreen.ButtonRegion();
      reg.x = x;
      reg.w = w;
      reg.y = y;
      reg.id = id;
      this.buttonRegions.set(this.absIndex(id), reg);
   }

   private int absIndex(int id) {
      return -id - 2;
   }

   public boolean mouseClicked(double mx, double my, int button) {
      for (int i = 0; i < 8; i++) {
         GlowConfigScreen.ButtonRegion r = this.buttonRegions.get(i);
         if (r != null && mx >= (double)r.x && mx <= (double)(r.x + r.w) && my >= (double)r.y && my <= (double)(r.y + 14)) {
            this.onButton(r.id);
            return true;
         }
      }

      for (GlowConfigScreen.Row row : this.rows) {
         if (this.contains(row, mx, my)) {
            if (row.kind == 0) {
               this.setBool(row.id, !this.getBool(row.id));
               this.say(row.label + "：" + (this.getBool(row.id) ? "开" : "关"));
               return true;
            }

            if (row.kind == 2) {
               this.cycle(row.id);
               this.say(row.label + " → " + this.cycleText(row.id));
               return true;
            }

            this.dragging = row.id;
            this.applySlider(row, mx);
            return true;
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (this.dragging < 0) {
         return super.mouseDragged(mx, my, button, dx, dy);
      } else {
         for (GlowConfigScreen.Row row : this.rows) {
            if (row.id == this.dragging) {
               this.applySlider(row, mx);
               break;
            }
         }

         return true;
      }
   }

   public boolean mouseReleased(double mx, double my, int button) {
      this.dragging = -1;
      return super.mouseReleased(mx, my, button);
   }

   private void applySlider(GlowConfigScreen.Row row, double mx) {
      float r01 = (float)((mx - (double)row.hitX) / (double)Math.max(1, row.hitW));
      r01 = Math.max(0.0F, Math.min(1.0F, r01));
      this.setFloat(row.id, row.min + r01 * (row.max - row.min));
   }

   private void onButton(int id) {
      if (id == -2) {
         boolean ok = ItemGlowClient.saveConfig();
         this.say(ok ? "已保存配置" : "已记录（配置句柄未就绪）");
      } else if (id == -3) {
         GlowState.resetDefaults();
         Governor.reset();
         FpsSampler.reset();
         GlowRules.invalidate();
         this.say("已恢复默认参数");
      } else if (id == -4) {
         this.backToGame();
      }
   }

   private void say(String s) {
      this.toast = s;
      this.toastTimer = 60;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && keyCode != 257) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         this.onClose();
         return true;
      }
   }

   private void backToGame() {
      ItemGlowClient.saveConfig();
      this.minecraft.setScreen(this.parent);
   }

   public void removed() {
      super.removed();
      ItemGlowClient.saveConfig();
   }

   private boolean getBool(int id) {
      switch (id) {
         case 0:
            return GlowState.enabled;
         case 1:
            return GlowState.hand;
         case 2:
            return GlowState.drop;
         case 3:
            return GlowState.lighting;
         case 4:
            return GlowState.gui;
         case 5:
            return GlowState.xray;
         case 6:
            return GlowState.fpsHud;
         default:
            return false;
      }
   }

   private void setBool(int id, boolean v) {
      switch (id) {
         case 0:
            GlowState.enabled = v;
            GlowState.applyTier();
            break;
         case 1:
            GlowState.hand = v;
            break;
         case 2:
            GlowState.drop = v;
            break;
         case 3:
            GlowState.lighting = v;
            break;
         case 4:
            GlowState.gui = v;
            break;
         case 5:
            GlowState.xray = v;
            break;
         case 6:
            GlowState.fpsHud = v;
      }

      ItemGlowClient.saveConfig();
   }

   private float getFloat(int id) {
      switch (id) {
         case 7:
            return GlowState.strength;
         case 8:
            return GlowState.radius;
         case 9:
            return (float)GlowState.range;
         case 10:
            return (float)GlowState.maxEmitters;
         case 11:
            return (float)GlowState.downscale;
         case 12:
            return GlowState.budgetMs;
         default:
            return 0.0F;
      }
   }

   private void setFloat(int id, float v) {
      switch (id) {
         case 7:
            GlowState.strength = v;
            break;
         case 8:
            GlowState.radius = v;
            break;
         case 9:
            GlowState.range = Math.round(v);
            GlowState.range = clampI(GlowState.range, 4, 96);
            break;
         case 10:
            GlowState.maxEmitters = clampI(Math.round(v), 8, 512);
            break;
         case 11:
            GlowState.downscale = clampI(Math.round(v), 1, 8);
            break;
         case 12:
            GlowState.budgetMs = v;
      }
   }

   private static int clampI(int v, int lo, int hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private void cycle(int id) {
      if (id == 13) {
         Tier[] all = new Tier[]{Tier.AUTO, Tier.ULTRA, Tier.HIGH, Tier.BALANCED, Tier.LITE, Tier.OFF};
         GlowState.tierMode = next(all, GlowState.tierMode);
         GlowState.applyTier();
      } else if (id == 14) {
         GlowState.RuleMode[] all = new GlowState.RuleMode[]{
            GlowState.RuleMode.RARITY, GlowState.RuleMode.ENCHANTED, GlowState.RuleMode.ALL, GlowState.RuleMode.WHITELIST
         };
         GlowState.ruleMode = next(all, GlowState.ruleMode);
         GlowRules.invalidate();
      } else if (id == 15) {
         int[] presets = new int[]{3727871, 16732067, 5767088, 16761963, 11570431};
         int idx = 0;

         for (int i = 0; i < presets.length; i++) {
            if (presets[i] == GlowState.accent) {
               idx = i;
            }
         }

         GlowState.accent = presets[(idx + 1) % presets.length];
         GlowRules.invalidate();
      }

      ItemGlowClient.saveConfig();
   }

   private static <E> E next(E[] arr, E cur) {
      for (int i = 0; i < arr.length; i++) {
         if (arr[i] == cur) {
            return arr[(i + 1) % arr.length];
         }
      }

      return arr[0];
   }

   private String cycleText(int id) {
      if (id == 13) {
         return GlowState.tierMode == Tier.AUTO ? "AUTO→" + GlowState.activeTier.name() : GlowState.tierMode.name();
      } else if (id == 14) {
         return GlowState.ruleMode.name();
      } else {
         return id == 15 ? String.format("#%06X", GlowState.accent) : "";
      }
   }

   private String valueText(GlowConfigScreen.Row row, float v) {
      return row.id != 9 && row.id != 10 && row.id != 11 ? String.format(Locale.ROOT, "%.2f", v) : Integer.toString(Math.round(v));
   }

   private boolean contains(GlowConfigScreen.Row row, double mx, double my) {
      return mx >= (double)(this.panelX + 8) && mx <= (double)(this.panelX + this.panelW - 8) && my >= (double)row.y && my <= (double)(row.y + 20);
   }

   private boolean hovered(GlowConfigScreen.Row row, int mx, int my) {
      return mx >= this.panelX + 8 && mx <= this.panelX + this.panelW - 8 && my >= row.y && my <= row.y + 20;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static final class ButtonRegion {
      int x;
      int y;
      int w;
      int id;
   }

   private interface FontDisplayMode {
      int NORMAL = 0;
   }

   private static final class Regions {
      private final GlowConfigScreen.ButtonRegion[] arr = new GlowConfigScreen.ButtonRegion[8];

      void ensure(int id) {
      }

      void set(int i, GlowConfigScreen.ButtonRegion r) {
         if (i >= 0 && i < this.arr.length) {
            this.arr[i] = r;
         }
      }

      GlowConfigScreen.ButtonRegion get(int i) {
         return i >= 0 && i < this.arr.length ? this.arr[i] : null;
      }

      int size() {
         return this.arr.length;
      }
   }

   private static final class Row {
      final int id;
      final int kind;
      final String label;
      final float min;
      final float max;
      int y = -1000;
      int hitX;
      int hitW;

      Row(int id, int kind, String label) {
         this(id, kind, label, 0.0F, 0.0F);
      }

      Row(int id, int kind, String label, float min, float max) {
         this.id = id;
         this.kind = kind;
         this.label = label;
         this.min = min;
         this.max = max;
      }
   }
}
