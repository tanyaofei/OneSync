package io.github.hello09x.onesync.repository.model;


import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.constant.Keybinds;
import io.github.hello09x.devtools.core.utils.ComponentUtils;
import io.github.hello09x.devtools.database.jdbc.RowMapper;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Stream;

import static io.github.hello09x.devtools.core.utils.ComponentUtils.noItalic;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

/**
 * 快照
 *
 * @param id        快照 ID
 * @param playerId  玩家 ID
 * @param cause     生成快照原因
 * @param createdAt 创建时间
 */
public record Snapshot(

        Long id,

        @NotNull
        UUID playerId,

        @NotNull
        SnapshotCause cause,

        LocalDateTime createdAt

) {

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static @NotNull String stringifyTime(@NotNull LocalDateTime dateTime) {
        var date = dateTime.toLocalDate();
        var diff = Period.between(date, LocalDate.now()).getDays();
        if (diff == 0) {
            return "今天 " + TIME_FORMATTER.format(dateTime);
        } else if (diff == 1) {
            return "昨天 " + TIME_FORMATTER.format(dateTime);
        } else if (diff == 2) {
            return "前天 " + TIME_FORMATTER.format(dateTime);
        } else {
            return DATE_TIME_FORMATTER.format(dateTime);
        }
    }

    public @NotNull ItemStack toMenuItem() {
        var item = new ItemStack(this.cause.getIcon());
        item.editMeta(meta -> {
            meta.displayName(noItalic(text("快照")));
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            meta.lore(Stream.of(
                    textOfChildren(text("编号: ", GRAY), text("#" + this.id, WHITE)),
                    textOfChildren(text("节点: ", GRAY), this.cause.getDisplayName()),
                    textOfChildren(text("时间: ", GRAY), text(stringifyTime(this.createdAt), WHITE)),
                    empty(),
                    text("「左键」查看详情", GRAY),
                    text("「右键」恢复数据", GRAY),
                    textOfChildren(text("「"), keybind(Keybinds.DROP), text(" 键」删除")).color(GRAY)
            ).map(ComponentUtils::noItalic).toList());
        });

        return item;
    }

    @Singleton
    public static class SnapshotRowMapper implements RowMapper<Snapshot> {

        @Override
        public @Nullable Snapshot mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            return new Snapshot(
                    rs.getObject("id", Long.class),
                    UUID.fromString(rs.getString("player_id")),
                    SnapshotCause.valueOf(rs.getString("cause")),
                    LocalDateTime.ofInstant(rs.getDate("created_at").toInstant(), ZoneId.systemDefault())
            );
        }
    }

}
