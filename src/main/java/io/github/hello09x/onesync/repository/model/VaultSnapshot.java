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
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Table("vault_snapshot")
public record VaultSnapshot(

        @TableId("snapshot_id")
        Long snapshotId,

        @TableField("player_id")
        UUID playerId,

        @TableField("balance")
        Double balance

) implements SnapshotComponent {

    @Override
    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> onClickOutside) {
        var item = new ItemStack(Material.EMERALD);
        item.editMeta(meta -> {
            meta.displayName(noItalic("经济", GOLD));
            meta.lore(List.of(
                    noItalic(textOfChildren(text("余额: ", GRAY), text(String.valueOf(this.balance), WHITE)))
            ));
        });
        return new MenuItem(item);
    }
}
