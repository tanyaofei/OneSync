package io.github.hello09x.onesync.repository.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.ComponentUtils;
import io.github.hello09x.devtools.database.jdbc.RowMapper;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.util.PotionEffectListTypeCodec;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.github.hello09x.devtools.core.utils.ComponentUtils.noItalic;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public record PotionEffectSnapshot(

        Long snapshotId,

        @NotNull
        UUID playerId,

        @NotNull
        List<PotionEffect> effects

) implements SnapshotComponent {

    @Override
    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> prevMenu) {
        var item = new ItemStack(Material.POTION);
        item.editMeta(meta -> {
            meta.displayName(noItalic(text("效果", DARK_PURPLE)));
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
                        .map(ComponentUtils::noItalic)
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

    @Singleton
    public static class PotionEffectSnapshotRowMapper implements RowMapper<PotionEffectSnapshot> {

        private final PotionEffectListTypeCodec potionEffectListTypeCodec;

        @Inject
        public PotionEffectSnapshotRowMapper(PotionEffectListTypeCodec potionEffectListTypeCodec) {
            this.potionEffectListTypeCodec = potionEffectListTypeCodec;
        }

        @Override
        public @Nullable PotionEffectSnapshot mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            return new PotionEffectSnapshot(
                    rs.getObject("snapshot_id", Long.class),
                    UUID.fromString(rs.getString("snapshot_id")),
                    potionEffectListTypeCodec.deserialize(rs.getString("effects"))
            );
        }
    }

}
