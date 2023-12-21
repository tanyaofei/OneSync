package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.bedrock.util.Components;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.util.PotionEffectListTypeHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

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
    public @NotNull MenuItem[] toMenuItems(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> back) {
        var item = new ItemStack(Material.POTION);
        item.editMeta(meta -> {
            meta.displayName(text("效果"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            meta.lore(this.effects
                    .stream()
                    .map(effect -> textOfChildren(
                                    translatable(effect.getType()), text(": "),
                                    text(effect.getAmplifier()),
                                    text(" | "), text(effect.isInfinite() ? "永久" : (effect.getDuration() / 20) + " 秒")).color(WHITE)
                    )
                    .map(Components::noItalic)
                    .toList());
        });
        return new MenuItem[]{new MenuItem(item)};
    }

}
