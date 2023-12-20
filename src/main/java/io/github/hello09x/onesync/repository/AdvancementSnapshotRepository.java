package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.bedrock.database.typehandler.JsonTypeHandler;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.AdvancementSnapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

public class AdvancementSnapshotRepository extends Repository<AdvancementSnapshot> {

    public final static AdvancementSnapshotRepository instance = new AdvancementSnapshotRepository(Main.getInstance());

    public AdvancementSnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull AdvancementSnapshot snapshot) {
        var sql = "insert into advancement_snapshot (snapshot_id, player_id, advancements) values (?, ?, ?)";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                stm.setString(3, JsonTypeHandler.gson.toJson(snapshot.advancements()));
                return stm.executeUpdate();
            }
        });
    }

    public @Nullable AdvancementSnapshot selectLatestByPlayerId(@NotNull UUID playerId) {
        var sql = "select * from advancement_snapshot where player_id = ? order by snapshot_id limit 1";
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
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted(connection.getCatalog() + "/" + "advancement_snapshot"));
            if (rs.next()) {
                return;
            }
            stm.executeUpdate("""
                    create table advancement_snapshot
                    (
                        snapshot_id  bigint   not null
                            primary key,
                        player_id    char(36) not null,
                        advancements json     not null
                    );
                    """);
            stm.executeUpdate("""
                    create index advancement_snapshot_player_id_index
                        on advancement_snapshot (player_id);
                    """);
        });
    }
}
