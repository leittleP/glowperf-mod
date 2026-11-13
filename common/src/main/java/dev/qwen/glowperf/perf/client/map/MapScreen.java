package dev.qwen.glowperf.perf.client.map;

import dev.qwen.glowperf.perf.client.PerfClient;
import dev.qwen.glowperf.perf.client.ui.VrTheme;
import dev.qwen.glowperf.perf.core.PerfState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 全屏小地图：/open the map 打开，/close the map 关闭。
 * 支持拖拽平移、滚轮缩放、点击传送、坐标传送、玩家传送、洞穴/立体/标记切换。
 */
public final class MapScreen extends Screen {
   private static final int COLOR_BG = -872415232;
   private static final int COLOR_GRID = 872415231;
   private static final int COLOR_PLAYER = -1;
   private static final String[] BUTTONS = new String[]{"坐标", "玩家", "路点", "洞穴", "立体", "标记", "缩小", "回中", "放大"};
   private int mapX;
   private int mapY;
   private int mapSize;
   private final MapRaster raster = new MapRaster();
   private final List<EntityScanner.Marker> markers = new ArrayList<>();
   private final List<EditBox> fields = new ArrayList<>();
   private int mapCenterX;
   private int mapCenterY;
   private long lastMarkerMs;
   private boolean dragging;
   private double dragStartX;
   private double dragStartZ;
   private double dragMouseX;
   private double dragMouseY;
   private boolean showCoordDialog;
   private boolean showPlayerDialog;
   private boolean showWaypointDialog;
   private List<Waypoint> wpRows = new ArrayList<>();
   private EditBox fieldX;
   private EditBox fieldY;
   private EditBox fieldZ;
   private List<String> playerNames = new ArrayList<>();
   private String dialogMsg = "";

   public MapScreen() {
      super(Component.literal("GlowPerf 小地图"));
      this.mapX = PerfState.mapX;
      this.mapY = PerfState.mapY;
      this.mapSize = Math.max(120, PerfState.mapSize);
   }

   protected void init() {
      Minecraft mc = Minecraft.getInstance();
      Font font = mc.font;
      this.mapSize = Math.max(120, Math.min(PerfState.mapSize, Math.min(this.width - 24, this.height - 150)));
      this.mapX = Math.max(8, Math.min(PerfState.mapX, this.width - this.mapSize - 20));
      this.mapY = Math.max(32, Math.min(PerfState.mapY, this.height - this.panelH() - 8));
      this.mapCenterX = this.mapX + this.mapSize / 2;
      this.mapCenterY = this.mapY + this.mapSize / 2;
      int dx = this.mapX + 10;
      int dy = this.mapY + 40;
      int dw = this.mapSize - 20;
      int fw = Math.max(40, (dw - 20 - 12) / 3);
      this.fieldX = new EditBox(font, dx + 10, dy + 30, fw, 16, Component.literal("X"));
      this.fieldY = new EditBox(font, dx + 10 + fw + 6, dy + 30, fw, 16, Component.literal("Y"));
      this.fieldZ = new EditBox(font, dx + 10 + 2 * (fw + 6), dy + 30, fw, 16, Component.literal("Z"));
      this.fieldX.deleteWords(8);
      this.fieldY.deleteWords(8);
      this.fieldZ.deleteWords(8);
      this.fieldX.setValue("0");
      this.fieldY.setValue("64");
      this.fieldZ.setValue("0");
      this.fields.add(this.fieldX);
      this.fields.add(this.fieldY);
      this.fields.add(this.fieldZ);
   }

   public void removed() {
      super.removed();
      MapState.open = false;
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      LocalPlayer player = mc.player;
      Font font = mc.font;
      if (level != null && player != null) {
         boolean cave = MapState.caveManual || (PerfState.mapCaveAuto && player.getY() < 0.0);
         MapState.cave = cave;
         MapState.dim = MapDraw.dimName(level);
         double cx = player.getX() + MapState.panX;
         double cz = player.getZ() + MapState.panZ;
         long now = System.currentTimeMillis();
         if (this.raster.needs(level, cx, cz, MapState.scale, cave, MapState.stereo, now)) {
            this.raster.build(level, cx, cz, this.mapSize, MapState.scale, cave, MapState.stereo);
         }

         if (PerfState.mapMarkers && now - this.lastMarkerMs > 300L) {
            this.lastMarkerMs = now;
            double radius = (double)this.mapSize / 2.0 / MapState.scale + 16.0;
            this.markers.clear();
            this.markers.addAll(EntityScanner.scan(level, player.getX(), player.getZ(), radius));
         }

         VrTheme.panel(g, this.mapX - 4, this.mapY - 28, this.mapSize + 8, this.panelH());
         g.fill(this.mapX, this.mapY, this.mapX + this.mapSize, this.mapY + this.mapSize, COLOR_BG);
         this.raster.draw(g, this.mapCenterX, this.mapCenterY, this.mapX, this.mapY, this.mapSize, this.mapSize);
         this.drawGrid(g, font, cx, cz);
         if (cave) {
            MapDraw.caveTint(g, this.mapX, this.mapY, this.mapSize, this.mapSize);
         }

         if (PerfState.mapMarkers) {
            MapDraw.markers(g, this.markers, cx, cz, this.mapCenterX, this.mapCenterY, MapState.scale, this.mapX, this.mapY, this.mapSize, this.mapSize);
         }

         MapDraw.waypoints(
            g,
            font,
            WaypointStore.all(),
            MapState.dim,
            cx,
            cz,
            this.mapCenterX,
            this.mapCenterY,
            MapState.scale,
            this.mapX,
            this.mapY,
            this.mapSize,
            this.mapSize,
            true
         );
         MapDraw.arrow(g, this.mapCenterX, this.mapCenterY, player.getYRot());
         this.drawScaleBar(g, font, mouseX, mouseY);
         this.drawHoverInfo(g, font, level, player, mouseX, mouseY);
         this.drawTopBar(g, font, level, player);
         this.drawClose(g, font, mouseX, mouseY);
         this.drawButtons(g, font, mouseX, mouseY);
         this.drawLegend(g, font);
         if (this.showCoordDialog) {
            this.drawCoordDialog(g, font, mouseX, mouseY);
         }

         if (this.showPlayerDialog) {
            this.drawPlayerDialog(g, font, mouseX, mouseY);
         }

         if (this.showWaypointDialog) {
            this.drawWaypointDialog(g, font, mouseX, mouseY);
         }
      }
   }

   private int panelH() {
      return 28 + this.mapSize + 6 + 3 * 20 + 26;
   }

   private int buttonWidth() {
      return Math.max(40, (this.mapSize - 8) / 3);
   }

   private int buttonX(int i) {
      return this.mapX + i % 3 * (this.buttonWidth() + 4);
   }

   private int buttonY(int i) {
      return this.mapY + this.mapSize + 6 + i / 3 * 20;
   }

   private int closeX() {
      return this.mapX + this.mapSize - 12;
   }

   private int closeY() {
      return this.mapY - 25;
   }

   private void drawGrid(GuiGraphics g, Font font, double cx, double cz) {
      double sc = MapState.scale;
      if (sc < 2.0) {
         return;
      }

      int minX = (int)Math.floor(cx - (double)this.mapSize / 2.0 / sc);
      int maxX = (int)Math.ceil(cx + (double)this.mapSize / 2.0 / sc);
      int minZ = (int)Math.floor(cz - (double)this.mapSize / 2.0 / sc);
      int maxZ = (int)Math.ceil(cz + (double)this.mapSize / 2.0 / sc);

      for (int wx = minX; wx <= maxX; wx++) {
         if (wx % 16 == 0) {
            double px = (double)this.mapCenterX + ((double)wx - cx) * sc;
            if (px >= (double)this.mapX && px <= (double)(this.mapX + this.mapSize)) {
               g.fill((int)px, this.mapY, (int)px + 1, this.mapY + this.mapSize, COLOR_GRID);
            }
         }
      }

      for (int wz = minZ; wz <= maxZ; wz++) {
         if (wz % 16 == 0) {
            double py = (double)this.mapCenterY + ((double)wz - cz) * sc;
            if (py >= (double)this.mapY && py <= (double)(this.mapY + this.mapSize)) {
               g.fill(this.mapX, (int)py, this.mapX + this.mapSize, (int)py + 1, COLOR_GRID);
            }
         }
      }

      if (sc >= 6.0 && font != null) {
         int c0x = Math.floorDiv(minX, 16);
         int c1x = Math.floorDiv(maxX, 16);
         int c0z = Math.floorDiv(minZ, 16);
         int c1z = Math.floorDiv(maxZ, 16);

         for (int ccx = c0x; ccx <= c1x; ccx++) {
            for (int ccz = c0z; ccz <= c1z; ccz++) {
               double px = (double)this.mapCenterX + ((double)(ccx * 16 + 8) - cx) * sc;
               double py = (double)this.mapCenterY + ((double)(ccz * 16 + 8) - cz) * sc;
               if (px >= (double)this.mapX && px <= (double)(this.mapX + this.mapSize) && py >= (double)this.mapY && py <= (double)(this.mapY + this.mapSize)) {
                  String label = ccx + "," + ccz;
                  g.drawString(font, label, (int)px - font.width(label) / 2, (int)py - 4, 1728053247);
               }
            }
         }
      }
   }

   private void drawScaleBar(GuiGraphics g, Font font, int mouseX, int mouseY) {
      int barW = (int)Math.max(8.0, 16.0 * MapState.scale);
      int bx = this.mapX + this.mapSize - barW - 6;
      int by = this.mapY + 14;
      g.fill(bx, by, bx + barW, by + 2, -1);
      g.fill(bx, by - 2, bx + 1, by + 4, -1);
      g.fill(bx + barW - 1, by - 2, bx + barW, by + 4, -1);
      String label = "16格  缩放 " + String.format("%.1f", MapState.scale) + "x";
      g.drawString(font, label, bx + (barW - font.width(label)) / 2, by - 11, VrTheme.TEXT_DIM);
   }

   private void drawHoverInfo(GuiGraphics g, Font font, ClientLevel level, LocalPlayer player, int mouseX, int mouseY) {
      if (mouseX < this.mapX || mouseX > this.mapX + this.mapSize || mouseY < this.mapY || mouseY > this.mapY + this.mapSize) {
         return;
      }

      double sc = MapState.scale;
      double wx = player.getX() + MapState.panX + ((double)mouseX - (double)this.mapCenterX) / sc;
      double wz = player.getZ() + MapState.panZ + ((double)mouseY - (double)this.mapCenterY) / sc;
      double dist = Math.hypot(wx - player.getX(), wz - player.getZ());
      String biome = MapDraw.biomeName(level, new net.minecraft.core.BlockPos((int)Math.floor(wx), (int)player.getY(), (int)Math.floor(wz)));
      String s = String.format("X %d  Z %d  距离 %d  区块 %d,%d  %s", (int)Math.floor(wx), (int)Math.floor(wz), (int)dist,
         Math.floorDiv((int)Math.floor(wx), 16), Math.floorDiv((int)Math.floor(wz), 16), biome);
      g.fill(this.mapX + 3, this.mapY + this.mapSize - 14, this.mapX + 3 + font.width(s) + 6, this.mapY + this.mapSize - 3, -1442840576);
      g.drawString(font, s, this.mapX + 6, this.mapY + this.mapSize - 12, -1);
   }

   private void drawClose(GuiGraphics g, Font font, int mouseX, int mouseY) {
      int x = this.closeX();
      int y = this.closeY();
      boolean hover = mouseX >= x && mouseX <= x + 12 && mouseY >= y && mouseY <= y + 12;
      g.fill(x, y, x + 12, y + 12, hover ? -3381760 : -15066598);
      g.fill(x, y, x + 12, y + 1, VrTheme.BAD);
      g.drawString(font, "x", x + 4, y + 2, hover ? -1 : VrTheme.BAD);
   }

   private void drawWaypointDialog(GuiGraphics g, Font font, int mouseX, int mouseY) {
      int dx = this.mapX + 10;
      int dy = this.mapY + 40;
      int dw = this.mapSize - 20;
      int dh = 150;
      g.fill(dx, dy, dx + dw, dy + dh, -268435456);
      g.fill(dx, dy, dx + dw, dy + 1, VrTheme.NEON);
      g.drawString(font, "路点（点击传送，x 删除）", dx + 8, dy + 6, VrTheme.NEON);
      this.wpRows = new ArrayList<>();

      for (Waypoint w : WaypointStore.all()) {
         if (MapState.dim.equals(w.dim)) {
            this.wpRows.add(w);
         }
      }

      if (this.wpRows.isEmpty()) {
         g.drawString(font, "本维度暂无路点，右键地图或 /wp add 添加", dx + 8, dy + 30, VrTheme.TEXT_DIM);
      } else {
         for (int i = 0; i < this.wpRows.size() && i < 8; i++) {
            Waypoint w = this.wpRows.get(i);
            int rowY = dy + 28 + i * 14;
            boolean hover = mouseX >= dx + 8 && mouseX <= dx + dw - 24 && mouseY >= rowY && mouseY <= rowY + 12;
            if (hover) {
               g.fill(dx + 8, rowY, dx + dw - 8, rowY + 12, 1157627903);
            }

            g.fill(dx + 10, rowY + 3, dx + 15, rowY + 8, w.color);
            String line = w.name + "  " + (int)w.x + ", " + (int)w.y + ", " + (int)w.z;
            g.drawString(font, line, dx + 18, rowY + 2, hover ? VrTheme.NEON : VrTheme.TEXT);
            boolean delHover = mouseX >= dx + dw - 22 && mouseX <= dx + dw - 10 && mouseY >= rowY && mouseY <= rowY + 12;
            g.drawString(font, "x", dx + dw - 20, rowY + 2, delHover ? -1 : VrTheme.BAD);
         }
      }

      this.drawTextButton(g, font, dx + dw - 60, dy + dh - 22, 50, 16, "关闭", VrTheme.BAD, mouseX, mouseY);
   }

   private void drawTopBar(GuiGraphics g, Font font, ClientLevel level, LocalPlayer player) {
      String dim = MapState.dim;
      String biome = MapDraw.biomeName(level, player.blockPosition());
      String title = "小地图  " + dim + (MapState.cave ? " [洞穴]" : "") + (MapState.stereo ? " [立体]" : "");
      g.drawString(font, title, this.mapX, this.mapY - 22, -13049345);
      String line = String.format("X %.1f  Y %.1f  Z %.1f  |  %s", player.getX(), player.getY(), player.getZ(), biome);
      g.drawString(font, line, this.mapX, this.mapY - 10, -2689537);
   }

   private void drawButtons(GuiGraphics g, Font font, int mouseX, int mouseY) {
      int bw = this.buttonWidth();

      for (int i = 0; i < BUTTONS.length; i++) {
         int bx = this.buttonX(i);
         int by = this.buttonY(i);
         boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + 16;
         int bg = hover ? -15058342 : -16115158;
         g.fill(bx, by, bx + bw, by + 16, bg);
         g.fill(bx, by, bx + bw, by + 1, VrTheme.NEON_DIM);
         g.fill(bx, by + 15, bx + bw, by + 16, VrTheme.NEON_FAINT);
         int text = -2689537;
         if (i == 3 && MapState.cave) {
            text = -11010128;
         }

         if (i == 4 && MapState.stereo) {
            text = -11010128;
         }

         if (i == 5 && PerfState.mapMarkers) {
            text = -11010128;
         }

         g.drawString(font, BUTTONS[i], bx + (bw - font.width(BUTTONS[i])) / 2, by + 4, text);
      }
   }

   private void drawLegend(GuiGraphics g, Font font) {
      int y = this.buttonY(8) + 19;
      int x = this.mapX;
      String[] labels = new String[]{"掉落物", "刷怪笼", "敌对", "友好"};
      int[] colors = new int[]{
         EntityScanner.COLOR_ITEM, EntityScanner.COLOR_SPAWNER, EntityScanner.COLOR_HOSTILE, EntityScanner.COLOR_FRIENDLY
      };

      for (int i = 0; i < labels.length; i++) {
         int w = font.width(labels[i]);
         if (x + w + 14 > this.mapX + this.mapSize) {
            x = this.mapX;
            y += 10;
         }

         g.fill(x, y + 2, x + 4, y + 6, colors[i]);
         g.drawString(font, labels[i], x + 7, y, VrTheme.TEXT_DIM);
         x += w + 14;
      }

      g.drawString(font, "右键加路点  左键传送  滚轮缩放  按钮操控", this.mapX, y + 12, VrTheme.NEON_FAINT);
   }

   private int buttonAt(int mx, int my) {
      int bw = this.buttonWidth();

      for (int i = 0; i < BUTTONS.length; i++) {
         int bx = this.buttonX(i);
         int by = this.buttonY(i);
         if (mx >= bx && mx <= bx + bw && my >= by && my <= by + 16) {
            return i;
         }
      }

      return -1;
   }

   private void drawCoordDialog(GuiGraphics g, Font font, int mouseX, int mouseY) {
      int dx = this.mapX + 10;
      int dy = this.mapY + 40;
      int dw = this.mapSize - 20;
      int dh = 104;
      g.fill(dx, dy, dx + dw, dy + dh, -268435456);
      g.fill(dx, dy, dx + dw, dy + 1, VrTheme.NEON);
      g.drawString(font, "输入坐标传送", dx + 8, dy + 6, VrTheme.NEON);
      g.drawString(font, "X", this.fieldX.getX(), dy + 20, VrTheme.TEXT_DIM);
      g.drawString(font, "Y", this.fieldY.getX(), dy + 20, VrTheme.TEXT_DIM);
      g.drawString(font, "Z", this.fieldZ.getX(), dy + 20, VrTheme.TEXT_DIM);
      this.fieldX.render(g, mouseX, mouseY, 0.0F);
      this.fieldY.render(g, mouseX, mouseY, 0.0F);
      this.fieldZ.render(g, mouseX, mouseY, 0.0F);
      int bx = dx + dw - 110;
      int by = dy + dh - 26;
      this.drawTextButton(g, font, bx, by, 50, 16, "完成", VrTheme.OK, mouseX, mouseY);
      this.drawTextButton(g, font, bx + 56, by, 50, 16, "取消", VrTheme.BAD, mouseX, mouseY);
      if (!this.dialogMsg.isEmpty()) {
         g.drawString(font, this.dialogMsg, dx + 8, dy + dh - 12, VrTheme.WARN);
      }
   }

   private void drawTextButton(GuiGraphics g, Font font, int x, int y, int w, int h, String label, int accent, int mouseX, int mouseY) {
      boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
      g.fill(x, y, x + w, y + h, hover ? -14013910 : -15066598);
      g.fill(x, y, x + w, y + 1, accent);
      g.drawString(font, label, x + (w - font.width(label)) / 2, y + 4, hover ? -1 : accent);
   }

   private void drawPlayerDialog(GuiGraphics g, Font font, int mouseX, int mouseY) {
      int dx = this.mapX + 10;
      int dy = this.mapY + 40;
      int dw = this.mapSize - 20;
      int dh = 150;
      g.fill(dx, dy, dx + dw, dy + dh, -268435456);
      g.fill(dx, dy, dx + dw, dy + 1, VrTheme.NEON);
      g.drawString(font, "选择玩家传送", dx + 8, dy + 6, VrTheme.NEON);
      Minecraft mc = Minecraft.getInstance();
      this.playerNames = new ArrayList<>();
      if (mc.level != null) {
         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof Player && e != mc.player) {
               this.playerNames.add(e.getDisplayName().getString());
            }
         }
      }

      if (this.playerNames.isEmpty()) {
         g.drawString(font, "附近没有其他在线玩家", dx + 8, dy + 30, VrTheme.TEXT_DIM);
      } else {
         for (int i = 0; i < this.playerNames.size() && i < 8; i++) {
            int rowY = dy + 28 + i * 14;
            boolean hover = mouseX >= dx + 8 && mouseX <= dx + dw - 8 && mouseY >= rowY && mouseY <= rowY + 12;
            if (hover) {
               g.fill(dx + 8, rowY, dx + dw - 8, rowY + 12, 1157627903);
            }

            g.drawString(font, this.playerNames.get(i), dx + 12, rowY + 2, hover ? VrTheme.NEON : VrTheme.TEXT);
         }
      }

      this.drawTextButton(g, font, dx + dw - 60, dy + dh - 22, 50, 16, "关闭", VrTheme.BAD, mouseX, mouseY);
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (this.showCoordDialog) {
         EditBox hit = null;

         for (EditBox f : this.fields) {
            if (inRect(mx, my, f.getX(), f.getY(), f.getWidth(), f.getHeight())) {
               hit = f;
            }
         }

         for (EditBox f : this.fields) {
            f.setBordered(f == hit);
         }

         if (hit != null) {
            hit.mouseClicked(mx, my, button);
            return true;
         }

         int dx = this.mapX + 10;
         int dy = this.mapY + 40;
         int dw = this.mapSize - 20;
         int bx = dx + dw - 110;
         int by = dy + 104 - 26;
         if (inRect(mx, my, bx, by, 50, 16)) {
            this.doTeleportCoords();
            return true;
         }

         if (inRect(mx, my, bx + 56, by, 50, 16)) {
            this.showCoordDialog = false;
            return true;
         }

         return true;
      }

      if (this.showPlayerDialog) {
         int dx = this.mapX + 10;
         int dy = this.mapY + 40;
         int dw = this.mapSize - 20;
         for (int i = 0; i < this.playerNames.size() && i < 8; i++) {
            int rowY = dy + 28 + i * 14;
            if (inRect(mx, my, dx + 8, rowY, dw - 16, 12)) {
               Teleport.toPlayer(this.playerNames.get(i));
               this.showPlayerDialog = false;
               return true;
            }
         }

         if (inRect(mx, my, dx + dw - 60, dy + 128, 50, 16)) {
            this.showPlayerDialog = false;
         }

         return true;
      }

      if (this.showWaypointDialog) {
         int dx = this.mapX + 10;
         int dy = this.mapY + 40;
         int dw = this.mapSize - 20;
         for (int i = 0; i < this.wpRows.size() && i < 8; i++) {
            Waypoint w = this.wpRows.get(i);
            int rowY = dy + 28 + i * 14;
            if (inRect(mx, my, dx + dw - 22, rowY, 12, 12)) {
               WaypointStore.remove(w.name);
               WaypointStore.persist();
               return true;
            }

            if (inRect(mx, my, dx + 8, rowY, dw - 16, 12)) {
               Teleport.to(w.x, w.y, w.z);
               this.showWaypointDialog = false;
               return true;
            }
         }

         if (inRect(mx, my, dx + dw - 60, dy + 128, 50, 16)) {
            this.showWaypointDialog = false;
         }

         return true;
      }

      if (inRect(mx, my, this.closeX(), this.closeY(), 12, 12)) {
         this.onClose();
         return true;
      }

      int b = this.buttonAt((int)mx, (int)my);
      if (b >= 0) {
         this.handleButton(b);
         return true;
      }

      if (mx >= (double)this.mapX && mx <= (double)(this.mapX + this.mapSize) && my >= (double)this.mapY && my <= (double)(this.mapY + this.mapSize)) {
         if (button == 1) {
            this.addWaypointAt(mx, my);
            return true;
         }

         this.dragging = true;
         this.dragStartX = MapState.panX;
         this.dragStartZ = MapState.panZ;
         this.dragMouseX = mx;
         this.dragMouseY = my;
         return true;
      }

      return super.mouseClicked(mx, my, button);
   }

   public boolean mouseReleased(double mx, double my, int button) {
      if (this.dragging) {
         if (Math.abs(mx - this.dragMouseX) + Math.abs(my - this.dragMouseY) < 3.0) {
            this.teleportAt(mx, my);
         }

         this.dragging = false;
         return true;
      }

      return super.mouseReleased(mx, my, button);
   }

   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (this.dragging) {
         double sc = MapState.scale;
         MapState.panX = this.dragStartX - (mx - this.dragMouseX) / sc;
         MapState.panZ = this.dragStartZ - (my - this.dragMouseY) / sc;
         return true;
      }

      return super.mouseDragged(mx, my, button, dx, dy);
   }

   public boolean mouseScrolled(double mx, double my, double delta) {
      MapState.scale *= delta > 0.0 ? 1.2 : 0.8333333333333334;
      MapState.clampScale();
      return true;
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (this.showCoordDialog) {
         for (EditBox f : this.fields) {
            f.keyPressed(key, scan, mods);
         }

         if (key == 257) {
            this.doTeleportCoords();
         } else if (key == 256) {
            this.showCoordDialog = false;
         }

         return true;
      }

      if (key == 256) {
         if (this.showWaypointDialog || this.showPlayerDialog) {
            this.showWaypointDialog = false;
            this.showPlayerDialog = false;
            return true;
         }

         this.onClose();
         return true;
      }

      return super.keyPressed(key, scan, mods);
   }

   public boolean charTyped(char ch, int mods) {
      if (this.showCoordDialog) {
         for (EditBox f : this.fields) {
            f.charTyped(ch, mods);
         }

         return true;
      }

      return super.charTyped(ch, mods);
   }

   private void handleButton(int index) {
      Minecraft mc = Minecraft.getInstance();
      switch (index) {
         case 0:
            LocalPlayer p = mc.player;
            if (p != null) {
               this.fieldX.setValue(String.valueOf((int)p.getX()));
               this.fieldY.setValue(String.valueOf((int)p.getY()));
               this.fieldZ.setValue(String.valueOf((int)p.getZ()));
            }

            this.showCoordDialog = true;
            this.showPlayerDialog = false;
            this.showWaypointDialog = false;
            this.dialogMsg = "";
            this.fields.get(0).setBordered(true);
            this.fields.get(1).setBordered(false);
            this.fields.get(2).setBordered(false);
            break;
         case 1:
            this.showPlayerDialog = true;
            this.showCoordDialog = false;
            this.showWaypointDialog = false;
            break;
         case 2:
            this.showWaypointDialog = true;
            this.showCoordDialog = false;
            this.showPlayerDialog = false;
            break;
         case 3:
            MapState.caveManual = !MapState.caveManual;
            break;
         case 4:
            MapState.stereo = !MapState.stereo;
            break;
         case 5:
            PerfState.mapMarkers = !PerfState.mapMarkers;
            PerfClient.persist();
            break;
         case 6:
            MapState.scale *= 0.8333333333333334;
            MapState.clampScale();
            break;
         case 7:
            MapState.resetPan();
            break;
         case 8:
            MapState.scale *= 1.2;
            MapState.clampScale();
      }
   }

   private void addWaypointAt(double mx, double my) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      ClientLevel level = mc.level;
      if (p == null) {
         return;
      }

      double sc = MapState.scale;
      double wx = p.getX() + MapState.panX + (mx - (double)this.mapCenterX) / sc;
      double wz = p.getZ() + MapState.panZ + (my - (double)this.mapCenterY) / sc;
      double wy = MapDraw.safeY(level, wx, wz, p, MapState.cave);
      String name = "路点" + (WaypointStore.all().size() + 1);
      if (WaypointStore.add(new Waypoint(MapState.dim, wx, wy, wz, name, 0))) {
         WaypointStore.persist();
         p.displayClientMessage(Component.literal("[光影性能] 已添加路点 " + name + "，/wp tp " + name + " 可传送").withStyle(ChatFormatting.AQUA), false);
      } else {
         p.displayClientMessage(Component.literal("[光影性能] 路点已达上限 " + WaypointStore.MAX + " 个").withStyle(ChatFormatting.RED), false);
      }
   }

   private void teleportAt(double mx, double my) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      ClientLevel level = mc.level;
      if (p != null) {
         double sc = MapState.scale;
         double wx = p.getX() + MapState.panX + (mx - (double)this.mapCenterX) / sc;
         double wz = p.getZ() + MapState.panZ + (my - (double)this.mapCenterY) / sc;
         double wy = MapDraw.safeY(level, wx, wz, p, MapState.cave);
         Teleport.to(wx, wy, wz);
      }
   }

   private void doTeleportCoords() {
      try {
         double x = Double.parseDouble(this.fieldX.getHighlighted().trim());
         double y = Double.parseDouble(this.fieldY.getHighlighted().trim());
         double z = Double.parseDouble(this.fieldZ.getHighlighted().trim());
         Teleport.to(x, y, z);
         this.showCoordDialog = false;
      } catch (NumberFormatException e) {
         this.dialogMsg = "坐标需为数字";
      }
   }

   private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
      return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }
}
