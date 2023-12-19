package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import org.bukkit.GameMode;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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

) {
}
