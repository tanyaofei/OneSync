package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.ProfileSnapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLType;
import java.util.Optional;
import java.util.UUID;

public class ProfileSnapshotRepository extends Repository<ProfileSnapshot> {


    public final static ProfileSnapshotRepository instance = new ProfileSnapshotRepository(Main.getInstance());

    public int insert(@NotNull ProfileSnapshot snapshot) {
        var sql = """
                insert into profile_snapshot (
                    snapshot_id,
                    player_id,
                    game_mode,
                    op,
                    `level`,
                    exp,
                    health,
                    max_health,
                    food_level,
                    saturation,
                    exhaustion
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                stm.setString(3, Optional.ofNullable(snapshot.gameMode()).map(Enum::name).orElse(null));
                stm.setObject(4, snapshot.op());
                stm.setObject(5, snapshot.level());
                stm.setObject(6, snapshot.exp());
                stm.setObject(7, snapshot.health());
                stm.setObject(8, snapshot.maxHealth());
                stm.setObject(9, snapshot.foodLevel());
                stm.setObject(10, snapshot.saturation());
                stm.setObject(11, snapshot.exhaustion());
                return stm.executeUpdate();
            }
        });
    }

    public @Nullable ProfileSnapshot selectLatestByPlayerId(@NotNull UUID playerId) {
        var sql = "select * from profile_snapshot where player_id = ? order by snapshot_id limit 1";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, playerId.toString());
                return mapOne(stm.executeQuery());
            }
        });
    }

    public ProfileSnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    protected void initTables() {

    }
}
