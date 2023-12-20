package io.github.hello09x.onesync.manager;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.repository.SnapshotRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.text;

public class MenuManager {

    public final static MenuManager instance = new MenuManager();

    private final SnapshotRepository repository = SnapshotRepository.instance;

    /**
     * 获取所有快照的菜单
     *
     * @param page   页码
     * @param player 玩家
     */
    public void openSnapshotPage(@NotNull Player sender, int page, @NotNull OfflinePlayer player) {
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
                    event -> this.openSnapshot(sender, snapshot.id(), x -> this.openSnapshotPage(sender, page, player)));
        }

        if (page != 1) {
            Main.getMenuRegistry().setButton(
                    menu,
                    45,
                    Material.PAPER,
                    text("上一页"),
                    event -> this.openSnapshotPage(sender, page - 1, player)
            );
        }

        if (p.pages() > page) {
            Main.getMenuRegistry().setButton(
                    menu,
                    53,
                    Material.PAPER,
                    text("下一页"),
                    event -> this.openSnapshotPage(sender, page + 1, player)
            );
        }

        Main.getMenuRegistry().setButton(
                menu,
                49,
                Material.BARRIER,
                text("关闭"),
                event -> sender.closeInventory()
        );

        sender.openInventory(menu);
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

        var components = new ArrayList<SnapshotComponent.MenuItem>();
        for (var handler : SnapshotHandler.HANDLERS) {
            if (!handler.plugin().isEnabled()) {
                continue;
            }

            var component = handler.getOne(snapshot.id());
            if (component == null) {
                continue;
            }

            var items = component.toMenuItems(viewer, event -> this.openSnapshot(viewer, id, back));
            components.addAll(Arrays.asList(items));
        }

        var itr = components.listIterator();
        while (itr.hasNext() && itr.nextIndex() < 45) {
            var i = itr.nextIndex();
            var item = itr.next();
            Main.getMenuRegistry().setButton(menu, i, item.item(), item.onClick());
        }
        Main.getMenuRegistry().setButton(menu, 49, Material.BARRIER, text("返回"), back);

        viewer.openInventory(menu);
    }


}
