package io.github.hello09x.onesync.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.JdbcTemplate;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.repository.model.ProfileSnapshot;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static io.github.hello09x.devtools.core.utils.Exceptions.suppress;

@Singleton
public class ProfileSnapshotRepository {

    private final JdbcTemplate jdbc;

    private final ProfileSnapshot.ProfileSnapshotRowMapper rowMapper;

    @Inject
    public ProfileSnapshotRepository(JdbcTemplate jdbc, ProfileSnapshot.ProfileSnapshotRowMapper rowMapper) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
        this.initTables();
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


        return jdbc.update(
                sql,
                snapshot.snapshotId(),
                snapshot.playerId().toString(),
                Optional.ofNullable(snapshot.gameMode()).map(Enum::name).orElse(null),
                snapshot.op(),
                snapshot.level(),
                snapshot.exp(),
                snapshot.health(),
                snapshot.maxHealth(),
                snapshot.foodLevel(),
                snapshot.saturation(),
                snapshot.exhaustion(),
                snapshot.remainingAir()
        );
    }

    public @Nullable ProfileSnapshot selectBySnapshotId(@NotNull Long snapshotId) {
        return jdbc.queryForObject("select * from profile_snapshot where snapshot_id=?", rowMapper, snapshotId);
    }

    @CanIgnoreReturnValue
    public int deleteBySnapshotIds(@NotNull List<Long> snapshotIds) {
        if (snapshotIds.isEmpty()) {
            return 0;
        }
        return jdbc.update("delete from profile_snapshot where snapshot_id in (?)", StringUtils.join(snapshotIds, ","));
    }

    protected void initTables() {
        jdbc.execute("""
                     create table if not exists profile_snapshot
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

        suppress(OneSync.getInstance(), () -> {
            jdbc.execute("""
                        create index profile_snapshot_player_id_index
                            on profile_snapshot (player_id);
                        """);
        }, false);

    }
}
