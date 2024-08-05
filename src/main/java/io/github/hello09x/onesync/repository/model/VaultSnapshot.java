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
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public record VaultSnapshot(

        Long snapshotId,

        UUID playerId,

        Double balance

) implements SnapshotComponent {

    @Override
    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> prevMenu) {
        var item = new ItemStack(Material.EMERALD);
        item.editMeta(meta -> {
            meta.displayName(noItalic(text("经济", GOLD)));
            meta.lore(List.of(
                    noItalic(textOfChildren(text("余额: ", GRAY), text(String.valueOf(this.balance), WHITE)))
            ));
        });
        return new MenuItem(item);
    }

    @Singleton
    public static class VaultSnapshotRowMapper implements RowMapper<VaultSnapshot> {

        @Override
        public @Nullable VaultSnapshot mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            return new VaultSnapshot(
                    rs.getObject("snapshot_id", Long.class),
                    UUID.fromString(rs.getString("player_id")),
                    rs.getObject("balance", Double.class)
            );
        }
    }
}
