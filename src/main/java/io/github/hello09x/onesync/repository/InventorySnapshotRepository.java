package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import io.github.hello09x.onesync.util.ItemStackMapTypeHandler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
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

    public @NotNull List<InventorySnapshot> selectByPlayerId(@NotNull UUID playerId) {
        var sql = "select * from inventory_snapshot where player_id = ? order by snapshot_id";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, playerId.toString());
                return mapMany(stm.executeQuery());
            }
        });
    }

    @Override
    protected void initTables() {

    }

}
