package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.PDCSnapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

public class PDCSnapshotRepository extends Repository<PDCSnapshot> {

    public final static PDCSnapshotRepository instance = new PDCSnapshotRepository(Main.getInstance());

    public PDCSnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull PDCSnapshot snapshot) {
        var sql = "insert into pdc_snapshot (snapshot_id, player_id, `data`) values (?, ?, ?)";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                stm.setBytes(3, snapshot.data());
                return stm.executeUpdate();
            }
        });
    }

    public @Nullable PDCSnapshot selectLatestByPlayerId(@NotNull UUID playerId) {
        var sql = "select * from pdc_snapshot where player_id = ? order by snapshot_id limit 1";
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
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted(connection.getCatalog() + "/" + "pdc_snapshot"));
            if (rs.next()) {
                return;
            }

            stm.executeUpdate("""
                    create table pdc_snapshot
                    (
                        snapshot_id bigint   not null
                            primary key,
                        player_id   char(36) not null,
                        data        blob     not null
                    );
                    """);
            stm.executeUpdate("""
                    create index pdc_snapshot_player_id_index
                        on pdc_snapshot (player_id);
                    """);
        });
    }
}
