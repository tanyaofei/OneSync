package io.github.hello09x.onesync.repository.model;

import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.onesync.util.ItemStackMapTypeHandler;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @param snapshotId 快照 ID
 * @param playerId   玩家 ID
 * @param items      背包物品
 * @param enderItems 末影响物品
 */
@Table("inventory_snapshot")
public record InventorySnapshot(

        @TableField("snapshot_id")
        Long snapshotId,

        @TableField("player_id")
        UUID playerId,

        @TableField(value = "items", typeHandler = ItemStackMapTypeHandler.class)
        Map<Integer, ItemStack> items,

        @TableField(value = "ender_items", typeHandler = ItemStackMapTypeHandler.class)
        Map<Integer, ItemStack> enderItems


) {


}
