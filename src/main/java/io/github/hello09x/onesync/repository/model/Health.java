package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;

import java.util.UUID;

@Table("onesync.health")
public record Health (

        @TableId("uuid")
        UUID uuid,

        @TableField("health")
        Double health,

        @TableField("max_health")
        Double maxHealth

){
}
