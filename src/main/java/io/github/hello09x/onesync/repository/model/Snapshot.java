package io.github.hello09x.onesync.repository.model;


import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
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

        private final static DateTimeFormatter FULL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

        public @NotNull ItemStack toMenuItem() {
                var item = new ItemStack(Material.CHEST);
                item.editMeta(meta -> {
                        meta.displayName(text("快照"));
                        meta.lore(List.of(
                                textOfChildren(text("ID: ", GRAY), text(this.id, WHITE)),
                                textOfChildren(text("节点: ", GRAY), this.cause.getDisplayName().color(WHITE)),
                                textOfChildren(text("时间: ", GRAY), text(stringifyTime(this.createdAt), WHITE))
                        ));
                });

                return item;
        }

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
                        return FULL_DATE_TIME_FORMATTER.format(dateTime);
                }
        }

}
