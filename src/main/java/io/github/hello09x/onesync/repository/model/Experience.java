package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;

import java.util.UUID;

@Table("onesync.experience")
public record Experience(

        @TableId("uuid")
        UUID uuid,

        @TableField("level")
        Integer level,

        @TableField("exp")
        Float exp

) {
}
