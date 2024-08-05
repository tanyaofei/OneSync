package io.github.hello09x.onesync.repository.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.RowMapper;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.manager.synchronize.handler.EnderChestSnapshotHandler;
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
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import static io.github.hello09x.devtools.core.utils.ComponentUtils.noItalic;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public record EnderChestSnapshot(

        Long snapshotId,

        @NotNull
        UUID playerId,

        @NotNull
        Map<Integer, ItemStack> items

) implements SnapshotComponent {

    @Override
    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> prevMenu) {
        var item = new ItemStack(Material.ENDER_CHEST);
        item.editMeta(meta -> {
            meta.displayName(noItalic(text("末影箱", LIGHT_PURPLE)));
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
                        text("末影箱"),
                        InventoryType.ENDER_CHEST,
                        this.items,
                        newItems -> Main.getInjector().getInstance(EnderChestSnapshotHandler.class).updateItems(this.snapshotId, newItems),
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
    public static class EnderChestSnapshotRowMapper implements RowMapper<EnderChestSnapshot> {

        private final ItemStackCodec itemStackCodec;

        @Inject
        public EnderChestSnapshotRowMapper(ItemStackCodec itemStackCodec) {
            this.itemStackCodec = itemStackCodec;
        }

        @Override
        public @Nullable EnderChestSnapshot mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            return new EnderChestSnapshot(
                    rs.getObject("snapshot_id", Long.class),
                    UUID.fromString(rs.getString("player_id")),
                    itemStackCodec.deserialize(Objects.requireNonNull(rs.getString("items")))
            );
        }
    }


}
