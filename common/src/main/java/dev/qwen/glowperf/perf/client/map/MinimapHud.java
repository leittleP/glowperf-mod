package dev.qwen.glowperf.perf.client.map;

import dev.qwen.glowperf.perf.client.ui.VrTheme;
import dev.qwen.glowperf.perf.core.PerfState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

/**
 * 常驻角落小地图（游戏内始终显示）。/minimap on|off 切换。
 */
public final class MinimapHud {
   public static final MinimapHud INSTANCE = new MinimapHud();
   private final MapRaster raster = new MapRaster();
   private List<EntityScanner.Marker> markers = new ArrayList<>();
   private long lastScan;

   private MinimapHud() {
   }

   public void render(GuiGraphics g, float partialTick, int width, int height) {
      if (!PerfState.minimap) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      ClientLevel level = mc.level;
      if (p == null || level == null || mc.screen != null) {
         return;
      }

      int size = Math.max(64, Math.min(PerfState.minimapSize, Math.min(width, height) - 40));
      int x = Math.max(2, Math.min(PerfState.minimapX, width - size - 4));
      int y = Math.max(16, Math.min(PerfState.minimapY, height - size - 4));
      int cx = x + size / 2;
      int cy = y + size / 2;
      double cxw = p.getX();
      double czw = p.getZ();
      boolean cave = MapState.caveManual || (PerfState.mapCaveAuto && p.getY() < 0.0);
      MapState.cave = cave;
      MapState.dim = MapDraw.dimName(level);
      long now = System.currentTimeMillis();

      if (this.raster.needs(level, cxw, czw, MapState.scale, cave, MapState.stereo, now)) {
         this.raster.build(level, cxw, czw, size, MapState.scale, cave, MapState.stereo);
      }

      if (PerfState.mapMarkers && now - this.lastScan > 350L) {
         this.lastScan = now;
         double radius = (double)size / 2.0 / MapState.scale + 12.0;
         this.markers = EntityScanner.scan(level, cxw, czw, radius);
      }

      VrTheme.panel(g, x - 3, y - 14, size + 6, size + 17);
      g.fill(x, y, x + size, y + size, -872415232);
      this.raster.draw(g, cx, cy, x, y, size, size);
      if (cave) {
         MapDraw.caveTint(g, x, y, size, size);
      }

      if (PerfState.mapMarkers) {
         MapDraw.markers(g, this.markers, cxw, czw, cx, cy, MapState.scale, x, y, size, size);
      }

      MapDraw.waypoints(g, null, WaypointStore.all(), MapState.dim, cxw, czw, cx, cy, MapState.scale, x, y, size, size, false);
      MapDraw.arrow(g, cx, cy, p.getYRot());
      g.fill(x, y, x + size, y + 1, -13049345);
      g.fill(x, y + size - 1, x + size, y + size, -2143755777);
      g.fill(x, y, x + 1, y + size, -2143755777);
      g.fill(x + size - 1, y, x + size, y + size, -2143755777);

      Font font = mc.font;
      String title = MapState.dim + " · " + MapDraw.biomeName(level, p.blockPosition()) + " · Y" + (int)Math.floor(p.getY()) + (cave ? " · 洞穴" : "");
      if (font.width(title) > size + 4) {
         title = MapState.dim + " · Y" + (int)Math.floor(p.getY()) + (cave ? " · 洞穴" : "");
      }

      g.drawString(font, title, x, y - 11, -13049345);
   }
}
