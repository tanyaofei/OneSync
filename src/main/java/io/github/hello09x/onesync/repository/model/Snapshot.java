package io.github.hello09x.onesync.repository.model;


import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.bedrock.util.Components;
import io.github.hello09x.bedrock.util.KeyBinds;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Stream;

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
@Table("snapshot")
public record Snapshot(

        @TableId("id")
        Long id,

        @TableField("player_id")
        UUID playerId,

        @TableField("cause")
        SnapshotCause cause,

        @TableField("created_at")
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
            meta.displayName(Components.noItalic("快照"));
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            meta.lore(Stream.of(
                    textOfChildren(text("编号: ", GRAY), text("#" + this.id, WHITE)),
                    textOfChildren(text("节点: ", GRAY), this.cause.getDisplayName()),
                    textOfChildren(text("时间: ", GRAY), text(stringifyTime(this.createdAt), WHITE)),
                    empty(),
                    text("「左键」查看详情", GRAY),
                    text("「右键」恢复数据", GRAY),
                    textOfChildren(text("「"), keybind(KeyBinds.DROP), text(" 键」删除")).color(GRAY)
            ).map(Components::noItalic).toList());
        });

        return item;
    }

}
