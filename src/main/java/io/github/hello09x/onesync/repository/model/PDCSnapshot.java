package io.github.hello09x.onesync.repository.model;

import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.RowMapper;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static io.github.hello09x.devtools.core.utils.ComponentUtils.noItalic;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

public record PDCSnapshot(

        Long snapshotId,

        @NotNull
        UUID playerId,

        byte @NotNull [] data

) implements SnapshotComponent {

    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> prevMenu) {
        var item = new ItemStack(Material.STRUCTURE_VOID);
        item.editMeta(meta -> {
            meta.displayName(noItalic(text("PDC", DARK_GREEN)));
            meta.lore(List.of(
                    text("该数据不支持预览", GRAY),
                    empty()
            ));
        });
        return new MenuItem(item);
    }

    @Singleton
    public static class PDCSnapshotRowMapper implements RowMapper<PDCSnapshot> {

        @Override
        public @Nullable PDCSnapshot mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            return new PDCSnapshot(
                    rs.getObject("snapshot_id", Long.class),
                    UUID.fromString(rs.getString("player_id")),
                    rs.getBytes("data")
            );
        }
    }

}
