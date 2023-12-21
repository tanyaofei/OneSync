package io.github.hello09x.onesync.repository.model;

import com.google.common.base.Throwables;
import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.handler.InventorySnapshotHandler;
import io.github.hello09x.onesync.util.ItemStackMapTypeHandler;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static io.github.hello09x.bedrock.util.Components.noItalic;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * @param snapshotId 快照 ID
 * @param playerId   玩家 ID
 * @param items      背包物品
 * @param enderItems 末影响物品
 */
@Table("inventory_snapshot")
public record InventorySnapshot(

        @TableId("snapshot_id")
        Long snapshotId,

        @TableField("player_id")
        UUID playerId,

        @TableField(value = "items", typeHandler = ItemStackMapTypeHandler.class)
        Map<Integer, ItemStack> items,

        @TableField(value = "ender_items", typeHandler = ItemStackMapTypeHandler.class)
        Map<Integer, ItemStack> enderItems,

        @TableField(value = "held_item_slot")
        Integer heldItemSlot


) implements SnapshotComponent {

    private final static Logger log = Main.getInstance().getLogger();

    @Override
    public @NotNull MenuItem[] toMenuItems(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> back) {
        var inventory = new ItemStack(Material.CHEST);
        inventory.editMeta(meta -> {
            meta.displayName(noItalic("背包"));
            meta.lore(List.of(
                    noItalic(textOfChildren(text("物品: ", GRAY), text(this.items.size(), WHITE))),
                            empty(),
                            noItalic("「左键」查看详情", GRAY),
                            noItalic("「右键」恢复数据", GRAY)
                    )
            );
        });
        var first = this.toMenuItem(viewer, inventory, InventoryType.INVENTORY, back);

        var enderChest = new ItemStack(Material.ENDER_CHEST);
        enderChest.editMeta(meta -> {
            meta.displayName(noItalic("末影箱"));
            meta.lore(List.of(
                    noItalic(textOfChildren(text("物品: ", GRAY), text(this.enderItems.size(), WHITE))),
                    empty(),
                    noItalic("「左键」查看详情", GRAY),
                    noItalic("「右键」恢复数据", GRAY)
            ));
        });
        var second = this.toMenuItem(viewer, enderChest, InventoryType.ENDER_CHEST, back);

        return new MenuItem[]{first, second};
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private enum InventoryType {

        INVENTORY("背包"),

        ENDER_CHEST("末影箱");


        final String displayName;

    }

    private @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull ItemStack button, InventoryType type, @NotNull Consumer<InventoryClickEvent> back) {
        Consumer<InventoryClickEvent> onClick = e -> {
            var item = e.getCurrentItem();
            if (item == null) {
                return;
            }
            viewer.getInventory().addItem(item);
        };

        return new MenuItem(button, event -> {
            if (event.getClick() == ClickType.LEFT) {
                // 左键查看详情
                var menu = Main.getMenuRegistry().createMenu(54, text(type.displayName));

                var items = switch (type) {
                    case INVENTORY -> this.items;
                    case ENDER_CHEST -> this.enderItems;
                };
                for (var entry : items.entrySet()) {
                    Main.getMenuRegistry().setButton(menu, entry.getKey(), entry.getValue(), onClick);
                }
                Main.getMenuRegistry().setButton(menu, 49, Material.BARRIER, text("返回"), back);
                viewer.openInventory(menu);
            } else if (event.getClick() == ClickType.RIGHT) {
                // 右键恢复数据
                var player = Bukkit.getPlayer(this.playerId);
                if (player == null) {
                    viewer.sendMessage(text("该玩家不在线", RED));
                    viewer.closeInventory();
                    return;
                }

                try {
                    switch (type) {
                        case INVENTORY -> InventorySnapshotHandler.instance.applyInventory(player, this, true);
                        case ENDER_CHEST -> InventorySnapshotHandler.instance.applyEnderChest(player, this, true);
                        default -> throw new UnsupportedOperationException("Unsupported type: " + type.name());
                    }
                } catch (Throwable e) {
                    log.severe(Throwables.getStackTraceAsString(e));
                    viewer.sendMessage(text("恢复数据失败", RED));
                    viewer.closeInventory();
                    return;
                }

                viewer.sendMessage(textOfChildren(
                        text("为 ", GRAY),
                        text(player.getName(), WHITE),
                        text(" 恢复数据成功", GRAY)
                ));
                viewer.closeInventory();
            }
        });
    }
}
