package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.onesync.repository.constant.TeleportStatus;
import io.github.hello09x.onesync.repository.constant.TeleportType;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("teleport_request")
public record TeleportRequest(

        @TableField("requester_id")
        UUID requesterId,

        @TableField("receiver_id")
        UUID receiverId,

        @TableField("type")
        TeleportType type,

        @TableField("status")
        TeleportStatus status,

        @TableField("created_at")
        LocalDateTime createdAt


) {
}
