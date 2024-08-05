package io.github.hello09x.onesync.repository.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.RowMapper;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static io.github.hello09x.devtools.core.utils.ComponentUtils.noItalic;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public record AdvancementSnapshot(

        @NotNull
        Long snapshotId,

        @NotNull
        UUID playerId,

        @NotNull
        Map<String, Collection<String>> advancements

) implements SnapshotComponent {

    @Override
    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> prevMenu) {
        var item = new ItemStack(Material.GRASS_BLOCK);
        item.editMeta(meta -> {
            meta.displayName(noItalic(text("成就", WHITE)));
            meta.lore(List.of(
                    text("该数据不支持预览", GRAY)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return new MenuItem(item);
    }

    @Singleton
    public static class AdvancementSnapshotRowMapper implements RowMapper<AdvancementSnapshot> {

        private final Gson gson;

        private final static TypeToken<Map<String, Collection<String>>> ADVANCEMENTS_TYPE = new TypeToken<>() {
        };

        @Inject
        public AdvancementSnapshotRowMapper(Gson gson) {
            this.gson = gson;
        }

        @Override
        public @Nullable AdvancementSnapshot mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            return new AdvancementSnapshot(
                    rs.getObject("snapshot_id", Long.class),
                    UUID.fromString(rs.getString("player_id")),
                    gson.fromJson(rs.getString("advancements"), ADVANCEMENTS_TYPE)
            );
        }
    }

}
