package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.repository.SnapshotRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static io.github.hello09x.bedrock.util.Components.noItalic;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class MenuManager {

    public final static MenuManager instance = new MenuManager();
    private final static Logger log = Main.getInstance().getLogger();
    private final SnapshotRepository repository = SnapshotRepository.instance;


    /**
     * 获取所有快照的菜单
     *
     * @param page   页码
     * @param player 玩家
     */
    public void openSnapshotPage(@NotNull Player viewer, int page, @NotNull OfflinePlayer player) {
        var p = repository.selectPageByPlayerId(page, 45, player.getUniqueId());

        var menu = Main.getMenuRegistry().createMenu(
                54,
                text(player.getName() + " 的快照 [%d]".formatted(page))
        );

        var snapshots = p.records();
        var itr = snapshots.listIterator();
        while (itr.hasNext()) {
            var i = itr.nextIndex();
            var snapshot = itr.next();
            Main.getMenuRegistry().setButton(
                    menu,
                    i,
                    snapshot.toMenuItem(),
                    event -> {
                        if (event.getClick() == ClickType.LEFT) {
                            // 左键打开详情
                            this.openSnapshot(viewer, snapshot.id(), x -> this.openSnapshotPage(viewer, page, player));
                        } else if (event.getClick() == ClickType.RIGHT) {
                            // 右键恢复
                            this.applySnapshot(viewer, snapshot.id());
                        }
                    });
        }

        if (page != 1) {
            Main.getMenuRegistry().setButton(
                    menu,
                    45,
                    Material.PAPER,
                    text("上一页"),
                    event -> this.openSnapshotPage(viewer, page - 1, player)
            );
        }

        if (p.pages() > page) {
            Main.getMenuRegistry().setButton(
                    menu,
                    53,
                    Material.PAPER,
                    text("下一页"),
                    event -> this.openSnapshotPage(viewer, page + 1, player)
            );
        }

        Main.getMenuRegistry().setButton(
                menu,
                49,
                Material.BARRIER,
                noItalic("关闭", RED),
                event -> viewer.closeInventory()
        );

        viewer.openInventory(menu);
    }

    /**
     * 获取快照菜单
     *
     * @param id ID
     */
    public void openSnapshot(
            @NotNull Player viewer,
            @NotNull Long id,
            @NotNull Consumer<InventoryClickEvent> back
    ) {
        var snapshot = repository.selectById(id);
        if (snapshot == null) {
            return;
        }
        var owner = Bukkit.getOfflinePlayer(snapshot.playerId());
        var menu = Main.getMenuRegistry().createMenu(54, text("%s 的快照 [%d]".formatted(owner.getName(), snapshot.id())));

        int i = 0;
        for (var service : SnapshotHandler.getImpl()) {
            var sc = service.getOne(snapshot.id());
            if (sc == null) {
                continue;
            }

            var item = sc.toMenuItem(viewer, event -> this.openSnapshot(viewer, id, back));
            Main.getMenuRegistry().setButton(menu, i++, item.item(), event -> {
                if (event.getClick() == ClickType.RIGHT) {
                    // 右键打开恢复界面
                    this.openSnapshotApplyConfirm(viewer, service, sc, x -> this.openSnapshot(viewer, id, back));
                } else if (item.onClick() != null) {
                    // 左键传递到物品的自定义点击事件
                    item.onClick().accept(event);
                }
            });
            if (i == 45) {
                break;
            }
        }

        Main.getMenuRegistry().setButton(menu, 49, Material.BARRIER, noItalic("返回"), back);

        viewer.openInventory(menu);
    }

    /**
     * 打开确认恢复快照某一项数据的页面
     *
     * @param viewer   查看者
     * @param handler  快照处理器
     * @param snapshot 快照
     * @param back     返回上一页
     */
    public <T extends SnapshotComponent> void openSnapshotApplyConfirm(
            @NotNull Player viewer,
            @NotNull SnapshotHandler<T> handler,
            @NotNull T snapshot,
            @NotNull Consumer<InventoryClickEvent> back
    ) {
        var menu = Main.getMenuRegistry().createMenu(54, text("确认恢复?"));
        Stream.of(12, 13, 14, 21, 23, 30, 31, 32).forEach(slot -> Main.getMenuRegistry().setButton(
                menu,
                slot,
                Material.BLACK_STAINED_GLASS_PANE,
                empty(),
                null
        ));

        Main.getMenuRegistry().setButton(menu, 22, Material.GREEN_STAINED_GLASS_PANE, noItalic("确定", GREEN), event -> {
            try {
                var owner = snapshot.owner().getPlayer();
                if (owner == null) {
                    viewer.sendMessage(text("该玩家不在线", RED));
                    return;
                }

                try {
                    handler.apply(owner, snapshot, true);
                    viewer.sendMessage(text("恢复数据成功", GRAY));
                } catch (Throwable e) {
                    log.severe(Throwables.getStackTraceAsString(e));
                    viewer.sendMessage(text("恢复数据失败: " + e.getMessage()));
                }
            } finally {
                viewer.closeInventory();
            }
        });

        Main.getMenuRegistry().setButton(menu, 49, Material.BARRIER, noItalic("取消"), back);
        viewer.openInventory(menu);
    }

    public void applySnapshot(@NotNull Player viewer, @NotNull Long id) {
        var snapshot = repository.selectById(id);
        if (snapshot == null) {
            viewer.sendMessage(text("该快照不存在", RED));
            viewer.closeInventory();
            return;
        }

        var owner = Bukkit.getPlayer(snapshot.playerId());
        if (owner == null) {
            viewer.sendMessage(text("该玩家不在线", RED));
            viewer.closeInventory();
            return;
        }

        for (var handler : SnapshotHandler.getImpl()) {
            var s = handler.getOne(id);
            if (s == null) {
                continue;
            }

            try {
                handler.applyUnsafe(owner, s);
                viewer.sendMessage(text("恢复 %s 的「%s」数据成功".formatted(owner.getName(), handler.snapshotType())));
            } catch (Throwable e) {
                log.severe(Throwables.getStackTraceAsString(e));
                viewer.sendMessage(text("恢复 %s 的「%s」数据失败: %s".formatted(owner.getName(), handler.snapshotType(), e.getMessage()), RED));
            }
        }

        viewer.closeInventory();
    }


}
