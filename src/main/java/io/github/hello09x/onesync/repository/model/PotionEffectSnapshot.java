package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.bedrock.util.Components;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.handler.PotionEffectSnapshotHandler;
import io.github.hello09x.onesync.util.PotionEffectListTypeHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static io.github.hello09x.bedrock.util.Components.noItalic;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Table("potion_effect_snapshot")
public record PotionEffectSnapshot(

        @TableId("snapshot_id")
        Long snapshotId,

        @TableField("player_id")
        UUID playerId,

        @TableField(value = "effects", typeHandler = PotionEffectListTypeHandler.class)
        List<PotionEffect> effects

) implements SnapshotComponent {

    @Override
    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> onCancel) {
        var item = new ItemStack(Material.POTION);
        item.editMeta(meta -> {
            meta.displayName(noItalic("效果", DARK_PURPLE));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);

            if (this.effects.isEmpty()) {
                meta.lore(List.of(text("无", GRAY)));
            } else {
                meta.lore(this.effects
                        .stream()
                        .map(effect -> textOfChildren(
                                translatable(effect.getType()), text(": "),
                                text(effect.getAmplifier()),
                                text(" | "), text(effect.isInfinite() ? "永久" : (effect.getDuration() / 20) + " 秒")).color(WHITE)
                        )
                        .map(Components::noItalic)
                        .toList());
            }
        });
        return new MenuItem(item);
    }

}
