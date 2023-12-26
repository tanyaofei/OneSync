package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import io.github.hello09x.onesync.util.ItemStackMapTypeHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

public class InventorySnapshotRepository extends Repository<InventorySnapshot> {

    public final static InventorySnapshotRepository instance = new InventorySnapshotRepository(Main.getInstance());

    public InventorySnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull InventorySnapshot snapshot) {
        var sql = """
                insert into inventory_snapshot (snapshot_id, player_id, items, held_item_slot)
                values (?, ?, ?, ?)
                """;
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                ItemStackMapTypeHandler.instance.setParameter(stm, 3, snapshot.items());
                stm.setInt(4, snapshot.heldItemSlot());
                return stm.executeUpdate();
            }
        });
    }

    public int updateItemsBySnapshotId(@NotNull Long snapshotId, @NotNull Map<Integer, ItemStack> items) {
        var sql = "update inventory_snapshot set items = ? where snapshot_id = ?";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                ItemStackMapTypeHandler.instance.setParameter(stm, 1, items);
                stm.setLong(2, snapshotId);
                return stm.executeUpdate();
            }
        });
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted(connection.getCatalog() + "/" + "inventory_snapshot"));
            if (rs.next()) {
                return;
            }
            stm.executeUpdate("""
                    create table inventory_snapshot
                    (
                        snapshot_id        bigint        not null comment '快照 ID'
                            primary key,
                        player_id          char(36)      not null comment '玩家 ID',
                        items              json          not null comment '背包物品',
                        held_item_slot     int default 0 not null comment '选中的物品槽'
                    );
                    """);
            stm.executeUpdate("""
                    create index inventory_snapshot_player_id_index
                        on inventory_snapshot (player_id);
                    """);
        });
    }

}
