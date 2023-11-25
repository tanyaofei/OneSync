package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.onesync.manager.SynchronizedData;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("snapshot")
public record Snapshot(

        @TableId("id")
        Long id,

        @TableField("uuid")
        UUID uuid,

        @TableField("data")
        SynchronizedData data,

        @TableField("pinned")
        Boolean pinned,

        @TableField("createdAt")
        LocalDateTime createdAt


) {


}
