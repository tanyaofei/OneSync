package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.ProfileSnapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

public class ProfileSnapshotRepository extends Repository<ProfileSnapshot> {

    public final static ProfileSnapshotRepository instance = new ProfileSnapshotRepository(Main.getInstance());


    public ProfileSnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

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

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted(connection.getCatalog() + "/" + "profile_snapshot"));
            if (rs.next()) {
                return;
            }

            stm.executeUpdate("""
                    create table profile_snapshot
                    (
                        snapshot_id bigint      not null
                            primary key,
                        player_id   char(36)    not null,
                        game_mode   varchar(32) null,
                        op          tinyint(1)  null,
                        level       int         null,
                        exp         float       null,
                        health      double      null,
                        max_health  double      null,
                        food_level  int         null,
                        saturation  float       null,
                        exhaustion  float       null
                    );
                    """);
            stm.executeUpdate("""
                    create index profile_snapshot_player_id_index
                        on profile_snapshot (player_id);
                    """);
        });
    }
}
