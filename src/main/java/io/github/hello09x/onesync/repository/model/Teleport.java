package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.onesync.repository.constant.TeleportStatus;
import io.github.hello09x.onesync.repository.constant.TeleportType;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("teleport_request")
public record Teleport(

        @TableField("requester")
        String requester,

        @TableField("receiver")
        String receiver,

        @TableField("type")
        TeleportType type,

        @TableField("created_at")
        LocalDateTime createdAt


) {
}
