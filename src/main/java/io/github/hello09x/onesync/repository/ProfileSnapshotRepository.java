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
                    exhaustion,
                    remaining_air
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                stm.setObject(12, snapshot.remainingAir());
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
                        snapshot_id   bigint      not null comment '快照 ID'
                            primary key,
                        player_id     char(36)    not null comment '玩家 ID',
                        game_mode     varchar(32) null comment '游戏模式',
                        op            tinyint(1)  null comment '是否 OP',
                        level         int         null comment '等级',
                        exp           float       null comment '当前经验值',
                        health        double      null comment '生命值',
                        max_health    double      null comment '最大生命值',
                        food_level    int         null comment '饥饿值',
                        saturation    float       null comment '饱食度',
                        exhaustion    float       null comment '饥饿程度',
                        remaining_air int         null comment '氧气值'
                    );
                    """);
            stm.executeUpdate("""
                    create index profile_snapshot_player_id_index
                        on profile_snapshot (player_id);
                    """);
        });
    }
}
