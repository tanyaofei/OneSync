package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.repository.SnapshotRepository;
import io.github.hello09x.onesync.repository.model.Snapshot;
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
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

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

        var menu = Main.getChestMenuRegistry().createMenu(
                54,
                text(player.getName() + " 的快照 [%d]".formatted(page)),
                event -> viewer.closeInventory()
        );

        var snapshots = p.records();
        var itr = snapshots.listIterator();
        while (itr.hasNext()) {
            var i = itr.nextIndex();
            var snapshot = itr.next();
            menu.setButton(i, snapshot.toMenuItem(), event -> {
                if (event.getClick() == ClickType.LEFT) {
                    // 左键打开详情
                    this.openSnapshot(viewer, snapshot.id(), x -> this.openSnapshotPage(viewer, page, player));
                } else if (event.getClick() == ClickType.RIGHT) {
                    // 右键恢复
                    this.openApplyConfirm(viewer, () -> this.applySnapshot(viewer, snapshot), x -> this.openSnapshotPage(viewer, page, player));
                }
            });

            if (page != 1) {
                menu.setButton(
                        45,
                        Material.PAPER,
                        text("上一页"),
                        event -> this.openSnapshotPage(viewer, page - 1, player)
                );
            }

            if (p.pages() > page) {
                menu.setButton(
                        53,
                        Material.PAPER,
                        text("下一页"),
                        event -> this.openSnapshotPage(viewer, page + 1, player)
                );
            }

            viewer.openInventory(menu.getInventory());
        }
    }

    /**
     * 获取快照菜单
     *
     * @param id ID
     */
    public void openSnapshot(
            @NotNull Player viewer,
            @NotNull Long id,
            @NotNull Consumer<InventoryClickEvent> onCancel
    ) {
        var header = repository.selectById(id);
        if (header == null) {
            return;
        }
        var owner = Bukkit.getOfflinePlayer(header.playerId());
        var menu = Main.getChestMenuRegistry().createMenu(54, text("%s 的快照 [%d]".formatted(owner.getName(), header.id())), onCancel);

        int i = 0;
        for (var handler : SnapshotHandler.getImpl()) {
            var snapshot = handler.getOne(header.id());
            if (snapshot == null) {
                continue;
            }

            var item = snapshot.toMenuItem(viewer, event -> this.openSnapshot(viewer, id, onCancel));
            menu.setButton(i++, item.item(), event -> {
                if (event.getClick() == ClickType.RIGHT) {
                    // 右键打开恢复界面
                    this.openApplyConfirm(viewer, () -> applySnapshot(viewer, handler, snapshot), x -> this.openSnapshot(viewer, id, onCancel));
                } else if (item.onClick() != null) {
                    // 左键传递到物品的自定义点击事件
                    item.onClick().accept(event);
                }
            });
            if (i == 53) {
                break;
            }
        }

        viewer.openInventory(menu.getInventory());
    }

    /**
     * 打开确认恢复界面
     *
     * @param viewer    操作者
     * @param onConfirm 确认操作函数
     * @param onCancel  取消操作函数
     */
    public void openApplyConfirm(@NotNull Player viewer, @NotNull Runnable onConfirm, @NotNull Consumer<InventoryClickEvent> onCancel) {
        var menu = Main.getChestMenuRegistry().createMenu(45, text("确认恢复?"), onCancel);
        menu.setButton(22, Material.GREEN_STAINED_GLASS_PANE, noItalic("确定", GREEN), event -> onConfirm.run());
        Stream.of(12, 13, 14, 21, 23, 30, 31, 32)
                .forEach(slot -> menu.setButton(slot, Material.BLACK_STAINED_GLASS_PANE, empty(), null));
        viewer.openInventory(menu.getInventory());
    }

    /**
     * 恢复快照某项数据
     *
     * @param viewer   操作者
     * @param handler  快照处理器
     * @param snapshot 快照
     */
    public <T extends SnapshotComponent> void applySnapshot(
            @NotNull Player viewer,
            @NotNull SnapshotHandler<T> handler,
            @NotNull T snapshot
    ) {
        try {
            var owner = snapshot.owner().getPlayer();
            if (owner == null) {
                viewer.sendMessage(text("该玩家不在线", RED));
                return;
            }
            try {
                handler.apply(owner, snapshot, true);
                viewer.sendMessage(text("恢复 %s 的「%s」数据成功".formatted(owner.getName(), handler.snapshotType())));
            } catch (Throwable e) {
                log.severe(Throwables.getStackTraceAsString(e));
                viewer.sendMessage(text("恢复 %s 的「%s」数据失败: %s".formatted(owner.getName(), handler.snapshotType(), e.getMessage()), RED));
            }
        } finally {
            viewer.closeInventory();
        }
    }

    /**
     * 恢复整份快照
     *
     * @param viewer   操作者
     * @param snapshot 快照
     */
    public void applySnapshot(@NotNull Player viewer, @NotNull Snapshot snapshot) {
        try {
            var owner = Bukkit.getPlayer(snapshot.playerId());
            if (owner == null) {
                viewer.sendMessage(text("该玩家不在线", RED));
                return;
            }

            for (var handler : SnapshotHandler.getImpl()) {
                var s = handler.getOne(snapshot.id());
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
        } finally {
            viewer.closeInventory();
        }

    }


}
