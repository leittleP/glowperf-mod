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
   private static final String BOOK_TITLE = "光影性能核心 · 手册";

   private MapEvents() {
   }

   public static void onLogin(Player player) {
      if (player != null) {
         UUID var2 = player.getUUID();
         if (!GIVEN.contains(var2)) {
            GIVEN.add(var2);

            try {
               removeOldBooks(player);
               ItemStack var3 = createBook();
               player.addItem(var3);
            } catch (Throwable var4) {
            }
         }
      }
   }

   /** 清掉背包里所有旧版/重复的手册，保证只留新发的一本。 */
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
      return tag != null && BOOK_TITLE.equals(tag.getString("title"));
   }

   private static ItemStack createBook() {
      ItemStack var0 = new ItemStack(Items.WRITTEN_BOOK);
      CompoundTag var1 = var0.getOrCreateTag();
      if (var1 == null) {
         var1 = new CompoundTag();
      }

      var1.putString("title", BOOK_TITLE);
      var1.putString("author", "GlowPerf");
      var1.putByte("resolved", (byte)1);
      ListTag var2 = new ListTag();

      for (String page : pages()) {
         var2.add(StringTag.valueOf(page));
      }
      var1.put("pages", var2);
      var0.setTag(var1);
      return var0;
   }

   private static String[] pages() {
      return new String[]{
         "§6§l光影性能核心 §r§7v2.1.0§r\n§7使用手册§r\n\n本书记录全部功能与指令\n\n优化档位\n§b/performance optimization§r\n  关闭·轻度·中度·重度\n  无所谓·自动\n光影 §b/light and shadow§r\n  open·off·auto\n§7下一页看详细说明§r",
         "§e小地图 · 打开与关闭§r\n\n§b/open the map§r\n  打开全屏小地图\n§b/close the map§r\n  关闭小地图\n\nEsc 或右上角 x 也可关闭\n\n会记住缩放与平移位置，\n下次打开从上次视角继续。",
         "§e地图按钮（3×3）§r\n\n§b坐标§r 输入 X/Y/Z 传送\n§b玩家§r 选择在线玩家传送\n§b路点§r 打开路点列表\n  点整行传送，点 x 删除\n§b洞穴§r 手动切换洞穴模式\n  再按一次回到自动\n§b立体§r 立体着色开关\n§b标记§r 实体标记总开关\n§b缩小/放大§r 缩放\n§b回中§r 视角回到玩家\n§7右上角 x = 关闭地图§r",
         "§e地图操作§r\n\n§b左键单击§r 传送到该点\n§b右键单击§r 在该点加路点\n§b按住拖动§r 平移地图\n§b滚轮§r 缩放 0.5~16 倍\n\n缩放用「缩小/放大」按钮，\n回中用「回中」按钮，\n路点用「路点」按钮。\n\n鼠标悬停可看：\nX/Z、距离、区块、群系\n右上角显示比例尺",
         "§e标记与洞穴模式§r\n\n实体标记颜色：\n§6橙 = 掉落物§r\n§5紫 = 刷怪笼§r\n§c红 = 敌对生物§r\n§a绿 = 友好生物§r\n\n§b洞穴模式§r（Y<0 自动）\n地图变暗，仍标记实体\n回到 Y≥0 自动恢复\n想锁定就按「洞穴」按钮",
         "§e区块与高度§r\n\n地图按 16×16 区块铺开，\n细线就是区块边界。\n\n放大到 §b6 倍以上§r 时，\n每个区块中心显示\n区块坐标，例如 §b3,-2§r\n\n顶部标题显示当前维度，\n第二行显示玩家坐标、\nY 与所在生物群系。",
         "§e常驻小地图§r\n\n§b/minimap§r 开关\n/minimap on|off|toggle\n\n左上角常驻俯视小图，\n显示维度、群系、Y、\n实体标记、路点、洞穴模式。\n\n位置大小用 §b/gp posedit§r\n拖动调整（map.hud*）",
         "§e路点 /wp§r\n\n§b/wp add <名字>§r\n  在当前位置添加路点\n§b/wp list§r 列出全部\n§b/wp tp <名字>§r 传送\n§b/wp del <名字>§r 删除\n\n右键地图可直接添加，\n同名路点会被覆盖。\n最多 64 个，自动保存。\n跨维度会提示先换维度。",
         "§e光影 /light and shadow§r\n\n§b/light and shadow open§r\n  开启（默认）\n§b/light and shadow off§r\n  关闭\n§b/light and shadow auto§r\n  按帧时间自动开关\n§b/light and shadow fps on|off§r\n  帧率面板\n§b/light and shadow settings§r\n  光影设置界面\n效果：物品光晕、掉落物描边。",
         "§e优化档位§r\n\n§b/performance optimization§r\n  §boff§r 关闭优化\n  §bmild§r 轻度\n  §bmoderate§r 中度\n  §bsevere§r 重度\n  §bt matter§r 无所谓\n  §bauto§r 自动\n强度：无所谓>重度>中度\n>轻度>关闭；自动按帧时间\n在区间内收放。",
         "§e综合 /gp§r\n\n§b/gp scan§r 一键体检\n§b/gp top§r 实体来源排名\n§b/gp slow§r 卡顿快照\n§b/gp posedit§r 调整面板位置\n\n§e面板 /perf§r\n§b/perf on|off§r 总开关\n§b/perf panel|coords on|off§r\n§b/perf compact on|off§r 紧凑\n§b/perf distance§r 16~256\n§b/perf settings§r 设置界面\n§b/perf save§r 写入配置文件",
         "§e传送说明§r\n\n模组不校验权限，直接执行\n原版 /tp，需允许传送。\n\n§b单人§r 创建时勾选允许作弊；\n已有存档：Esc → 对局域网\n开放 → 允许作弊。\n\n§b服务器§r 需要 OP 权限。\n\n§7祝你游戏愉快！§r"
      };
   }
}
