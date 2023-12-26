package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.onesync.util.NamespacedKeyTypeHandler;
import org.bukkit.NamespacedKey;

import java.util.UUID;

/**
 * @param id         ID
 * @param snapshotId 快照 ID
 * @param playerId   玩家 ID
 * @param type       类型
 * @param data       序列化后的数据
 */
@Table("extension_snapshot")
public record ExtensionSnapshot(

        @TableId("id")
        Long id,

        @TableField("snapshot_id")
        Long snapshotId,

        @TableField("player_id")
        UUID playerId,

        @TableField(value = "type", typeHandler = NamespacedKeyTypeHandler.class)
        NamespacedKey type,

        @TableField("data")
        byte[] data

) {


}
