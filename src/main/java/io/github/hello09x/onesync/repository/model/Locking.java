package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;

import java.util.UUID;

@Table("onesync.locking")
public record Locking(

        @TableId("uuid")
        UUID uuid,

        @TableField("locked")
        Boolean locked

) {
}
