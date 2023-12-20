package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

@Table("pdc_snapshot")
public record PDCSnapshot(

        @TableId("snapshot_id")
        Long snapshotId,

        @TableField("player_id")
        UUID playerId,

        @TableField("data")
        byte[] data

) implements SnapshotComponent {

        @Override
        public @NotNull MenuItem[] toMenuItems(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> back) {
                var item = new ItemStack(Material.STRUCTURE_VOID);
                item.editMeta(meta -> {
                        meta.displayName(text("PDC"));
                        meta.lore(List.of(
                                text("该数据无法预览", GRAY)
                        ));
                });
                return new MenuItem[]{new MenuItem(item)};
        }

}
