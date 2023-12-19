package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import io.github.hello09x.onesync.util.ItemStackMapTypeHandler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

public class InventorySnapshotRepository extends Repository<InventorySnapshot> {

    public final static InventorySnapshotRepository instance = new InventorySnapshotRepository(Main.getInstance());

    public InventorySnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull InventorySnapshot snapshot) {
        var sql = """
                insert into inventory_snapshot (snapshot_id, player_id, items, ender_items)
                values (?, ?, ?, ?)
                """;
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                ItemStackMapTypeHandler.instance.setParameter(stm, 3, snapshot.items());
                ItemStackMapTypeHandler.instance.setParameter(stm, 4, snapshot.enderItems());
                return stm.executeUpdate();
            }
        });
    }

    public @Nullable InventorySnapshot selectLatestByPlayerId(@NotNull UUID playerId) {
        var sql = "select * from inventory_snapshot where player_id = ? order by snapshot_id limit 1";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, playerId.toString());
                return mapOne(stm.executeQuery());
            }
        });
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted( connection.getCatalog() + "/" + "inventory_snapshot"));
            if (rs.next()) {
                return;
            }
            stm.executeUpdate("""
                    create table inventory_snapshot
                    (
                        snapshot_id bigint   not null
                            primary key,
                        player_id   char(36) not null,
                        items       json     not null,
                        ender_items json     not null
                    );
                    """);
            stm.executeUpdate("""
                    create index inventory_snapshot_player_id_index
                        on inventory_snapshot (player_id);
                    """);
        });
    }

}
