package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("onesync.pdc")
public record PDC(

        @TableId("uuid")
        UUID uuid,

        @TableField("data")
        byte[] data,

        @TableField("created_at")
        LocalDateTime createdAt,

        @TableField("updated_at")
        LocalDateTime updatedAt

) {


}
