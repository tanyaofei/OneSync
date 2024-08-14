package io.github.hello09x.onesync.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.Exceptions;
import io.github.hello09x.devtools.database.jdbc.JdbcTemplate;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import io.github.hello09x.onesync.util.ItemStackCodec;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Singleton
public class InventorySnapshotRepository {

    private final JdbcTemplate jdbc;
    private final InventorySnapshot.InventorySnapshotRowMapper rowMapper;
    private final ItemStackCodec itemStackCodec;

    @Inject
    public InventorySnapshotRepository(JdbcTemplate jdbc, InventorySnapshot.InventorySnapshotRowMapper rowMapper, ItemStackCodec itemStackCodec) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
        this.itemStackCodec = itemStackCodec;
        this.initTables();
    }

    @CanIgnoreReturnValue
    public int insert(@NotNull InventorySnapshot snapshot) {
        var sql = """
                insert into inventory_snapshot (snapshot_id, player_id, items, held_item_slot)
                values (?, ?, ?, ?)
                """;

        return jdbc.update(
                sql,
                snapshot.snapshotId(),
                snapshot.playerId().toString(),
                itemStackCodec.serialize(snapshot.items()),
                snapshot.heldItemSlot()
        );
    }

    public @Nullable InventorySnapshot selectBySnapshotId(@NotNull Long snapshotId) {
        return jdbc.queryForObject("select * from inventory_snapshot where snapshot_id = ?", rowMapper, snapshotId);
    }

    @CanIgnoreReturnValue
    public int deleteBySnapshotIds(@NotNull List<Long> snapshotIds) {
        if (snapshotIds.isEmpty()) {
            return 0;
        }
        return jdbc.update("delete from inventory_snapshot where snapshot_id in (?)", StringUtils.join(snapshotIds, ","));
    }

    @CanIgnoreReturnValue
    public int updateItemsBySnapshotId(@NotNull Long snapshotId, @NotNull Map<Integer, ItemStack> items) {
        var sql = "update inventory_snapshot set items = ? where snapshot_id = ?";
        return jdbc.update(sql, itemStackCodec.serialize(items), snapshotId);
    }

    protected void initTables() {
        jdbc.execute("""
                    create table if not exists `inventory_snapshot`
                    (
                        snapshot_id        bigint        not null comment '快照 ID'
                            primary key,
                        player_id          char(36)      not null comment '玩家 ID',
                        items              json          not null comment '背包物品',
                        held_item_slot     int default 0 not null comment '选中的物品槽'
                    );
                    """);

        Exceptions.suppress(OneSync.getInstance(), () -> {
            jdbc.execute("""
                        create index inventory_snapshot_player_id_index
                            on inventory_snapshot (player_id);
                        """);
        }, false);
    }

}
