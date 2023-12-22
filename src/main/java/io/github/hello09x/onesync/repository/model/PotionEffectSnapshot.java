package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.bedrock.util.Components;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
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
import java.util.stream.Stream;

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
                meta.lore(Stream
                        .concat(this.effects
                                .stream()
                                .map(effect -> textOfChildren(
                                        translatable(effect.getType(), GRAY), space(), text(getLevel(effect.getAmplifier()), GRAY),
                                        text(" : ", GRAY),
                                        text(getDuration(effect.getDuration()))
                                ).color(WHITE)), Stream.of(empty()))
                        .map(Components::noItalic)
                        .toList());
            }
        });
        return new MenuItem(item);
    }

    private static @NotNull String getLevel(int amplifier) {
        var level = amplifier + 1;
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "XI";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
    }

    private static @NotNull String getDuration(int ticks) {
        if (ticks == PotionEffect.INFINITE_DURATION) {
            return "∞";
        }
        var total = ticks / 20;

        var hours = total / 3600;
        var minutes = (total % 3600) / 60;
        var seconds = total % 60;

        if (hours == 0) {
            if (minutes == 0) {
                return "%02d".formatted(seconds);
            }
            return "%02d:%02d".formatted(minutes, seconds);
        } else {
            return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
        }
    }

}
