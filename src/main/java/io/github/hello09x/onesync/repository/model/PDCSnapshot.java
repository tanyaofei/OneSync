package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static io.github.hello09x.bedrock.util.Components.noItalic;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

@Table("pdc_snapshot")
public record PDCSnapshot(

        @TableId("snapshot_id") Long snapshotId,

        @TableField("player_id") UUID playerId,

        @TableField("data") byte[] data

) implements SnapshotComponent {

    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> onCancel) {
        var item = new ItemStack(Material.STRUCTURE_VOID);
        item.editMeta(meta -> {
            meta.displayName(noItalic("PDC", DARK_GREEN));
            meta.lore(List.of(
                    text("该数据不支持预览", GRAY),
                    empty()
            ));
        });
        return new MenuItem(item);
    }

}
