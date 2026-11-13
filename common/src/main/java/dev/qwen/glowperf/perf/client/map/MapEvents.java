package dev.qwen.glowperf.perf.client.map;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MapEvents {
   private static final Set<UUID> GIVEN = new HashSet<>();
   private static final String MANUAL_TITLE = "GlowPerf 手册 Manual";
   private static final String CHANGELOG_TITLE = "GlowPerf 更新日志 Changelog";
   private static final String DEVELOPER_TITLE = "GlowPerf 开发构建 Guide";
   private static final String LEGACY_MANUAL_TITLE = "光影性能核心 · 手册";
   private static final String AUTHOR = "GlowPerf";

   private MapEvents() {
   }

   public static void onLogin(Player player) {
      if (player != null) {
         UUID id = player.getUUID();
         if (GIVEN.add(id) || !hasAllBooks(player)) {
            try {
               removeOldBooks(player);
               giveBook(player, createBook(MANUAL_TITLE, manualPages()));
               giveBook(player, createBook(CHANGELOG_TITLE, changelogPages()));
               giveBook(player, createBook(DEVELOPER_TITLE, developerPages()));
            } catch (Throwable ignored) {
            }
         }
      }
   }

   /** 三本书缺任意一本就视为丢失，下次登录补发。 */
   private static boolean hasAllBooks(Player player) {
      boolean manual = false;
      boolean changelog = false;
      boolean developer = false;
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack stack = inv.getItem(i);
         if (isOurBook(stack)) {
            String title = stack.getTag().getString("title");
            manual |= MANUAL_TITLE.equals(title);
            changelog |= CHANGELOG_TITLE.equals(title);
            developer |= DEVELOPER_TITLE.equals(title);
         }
      }

      return manual && changelog && developer;
   }

   /** 背包满时掉落到脚下，避免三本书被静默丢弃。 */
   private static void giveBook(Player player, ItemStack book) {
      if (!player.addItem(book)) {
         player.drop(book, false);
      }
   }

   /** 清掉背包里所有旧版/重复的手册与更新日志，保证只留新发的两本。 */
   private static void removeOldBooks(Player player) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         if (isOurBook(inv.getItem(i))) {
            inv.setItem(i, ItemStack.EMPTY);
         }
      }
   }

   private static boolean isOurBook(ItemStack stack) {
      if (stack == null || stack.isEmpty() || !stack.is(Items.WRITTEN_BOOK)) {
         return false;
      }

      CompoundTag tag = stack.getTag();
      if (tag == null) {
         return false;
      }

      String title = tag.getString("title");
      return MANUAL_TITLE.equals(title)
         || CHANGELOG_TITLE.equals(title)
         || DEVELOPER_TITLE.equals(title)
         || LEGACY_MANUAL_TITLE.equals(title)
         || title.startsWith("光影性能核心");
   }

   private static ItemStack createBook(String title, String[] pages) {
      ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
      CompoundTag tag = book.getOrCreateTag();
      tag.putString("title", title);
      tag.putString("author", AUTHOR);
      tag.putByte("resolved", (byte) 1);

      ListTag list = new ListTag();
      for (String page : pages) {
         list.add(StringTag.valueOf(page));
      }

      tag.put("pages", list);
      book.setTag(tag);
      return book;
   }

   private static String[] manualPages() {
      return new String[]{
         "§6§l光影性能核心§r\n§0GlowPerf 手册 Manual§r\n§0v2.1.0 · MC 1.20.1§r\n§7中 / CN§r\n§0全部功能走指令，无按键§r\n§b/perf help§r\n§b/gp help§r\n§7EN§r\n§0Commands only, no keys.§r\n§b/perf help§r\n§b/gp help§r",

         "§6§l小地图 Minimap§r\n§7中 / CN§r\n§b/open the map§0 打开§r\n§b/close the map§0 关闭§r\n§b/minimap on|off§0 常驻§r\n§0Esc 或右上 x 关闭§r\n§0记住缩放与平移§r\n§7EN§r\n§b/open the map§0 open§r\n§b/close the map§0 close§r\n§b/minimap on|off§0 corner§r\n§0Esc or x closes it.§r",

         "§6§l地图按钮 1 Map§r\n§7中 / CN§r\n§b坐标§0 X Y Z 传送§r\n§b玩家§0 在线玩家传送§r\n§b路点§0 列表 / 删除§r\n§b洞穴§0 锁定洞穴模式§r\n§b立体§0 立体着色§r\n§7EN§r\n§bXYZ§0 teleport by coords§r\n§bPlayer§0 player teleport§r\n§bWaypoint§0 list / delete§r\n§bCave§0 lock cave mode§r\n§b3D§0 stereo shading§r",

         "§6§l地图按钮 2 Map§r\n§7中 / CN§r\n§b标记§0 实体标记开关§r\n§b缩小§0 缩小地图§r\n§b放大§0 放大地图§r\n§b回中§0 回到玩家§r\n§0右上 x 关闭地图§r\n§7EN§r\n§bMarkers§0 toggle markers§r\n§bZoom-§0 zoom out§r\n§bZoom+§0 zoom in§r\n§bCenter§0 back to player§r\n§0x at top right closes§r",

         "§6§l地图操作 Controls§r\n§7中 / CN§r\n§b左键§0 点击传送§r\n§b右键§0 添加路点§r\n§b拖动§0 平移地图§r\n§b滚轮§0 缩放 0.5~16§r\n§0悬停看坐标 距离 区块 群系§r\n§7EN§r\n§bLeft click§0 teleport§r\n§bRight click§0 waypoint§r\n§bDrag§0 pan the map§r\n§bWheel§0 zoom 0.5-16§r",

         "§6§l标记与洞穴§r\n§0Markers & cave§r\n§7中 / CN§r\n§6橙§0 掉落物§r\n§5紫§0 刷怪笼§r\n§c红§0 敌对§r\n§a绿§0 友好§r\n§0Y<0 自动洞穴，Y≥0 恢复§r\n§7EN§r\n§6Orange§0 items§r\n§5Purple§0 spawner§r\n§cRed§0 hostile§r\n§aGreen§0 passive§r\n§0Y<0 cave mode, auto restore§r",

         "§6§l区块与高度§r\n§0Chunks & height§r\n§7中 / CN§r\n§0地图按 16×16 区块铺开§r\n§0细线为区块边界§r\n§0放大 6 倍显示区块坐标§r\n§0顶部显示维度 坐标 Y 群系§r\n§7EN§r\n§0Map is drawn in 16x16 chunks.§r\n§0Thin lines are borders.§r\n§0Zoom 6x+ shows chunk IDs.§r\n§0Header: dimension, pos, Y,§r\n§0biome.§r",

         "§6§l路点 Waypoints§r\n§7中 / CN§r\n§b/wp add 名字§r\n§b/wp list§r\n§b/wp tp 名字§r\n§b/wp del 名字§r\n§0最多 64 个，自动保存§r\n§7EN§r\n§b/wp add name§0 create§r\n§b/wp list§0 list§r\n§b/wp tp name§0 teleport§r\n§b/wp del name§0 delete§r\n§0Up to 64, auto saved.§r",

         "§6§l光影 Glow§r\n§7中 / CN§r\n§0默认开启§r\n§b/light and shadow§r\n§0 open 开 · off 关§r\n§0 auto 自动§r\n§0 fps on|off 帧率§r\n§0 settings 设置§r\n§7EN§r\n§0On by default.§r\n§b/light and shadow§r\n§0 open · off · auto§r\n§0 fps on|off · settings§r",

         "§6§l性能档位 Presets§r\n§7中 / CN§r\n§b/performance optimization§r\n§0 off 关闭§r\n§0 mild 轻度§r\n§0 moderate 中度§r\n§0 severe 重度§r\n§0 t matter 无所谓§r\n§0 auto 自动§r\n§7EN§r\n§0Strength: t matter > severe§r\n§0> moderate > mild > off.§r\n§0auto follows frame time.§r",

         "§6§l面板 诊断 HUD§r\n§7中 / CN§r\n§b/perf help§0 指令§r\n§b/perf settings§0 面板§r\n§b/perf on|off§0 总开关§r\n§b/perf save§0 保存§r\n§b/gp scan§0 体检§r\n§b/gp top§0 来源§r\n§b/gp slow§0 长帧§r\n§7EN§r\n§bhelp / settings / save§r\n§bscan / top / slow§0 diagnostics§r",

         "§6§l面板位置编辑§r\n§0HUD editor§r\n§7中 / CN§r\n§b/gp posedit§0 进入编辑§r\n§0拖动面板到目标位置§r\n§c✕§0 取消还原§r\n§a✓§0 保存位置§r\n§7EN§r\n§b/gp posedit§0 open editor§r\n§0Drag any HUD element.§r\n§c✕§0 cancel§r\n§a✓§0 save§r",

         "§6§l传送 作弊§r\n§0Teleport§r\n§7中 / CN§r\n§0不校验模组权限§r\n§0执行原版 /tp§r\n§0单人：允许作弊§r\n§0已有存档：对局域网开放§r\n§0服务器：需要 OP§r\n§7EN§r\n§0Vanilla /tp is used.§r\n§0Single player: allow cheats.§r\n§0Server: OP required.§r",

         "§6§l祝你游戏愉快§r\n§0Have fun!§r\n§7中 / CN§r\n§0问题反馈邮箱见更新日志首页§r\n§0更新内容见另一本书§r\n§7EN§r\n§0Contact emails are on page 1§r\n§0of the changelog book.§r\n§0Update notes are in that book.§r"
      };
   }

   private static String[] changelogPages() {
      return new String[]{
         "§6§l更新日志§r\n§0Changelog§r\n§0有问题请联系 Issues:§r\n§bmc_xiaopjingcai@outlook.com§r\n§0或 / or§r\n§bmc_xiaopjingcai@hotmail.com§r\n§e请翻下一页才是更新日志§r\n§eTurn the page for the log§r",

         "§6§l2026-11-13§r\n§0v2.1.0§r\n§7中 / CN§r\n§0支持 Forge NeoForge§r\n§0Fabric Quilt 四种加载器§r\n§0每个加载器单独 jar§r\n§0jar 名带 MC 版本§r\n§0两本书改为双语§r\n§7EN§r\n§0Four loaders supported, one§r\n§0jar each. Jar names include§r\n§0the MC version. Books are§r\n§0bilingual.§r",

         "§6§l2026-11-12§r\n§0v2.1.0§r\n§7中 / CN§r\n§0新增小地图与路点§r\n§0新增 HUD 位置编辑§r\n§0光影默认开启§r\n§0新增性能档位指令§r\n§0删除旧光影性能指令§r\n§0手册扩写并加颜色§r\n§7EN§r\n§0Minimap, waypoints, HUD§r\n§0editor. Glow on by default.§r\n§0Performance presets. Legacy§r\n§0commands removed.§r",

         "§6§l2026-11-10§r\n§0v2.1.0§r\n§7中 / CN§r\n§0首个版本§r\n§0物品光影与 Bloom§r\n§0实体与玩家剔除§r\n§0新生物生成护栏§r\n§0闲置 AI 冻结§r\n§0帧时间诊断§r\n§7EN§r\n§0First release: item glow,§r\n§0culling, spawn guard, idle§r\n§0AI freeze, frame time stats.§r"
      };
   }

   private static String[] developerPages() {
      return new String[]{
         "§6§l开发 构建§r\n§0Dev & Build§r\n§0源码仓库 Repo§r\n§bgithub.com/leittleP/§r\n§bglowperf-mod§r\n§7中 / CN§r\n§0需要 JDK 17§r\n§0Gradle Wrapper 8.10.2§r\n§7EN§r\n§0JDK 17 required.§r\n§0Gradle wrapper 8.10.2.§r",

         "§6§l构建命令§r\n§0Build commands§r\n§7中 / CN§r\n§b./gradlew :forge:build§r\n§b -Ploaders=forge§r\n§0依次构建四种加载器§r\n§0每次只构建一个§r\n§7EN§r\n§0Build one loader per run:§r\n§b-Ploaders=forge§r\n§0neoforge, fabric, quilt§r\n§0Forge and NeoForge must not§r\n§0share one invocation.§r",

         "§6§lCI 自动构建§r\n§0GitHub Actions§r\n§7中 / CN§r\n§0推送 v* 标签§r\n§0自动构建四个 jar§r\n§0并发布到 Release§r\n§0也可 workflow_dispatch§r\n§7EN§r\n§0Push a v* tag to build all§r\n§0four jars and publish the§r\n§0release. Manual runs use§r\n§0workflow_dispatch.§r",

         "§6§l模块结构§r\n§0Modules§r\n§7中 / CN§r\n§0common 公共逻辑§r\n§0forge neoforge FML§r\n§0fabric quilt Loom§r\n§0产物名带 mc1.20.1§r\n§7EN§r\n§0common: shared logic§r\n§0forge & neoforge: FML§r\n§0fabric & quilt: Loom§r\n§0Jar names include the MC§r\n§0version.§r"
      };
   }
}
