package io.github.hello09x.onesync.repository;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.util.ItemStackMapTypeHandler;
import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.Statement;

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
