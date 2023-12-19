package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("locking")
public record Locking(

        @TableId("player_id")
        UUID playerId,

        @TableId("server_id")
        UUID serverId,

        @TableField("created_at")
        LocalDateTime createdAt

) {
}
