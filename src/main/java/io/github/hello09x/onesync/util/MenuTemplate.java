package io.github.hello09x.onesync.util;

import io.github.hello09x.bedrock.util.InventoryUtils;
import io.github.hello09x.onesync.Main;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class MenuTemplate {

    /**
     * 打开箱子菜单
     *
     * @param viewer         操作者
     * @param title          标题
     * @param size           容器原始大小
     * @param items          容器内容
     * @param updateItems    更新回调
     * @param onClickOutside 点击外部回调
     * @param modified       由调用方提供的一个可变 {@code boolean} 变量, 如果操作了这个容器, 则会设置为 {@code true}
     */
    public static void openInventoryMenu(
            @NotNull Player viewer,
            @NotNull Component title,
            int size,
            @NotNull Map<Integer, ItemStack> items,
            @NotNull Consumer<Map<Integer, ItemStack>> updateItems,
            @NotNull Consumer<InventoryClickEvent> onClickOutside,
            @NotNull MutableBoolean modified
    ) {
        int fixedSize;
        if (size % 9 != 0) {
            fixedSize = size / 9 * 9 + 9;
        } else {
            fixedSize = size;
        }

        var menu = Main.getChestMenuRegistry()
                .builder()
                .title(title)
                .size(fixedSize)
                .onClickOutside(onClickOutside)
                .onClose(event -> {
                    if (modified.booleanValue()) {
                        var newItems = InventoryUtils.toMap(event.getInventory());
                        updateItems.accept(newItems);
                    }
                });

        // 点击下方容器事件
        var onClickBottom = (Consumer<InventoryClickEvent>) event -> {
            var item = event.getCurrentItem();
            if (item == null) {
                return;
            }
            if (event.getClick() != ClickType.LEFT) {
                return;
            }

            var top = event.getView().getTopInventory();
            var slot = top.firstEmpty();
            if (slot == -1 || slot >= size) {
                return;
            }

            top.setItem(slot, item);
            modified.setValue(true);
        };
        menu.onClickBottom(onClickBottom);

        // 点击上方容器事件
        var onClickButton = (Consumer<InventoryClickEvent>) event -> {
            var item = event.getCurrentItem();
            if (item == null) {
                return;
            }

            switch (event.getClick()) {
                case LEFT -> {
                    // 左键复制物品
                    var bottom = event.getView().getBottomInventory();
                    var slot = bottom.firstEmpty();
                    if (slot == -1) {
                        return;
                    }
                    bottom.setItem(slot, event.getCurrentItem());
                }
                case DROP -> {
                    // Q 键删除物品
                    item.setAmount(0);
                    modified.setValue(true);
                }
            }
        };

        for (int i = fixedSize - 1; i >= 0; i--) {
            var item = Optional.ofNullable(items.get(i)).orElseGet(() -> new ItemStack(Material.AIR));
            menu.onClickButton(i, item, onClickButton);
        }

        viewer.openInventory(menu.build());
    }


}
