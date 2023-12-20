package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

/**
 * @param snapshotId 快照 ID
 * @param playerId   玩家 ID
 * @param gameMode   游戏模式
 * @param op         是否 OP
 * @param level      经验等级
 * @param exp        当前等级经验值
 * @param health     生命值
 * @param maxHealth  最大生命值
 * @param foodLevel  饥饿值
 * @param saturation 饱食度
 * @param exhaustion 饥饿度
 */
@Table("profile_snapshot")
public record ProfileSnapshot(

        @TableId("snapshot_id")
        Long snapshotId,

        @TableField("player_id")
        UUID playerId,

        @Nullable
        @TableField("game_mode")
        GameMode gameMode,

        @Nullable
        @TableField("op")
        Boolean op,

        @Nullable
        @TableField("level")
        Integer level,

        @Nullable
        @TableField("exp")
        Float exp,

        @Nullable
        @TableField("health")
        Double health,

        @Nullable
        @TableField("max_health")
        Double maxHealth,

        @Nullable
        @TableField("food_level")
        Integer foodLevel,

        @Nullable
        @TableField("saturation")
        Float saturation,

        @Nullable
        @TableField("exhaustion")
        Float exhaustion

) implements SnapshotComponent {

        @Override
        public @NotNull MenuItem[] toMenuItems(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> back) {
                var item = new ItemStack(Material.APPLE);
                item.editMeta(meta -> {
                        meta.displayName(text("档案"));
                        meta.lore(List.of(
                                textOfChildren(text("游戏模式: ", GRAY), Optional.ofNullable(this.gameMode).map(v -> (Component) translatable(v, WHITE)).orElse(text("<无>", WHITE))),
                                textOfChildren(text("OP: ", GRAY), text(Optional.ofNullable(this.op).map(v -> v ? "是" : "否").orElse("<无>"), WHITE)),
                                textOfChildren(text("等级: ", GRAY), text(Optional.ofNullable(this.level).map(Object::toString).orElse("<无>"), WHITE)),
                                textOfChildren(text("经验条: ", GRAY), text(Optional.ofNullable(this.exp).map(Object::toString).orElse("<无>"), WHITE)),
                                textOfChildren(text("生命值: ", GRAY), text(Optional.ofNullable(this.health).map(Object::toString).orElse("<无>"), WHITE)),
                                textOfChildren(text("最大生命值: ", GRAY), text(Optional.ofNullable(this.maxHealth).map(Object::toString).orElse("<无>"), WHITE)),
                                textOfChildren(text("饥饿值: ", GRAY), text(Optional.ofNullable(this.foodLevel).map(Object::toString).orElse("<无>"), WHITE)),
                                textOfChildren(text("饱食度: ", GRAY), text(Optional.ofNullable(this.saturation).map(Object::toString).orElse("<无>"), WHITE)),
                                textOfChildren(text("饥饿度: ", GRAY), text(Optional.ofNullable(this.exhaustion).map(Object::toString).orElse("<无>"), WHITE))
                        ));
                });
                return new MenuItem[]{new MenuItem(item)};
        }

}
