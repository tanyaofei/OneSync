package io.github.hello09x.onesync.repository.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.RowMapper;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.manager.synchronize.handler.InventorySnapshotHandler;
import io.github.hello09x.onesync.util.ItemStackCodec;
import io.github.hello09x.onesync.util.MenuTemplate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static io.github.hello09x.devtools.core.utils.ComponentUtils.noItalic;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * @param snapshotId 快照 ID
 * @param playerId   玩家 ID
 * @param items      背包物品
 */
public record InventorySnapshot(

        Long snapshotId,

        @NotNull
        UUID playerId,

        @NotNull
        Map<Integer, ItemStack> items,

        @NotNull
        Integer heldItemSlot


) implements SnapshotComponent {

    @Override
    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> prevMenu) {
        var item = new ItemStack(Material.CHEST);
        item.editMeta(meta -> {
            meta.displayName(noItalic(text("背包", GOLD)));
            meta.lore(List.of(
                    noItalic(textOfChildren(text("物品: ", GRAY), text(this.items.size(), WHITE))),
                    empty(),
                    noItalic(text("「左键」查看详情", GRAY))
            ));
        });

        var modified = new MutableBoolean();
        return new MenuItem(
                item,
                x -> MenuTemplate.openInventoryMenu(
                        viewer,
                        text("背包"),
                        InventoryType.PLAYER,
                        this.items,
                        newItems -> Main.getInjector().getInstance(InventorySnapshotHandler.class).updateItems(this.snapshotId, newItems),
                        event -> {
                            if (modified.booleanValue()) {
                                // 先关闭保存数据再打开加载数据
                                event.getWhoClicked().closeInventory();
                            }
                            prevMenu.accept(event);
                        },
                        modified
                ));
    }


    @Singleton
    public static class InventorySnapshotRowMapper implements RowMapper<InventorySnapshot> {

        private final ItemStackCodec itemStackCodec;

        @Inject
        public InventorySnapshotRowMapper(ItemStackCodec itemStackCodec) {
            this.itemStackCodec = itemStackCodec;
        }

        @Override
        public @Nullable InventorySnapshot mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            return new InventorySnapshot(
                    rs.getLong("snapshot_id"),
                    UUID.fromString(rs.getString("player_id")),
                    itemStackCodec.deserialize(rs.getString("items")),
                    rs.getObject("held_item_slot", Integer.class)
            );
        }
    }
}
