package io.github.hello09x.onesync.util;

import io.github.hello09x.devtools.core.utils.InventoryUtils;
import io.github.hello09x.devtools.core.utils.Mth;
import io.github.hello09x.devtools.core.utils.SingletonSupplier;
import io.github.hello09x.devtools.menu.ChestMenuRegistry;
import io.github.hello09x.onesync.OneSync;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MenuTemplate {

    private final static Supplier<ChestMenuRegistry> cmr = new SingletonSupplier<>(() -> OneSync.getInjector().getInstance(ChestMenuRegistry.class));

    /**
     * 打开箱子菜单
     *
     * @param viewer         操作者
     * @param title          标题
     * @param type           容器类型
     * @param items          容器内容
     * @param updateItems    更新回调
     * @param onClickOutside 点击外部回调
     * @param modified       由调用方提供的一个可变 {@code boolean} 变量, 如果操作了这个容器, 则会设置为 {@code true}
     */
    public static void openInventoryMenu(
            @NotNull Player viewer,
            @NotNull Component title,
            InventoryType type,
            @NotNull Map<Integer, ItemStack> items,
            @NotNull Consumer<Map<Integer, ItemStack>> updateItems,
            @NotNull Consumer<InventoryClickEvent> onClickOutside,
            @NotNull MutableBoolean modified
    ) {
        var size = type.getDefaultSize();
        int alignedSize = Mth.align(size, 9);

        var menu = cmr.get()
                .builder()
                .title(title)
                .size(alignedSize)
                .onClickOutside(onClickOutside)
                .onClose(event -> {
                    if (modified.booleanValue()) {
                        var newItems = InventoryUtils.toMap(event.getInventory());
                        updateItems.accept(newItems);
                    }
                });

        // 点击下方容器事件
        var onClickBottom = (Consumer<InventoryClickEvent>) event -> {
            switch (event.getClick()) {
                case LEFT -> {
                    // 左键将下方容器物品移动到上方容器里
                    if (InventoryUtils.moveItem(
                            event.getView().getBottomInventory(),
                            event.getSlot(),
                            event.getView().getTopInventory())
                    ) {
                        modified.setValue(true);
                    }
                }
                case MIDDLE -> {
                    if (InventoryUtils.copyItem(
                            event.getView().getBottomInventory(),
                            event.getSlot(),
                            event.getView().getTopInventory())
                    ) {
                        modified.setValue(true);
                    }
                }
            }
        };
        menu.onClickBottom(onClickBottom);

        // 点击上方容器事件
        var onClickButton = (Consumer<InventoryClickEvent>) event -> {
            switch (event.getClick()) {
                case LEFT -> {
                    // 左键将上方容器物品移动到下方容器里
                    if (InventoryUtils.moveItem(
                            event.getView().getTopInventory(),
                            event.getSlot(),
                            event.getView().getBottomInventory())
                    ) {
                        modified.setValue(true);
                    }
                }
                case MIDDLE -> {
                    // 中键将上方容器物品复制到下方容器
                    InventoryUtils.copyItem(
                            event.getView().getTopInventory(),
                            event.getSlot(),
                            event.getView().getBottomInventory()
                    );
                }
            }
        };
        for (int i = alignedSize - 1; i >= 0; i--) {
            var item = Optional.ofNullable(items.get(i)).orElseGet(() -> new ItemStack(Material.AIR));
            menu.onClickButton(i, item, onClickButton);
        }

        viewer.openInventory(menu.build());
    }


}
