package io.github.hello09x.onesync.manager.synchronize;

import com.google.common.base.Throwables;
import io.github.hello09x.bedrock.menu.ChestMenuBuilder;
import io.github.hello09x.bedrock.util.KeyBinds;
import io.github.hello09x.bedrock.util.Lores;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.repository.SnapshotRepository;
import io.github.hello09x.onesync.repository.model.Snapshot;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static io.github.hello09x.bedrock.util.Components.noItalic;
import static net.kyori.adventure.text.Component.*;
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

        var menu = Main.getChestMenuRegistry()
                .builder()
                .title(text(player.getName() + " 的快照 [%d]".formatted(page)))
                .size(54)
                .onClickOutside(event -> viewer.closeInventory());

        Consumer<InventoryClickEvent> here = x -> this.openSnapshotPage(viewer, page, player);
        var snapshots = p.records();
        var itr = snapshots.listIterator();
        while (itr.hasNext()) {
            var i = itr.nextIndex();
            var snapshot = itr.next();
            menu.onClickButton(i, snapshot.toMenuItem(), event -> {
                switch (event.getClick()) {
                    // 左键打开详情
                    case LEFT -> this.openSnapshot(
                            viewer,
                            snapshot.id(),
                            here
                    );
                    // 右键打开确认恢复界面
                    case RIGHT -> this.openConfirm(
                            viewer,
                            text("确认恢复?"),
                            () -> this.applySnapshot(viewer, snapshot),
                            here
                    );
                    // Q 删除快照
                    case DROP -> this.openConfirm(
                            viewer,
                            text("确认删除?"),
                            () -> this.removeSnapshot(viewer, snapshot),
                            here
                    );
                }
            });

            if (page > 1) {
                menu.onClickButton(
                        45,
                        Material.PAPER,
                        text("上一页"),
                        event -> this.openSnapshotPage(viewer, page - 1, player)
                );
            }

            if (p.pages() > page) {
                menu.onClickButton(
                        53,
                        Material.PAPER,
                        text("下一页"),
                        event -> this.openSnapshotPage(viewer, page + 1, player)
                );
            }

            viewer.openInventory(menu.build());
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
            @NotNull Consumer<InventoryClickEvent> onClickOutside
    ) {
        var header = repository.selectById(id);
        if (header == null) {
            return;
        }
        var owner = Bukkit.getOfflinePlayer(header.playerId());
        var menu = Main.getChestMenuRegistry()
                .builder()
                .title(text("%s 的快照 #%d".formatted(owner.getName(), header.id())))
                .onClickOutside(onClickOutside);

        Runnable here0 = () -> this.openSnapshot(viewer, id, onClickOutside);
        Consumer<InventoryClickEvent> here = x -> here0.run();
        int i = 0;
        for (var handler : SnapshotHandler.getImpl()) {
            var snapshot = handler.getOne(header.id());
            if (snapshot == null) {
                continue;
            }

            var button = snapshot.toMenuItem(viewer, here);
            Lores.append(button.item(), List.of(
                    noItalic("「右键」恢复", GRAY),
                    noItalic(textOfChildren(text("「"), keybind(KeyBinds.DROP), text(" 键」删除"))).color(GRAY)
            ));
            menu.onClickButton(i++, button.item(), event -> {
                switch (event.getClick()) {
                    // 右键恢复
                    case RIGHT -> this.openConfirm(
                            viewer,
                            text("确认恢复「%s」?".formatted(handler.snapshotType())),
                            () -> applySnapshot(viewer, handler, snapshot),
                            here
                    );
                    // Q 删除
                    case DROP -> this.openConfirm(
                            viewer,
                            text("确认删除「%s」?".formatted(handler.snapshotType()), RED),
                            () -> removeSnapshot(viewer, handler, id, here0),
                            here
                    );
                    // 传递给按钮自定义事件
                    default -> {
                        var onClick = button.onClick();
                        if (onClick != null) {
                            onClick.accept(event);
                        }
                    }
                }
            });
        }

        viewer.openInventory(menu.build());
    }

    /**
     * 打开确认恢复界面
     *
     * @param viewer         操作者
     * @param onConfirm      确认操作函数
     * @param onClickOutside 取消操作函数
     */
    public void openConfirm(@NotNull Player viewer, @NotNull Component title, @NotNull Runnable onConfirm, @NotNull Consumer<InventoryClickEvent> onClickOutside) {
        var menu = Main.getChestMenuRegistry()
                .builder()
                .title(title)
                .size(45)
                .onClickOutside(onClickOutside);
        menu.onClickButton(22, Material.GREEN_STAINED_GLASS_PANE, noItalic("确定", GREEN), event -> onConfirm.run());
        Stream.of(12, 13, 14, 21, 23, 30, 31, 32)
                .forEach(slot -> menu.onClickButton(slot, Material.BLACK_STAINED_GLASS_PANE, empty(), ChestMenuBuilder.ignore()));
        viewer.openInventory(menu.build());
    }

    /**
     * 删除快照某项数据
     *
     * @param viewer     操作者
     * @param handler    快照处理器
     * @param snapshotId 快照 ID
     */
    public <T extends SnapshotComponent> void removeSnapshot(
            @NotNull Player viewer,
            @NotNull SnapshotHandler<T> handler,
            @NotNull Long snapshotId,
            @NotNull Runnable onFinish
    ) {
        try {
            handler.remove(Collections.singletonList(snapshotId));
        } catch (Throwable e) {
            log.severe(Throwables.getStackTraceAsString(e));
            viewer.closeInventory();
            viewer.sendMessage(text("删除失败: " + e.getMessage()));
            return;
        }

        onFinish.run();
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
                    handler.apply(owner, s);
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

    /**
     * 删除快照
     *
     * @param viewer   操作者
     * @param snapshot 快照
     */
    public void removeSnapshot(@NotNull Player viewer, @NotNull Snapshot snapshot) {
        var snapshotId = snapshot.id();
        var owner = Bukkit.getOfflinePlayer(snapshot.playerId());
        try {
            var snapshotIds = Collections.singletonList(snapshotId);
            repository.deleteById(snapshotId);
            for (var handler : SnapshotHandler.getImpl()) {
                try {
                    handler.remove(snapshotIds);
                    viewer.sendMessage(text("删除 %s 的「%s」数据成功".formatted(owner.getName(), handler.snapshotType())));
                } catch (Throwable e) {
                    log.severe(Throwables.getStackTraceAsString(e));
                    viewer.sendMessage(text("删除 %s 的「%s」数据失败".formatted(owner.getName(), handler.snapshotType())));
                }
            }
        } finally {
            viewer.closeInventory();
        }
    }

}
