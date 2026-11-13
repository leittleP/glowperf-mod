package dev.qwen.glowperf.perf.client.ui;

import dev.qwen.glowperf.glow.core.GlowState;
import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.client.hud.PerfHud;
import dev.qwen.glowperf.perf.command.PerfCommand;
import dev.qwen.glowperf.perf.core.Counters;
import dev.qwen.glowperf.perf.core.EntityRules;
import dev.qwen.glowperf.perf.core.FrameProbe;
import dev.qwen.glowperf.perf.core.GovernorLite;
import dev.qwen.glowperf.perf.core.MemWatch;
import dev.qwen.glowperf.perf.core.PerfState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PerfConfigScreen extends Screen {
   private static final int TOGGLE = 0;
   private static final int SLIDER = 1;
   private static final int CYCLE = 2;
   private final Screen parent;
   private final List<PerfConfigScreen.Row> rows = new ArrayList<>();
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
   private static final int B_GC = -5;
   private final PerfConfigScreen.Region rSave = new PerfConfigScreen.Region();
   private final PerfConfigScreen.Region rGc = new PerfConfigScreen.Region();
   private final PerfConfigScreen.Region rReset = new PerfConfigScreen.Region();
   private final PerfConfigScreen.Region rClose = new PerfConfigScreen.Region();

   public PerfConfigScreen(Screen var1) {
      super(Component.translatable("screen.perfcore.settings"));
      this.parent = var1;
   }

   protected void init() {
      this.rows.clear();
      this.rows.add(new PerfConfigScreen.Row(0, 0, "性能优化总开关"));
      this.rows.add(new PerfConfigScreen.Row(1, 0, "远处生物渲染剔除"));
      this.rows.add(new PerfConfigScreen.Row(2, 0, "堆内存看护"));
      this.rows.add(new PerfConfigScreen.Row(3, 0, "帧与 tick 诊断"));
      this.rows.add(new PerfConfigScreen.Row(4, 0, "性能面板"));
      this.rows.add(new PerfConfigScreen.Row(5, 0, "远处玩家剔除[激进]"));
      this.rows.add(new PerfConfigScreen.Row(6, 0, "生成护栏[激进]"));
      this.rows.add(new PerfConfigScreen.Row(7, 0, "远处 AI 冻结[激进]"));
      this.rows.add(new PerfConfigScreen.Row(8, 1, "生物保留距离", 16.0F, 192.0F));
      this.rows.add(new PerfConfigScreen.Row(9, 1, "小体型保留距离", 8.0F, 128.0F));
      this.rows.add(new PerfConfigScreen.Row(10, 1, "生成数量上限", 16.0F, 512.0F));
      this.rows.add(new PerfConfigScreen.Row(11, 1, "帧预算(ms)", 5.0F, 100.0F));
      this.rows.add(new PerfConfigScreen.Row(12, 1, "内存收紧阈值", 0.5F, 0.95F));
      this.rows.add(new PerfConfigScreen.Row(13, 2, "档位"));
      this.rows.add(new PerfConfigScreen.Row(14, 2, "光影联动"));
      this.rows.add(new PerfConfigScreen.Row(15, 0, "坐标显示"));
      this.panelW = Math.min(280, this.width - 40);
      this.panelH = 40 + this.rows.size() * 24 + 34;
      this.panelX = (this.width - this.panelW) / 2;
      this.panelY = Math.max(12, (this.height - this.panelH) / 2);
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      var1.fill(0, 0, this.width, this.height, -1476130286);
      VrTheme.panel(var1, this.panelX, this.panelY, this.panelW, this.panelH);
      VrTheme.scanline(var1, this.panelX, this.panelY, this.panelW, this.panelH);
      int var5 = this.panelY + 10;
      var1.drawCenteredString(this.font, VrTheme.title(), this.width / 2, var5, -13049345);
      var1.drawString(this.font, PerfHud.statsLine(), this.panelX + 10, var5 + 12, FrameProbe.fps() >= 60.0F ? -11010128 : -15253);
      int var6 = this.panelY + 48;

      for (PerfConfigScreen.Row var8 : this.rows) {
         var8.y = var6;
         boolean var9 = this.mx(var2) && this.my(var6, var3);
         var1.drawString(this.font, var8.label, this.panelX + 10, var6 + 2, var9 ? -1 : -2689537);
         if (var8.kind == 0) {
            VrTheme.toggle(var1, this.panelX + this.panelW - 46, var6, this.getBool(var8.id), "");
         } else if (var8.kind == 1) {
            float var10 = this.getFloat(var8.id);
            float var11 = (var10 - var8.min) / Math.max(1.0E-4F, var8.max - var8.min);
            var8.hitX = this.panelX + this.panelW - 118;
            var8.hitW = 104;
            VrTheme.slider(var1, var8.hitX, var6 + 1, 104, var11, "", this.valueText(var8, var10));
         } else {
            var8.hitX = this.panelX + this.panelW - 128;
            var8.hitW = 118;
            var1.drawString(this.font, this.cycleText(var8.id), var8.hitX, var6 + 2, var9 ? -45149 : -13049345);
         }

         var6 += 24;
      }

      int var12 = this.panelY + this.panelH - 24;
      this.button(var1, "保存", -2, this.panelX + 10, var12, var2, var3);
      this.button(var1, "回收", -5, this.panelX + 66, var12, var2, var3);
      this.button(var1, "默认", -3, this.panelX + 122, var12, var2, var3);
      this.button(var1, "关闭", -4, this.panelX + this.panelW - 60, var12, var2, var3);
      if (this.toastTimer > 0) {
         this.toastTimer--;
         int var13 = this.font.width(this.toast) + 14;
         VrTheme.panel(var1, this.width / 2 - var13 / 2, this.panelY + this.panelH + 6, var13, 18, -871951585);
         var1.drawCenteredString(this.font, this.toast, this.width / 2, this.panelY + this.panelH + 11, -11010128);
      }
   }

   private boolean mx(int var1) {
      return var1 >= this.panelX + 8 && var1 <= this.panelX + this.panelW - 8;
   }

   private boolean my(int var1, int var2) {
      return var2 >= var1 && var2 <= var1 + 20;
   }

   private void button(GuiGraphics var1, String var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = this.font.width(var2) + 16;
      boolean var9 = var6 >= var4 && var6 <= var4 + var8 && var7 >= var5 && var7 <= var5 + 14;
      var1.fill(var4, var5, var4 + var8, var5 + 14, var9 ? -2009538049 : 1073741824);
      var1.fill(var4, var5, var4 + var8, var5 + 1, var9 ? -13049345 : 859365887);
      var1.fill(var4, var5 + 13, var4 + var8, var5 + 14, var9 ? -13049345 : 859365887);
      var1.drawString(this.font, var2, var4 + 8, var5 + 3, var9 ? -1 : -2689537);
      this.region(var3).set(var4, var5, var8);
   }

   private PerfConfigScreen.Region region(int var1) {
      switch (var1) {
         case -5:
            return this.rGc;
         case -4:
         default:
            return this.rClose;
         case -3:
            return this.rReset;
         case -2:
            return this.rSave;
      }
   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (this.rSave.hit((int)var1, (int)var3)) {
         this.say(PerfCommand.persist() ? "已保存到 perfcore.toml" : "已记录");
         return true;
      } else if (this.rGc.hit((int)var1, (int)var3)) {
         this.say("已发出回收提示，释放 " + Math.max(0L, MemWatch.hintGc()) + " MB");
         return true;
      } else if (this.rReset.hit((int)var1, (int)var3)) {
         PerfState.resetDefaults();
         GovernorLite.reset();
         FrameProbe.reset();
         EntityRules.resetAllFrozen();
         this.say("已恢复默认参数");
         return true;
      } else if (this.rClose.hit((int)var1, (int)var3)) {
         this.backToGame();
         return true;
      } else {
         for (PerfConfigScreen.Row var7 : this.rows) {
            if (this.mx((int)var1) && this.my(var7.y, (int)var3)) {
               if (var7.kind == 0) {
                  this.setBool(var7.id, !this.getBool(var7.id));
                  this.say(var7.label + "：" + (this.getBool(var7.id) ? "开" : "关"));
                  return true;
               }

               if (var7.kind == 2) {
                  this.cycle(var7.id);
                  this.say(var7.label + " → " + this.cycleText(var7.id));
                  return true;
               }

               this.dragging = var7.id;
               this.applySlider(var7, var1);
               return true;
            }
         }

         return super.mouseClicked(var1, var3, var5);
      }
   }

   public boolean mouseDragged(double var1, double var3, int var5, double var6, double var8) {
      if (this.dragging < 0) {
         return super.mouseDragged(var1, var3, var5, var6, var8);
      } else {
         for (PerfConfigScreen.Row var11 : this.rows) {
            if (var11.id == this.dragging) {
               this.applySlider(var11, var1);
               break;
            }
         }

         return true;
      }
   }

   public boolean mouseReleased(double var1, double var3, int var5) {
      this.dragging = -1;
      return super.mouseReleased(var1, var3, var5);
   }

   private void applySlider(PerfConfigScreen.Row var1, double var2) {
      float var4 = (float)((var2 - (double)var1.hitX) / (double)Math.max(1, var1.hitW));
      var4 = Math.max(0.0F, Math.min(1.0F, var4));
      this.setFloat(var1.id, var1.min + var4 * (var1.max - var1.min));
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (var1 != 256 && var1 != 257) {
         return super.keyPressed(var1, var2, var3);
      } else {
         this.backToGame();
         return true;
      }
   }

   private void backToGame() {
      PerfCommand.persist();
      this.minecraft.setScreen(this.parent);
   }

   public void removed() {
      super.removed();
      PerfCommand.persist();
   }

   private void say(String var1) {
      this.toast = var1;
      this.toastTimer = 60;
   }

   private boolean getBool(int var1) {
      switch (var1) {
         case 0:
            return PerfState.enabled;
         case 1:
            return PerfState.cullLiving;
         case 2:
            return PerfState.memWatch;
         case 3:
            return PerfState.probe;
         case 4:
            return PerfState.panel;
         case 5:
            return PerfState.cullPlayer;
         case 6:
            return PerfState.spawnCap;
         case 7:
            return PerfState.idleNoAi;
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         default:
            return false;
         case 15:
            return PerfState.coords;
      }
   }

   private void setBool(int var1, boolean var2) {
      switch (var1) {
         case 0:
            PerfState.enabled = var2;
            PerfState.applyPreset();
            break;
         case 1:
            PerfState.cullLiving = var2;
            break;
         case 2:
            PerfState.memWatch = var2;
            break;
         case 3:
            PerfState.probe = var2;
            break;
         case 4:
            PerfState.panel = var2;
            break;
         case 5:
            PerfState.cullPlayer = var2;
            break;
         case 6:
            PerfState.spawnCap = var2;
            break;
         case 7:
            PerfState.idleNoAi = var2;
            if (!var2) {
               EntityRules.resetAllFrozen();
            }
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         default:
            break;
         case 15:
            PerfState.coords = var2;
      }

      PerfClient.persist();
   }

   private float getFloat(int var1) {
      switch (var1) {
         case 8:
            return (float)PerfState.cullDistance;
         case 9:
            return (float)PerfState.cullDistanceSmall;
         case 10:
            return (float)PerfState.spawnCapCount;
         case 11:
            return PerfState.budgetMs;
         case 12:
            return PerfState.memSoftLimit;
         default:
            return 0.0F;
      }
   }

   private void setFloat(int var1, float var2) {
      switch (var1) {
         case 8:
            PerfState.cullDistance = Math.round(var2);
            break;
         case 9:
            PerfState.cullDistanceSmall = Math.round(var2);
            break;
         case 10:
            PerfState.spawnCapCount = Math.round(var2);
            break;
         case 11:
            PerfState.budgetMs = var2;
            break;
         case 12:
            PerfState.memSoftLimit = Math.max(0.4F, Math.min(0.97F, var2));
      }
   }

   private void cycle(int var1) {
      if (var1 == 13) {
         PerfState.Preset[] var2 = new PerfState.Preset[]{
            PerfState.Preset.AUTO, PerfState.Preset.OFF, PerfState.Preset.MILD, PerfState.Preset.MODERATE, PerfState.Preset.SEVERE, PerfState.Preset.NO_MATTER
         };
         PerfState.preset = next(var2, PerfState.preset);
         PerfState.applyPreset();
      } else if (var1 == 14) {
         PerfState.GlowMode[] var3 = new PerfState.GlowMode[]{
            PerfState.GlowMode.ON, PerfState.GlowMode.AUTO, PerfState.GlowMode.LITE, PerfState.GlowMode.OFF, PerfState.GlowMode.IGNORE
         };
         PerfState.glowMode = next(var3, PerfState.glowMode);
         PerfClient.applyGlowPublic(PerfState.glowMode == PerfState.GlowMode.OFF ? 2 : (PerfState.glowMode == PerfState.GlowMode.LITE ? 1 : 0));
      }

      PerfClient.persist();
   }

   private static <E> E next(E[] var0, E var1) {
      for (int var2 = 0; var2 < var0.length; var2++) {
         if (var0[var2] == var1) {
            return (E)var0[(var2 + 1) % var0.length];
         }
      }

      return (E)var0[0];
   }

   private String cycleText(int var1) {
      if (var1 == 13) {
         return PerfState.preset == PerfState.Preset.AUTO
            ? "自动→" + GovernorLite.describe()
            : PerfState.preset.label + "（当前 " + PerfState.effective.label + "）";
      } else {
         return var1 == 14
            ? PerfState.glowMode.label()
               + " · 光影 "
               + (GlowState.enabled ? GlowState.activeTier.name() : "关闭")
               + (PerfState.glowApplied == 1 ? "（柔光）" : (PerfState.glowApplied == 2 ? "（已关）" : ""))
            : "";
      }
   }

   private String valueText(PerfConfigScreen.Row var1, float var2) {
      if (var1.id == 12) {
         return String.format(Locale.ROOT, "%.0f%%", var2 * 100.0F);
      } else {
         return var1.id == 11 ? String.format(Locale.ROOT, "%.0f", var2) : Integer.toString(Math.round(var2));
      }
   }

   public static String liveLine() {
      return String.format(
         "剔除 %d ｜ 冻结 %d ｜ 长帧 %d ｜ 内存 %.0f%%", Counters.shownCulled, EntityRules.frozenCount(), Counters.longFramesTotal, MemWatch.ratio() * 100.0F
      );
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static final class Region {
      int x;
      int y;
      int w;

      void set(int var1, int var2, int var3) {
         this.x = var1;
         this.y = var2;
         this.w = var3;
      }

      boolean hit(int var1, int var2) {
         return var1 >= this.x && var1 <= this.x + this.w && var2 >= this.y && var2 <= this.y + 14;
      }
   }

   private static final class Row {
      final int id;
      final int kind;
      final String label;
      final float min;
      final float max;
      int y = -2000;
      int hitX;
      int hitW;

      Row(int var1, int var2, String var3) {
         this(var1, var2, var3, 0.0F, 0.0F);
      }

      Row(int var1, int var2, String var3, float var4, float var5) {
         this.id = var1;
         this.kind = var2;
         this.label = var3;
         this.min = var4;
         this.max = var5;
      }
   }
}
