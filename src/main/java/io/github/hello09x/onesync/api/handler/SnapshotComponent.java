package io.github.hello09x.onesync.api.handler;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface SnapshotComponent {


    /**
     * 转换为箱子菜单物品
     *
     * @return 箱子菜单物品
     */
    @NotNull MenuItem[] toMenuItems(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> back);

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
