package io.github.hello09x.onesync.repository.model;


import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;

import java.time.LocalDateTime;
import java.util.UUID;

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
}
