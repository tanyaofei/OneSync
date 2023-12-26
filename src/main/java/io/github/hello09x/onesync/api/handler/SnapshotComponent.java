package io.github.hello09x.onesync.api.handler;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface SnapshotComponent {

    /**
     * 返回快照 ID
     *
     * @return 快照 ID
     */
    @NotNull
    Long snapshotId();

    /**
     * @return 快照所属玩家
     */
    @NotNull OfflinePlayer owner();

    /**
     * 转换为箱子菜单物品
     *
     * @param viewer         操作者
     * @param onClickOutside 取消操作, 实现者如果需要创建新的箱子菜单, 可以将点击事件执行到这个函数来返回上一层页面
     * @return 箱子菜单物品
     */
    @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> onClickOutside);

    record MenuItem(

            @NotNull
            ItemStack item,
            @Nullable
            Consumer<InventoryClickEvent> onClick

    ) {

        public MenuItem(@NotNull ItemStack item) {
            this(item, null);
        }

    }


}
