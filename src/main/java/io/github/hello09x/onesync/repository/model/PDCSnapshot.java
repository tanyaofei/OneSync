package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;

import java.util.UUID;

@Table("pdc_snapshot")
public record PDCSnapshot(

        @TableId("snapshot_id")
        Long snapshotId,

        @TableField("player_id")
        UUID playerId,

        @TableField("data")
        byte[] data

) {
}
