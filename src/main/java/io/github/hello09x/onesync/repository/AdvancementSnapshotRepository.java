package io.github.hello09x.onesync.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.Exceptions;
import io.github.hello09x.devtools.database.jdbc.JdbcTemplate;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.repository.model.AdvancementSnapshot;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Singleton
public class AdvancementSnapshotRepository {

    private final JdbcTemplate jdbc;
    private final AdvancementSnapshot.AdvancementSnapshotRowMapper rowMapper;
    private final Gson gson;

    @Inject
    public AdvancementSnapshotRepository(JdbcTemplate jdbc, AdvancementSnapshot.AdvancementSnapshotRowMapper rowMapper, Gson gson) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
        this.gson = gson;
        this.initTables();
    }

    public @Nullable AdvancementSnapshot selectBySnapshotId(@NotNull Long snapshotId) {
        return jdbc.queryForObject("select * from advancement_snapshot where snapshot_id = ?", rowMapper, snapshotId);
    }

    @CanIgnoreReturnValue
    public int deleteBySnapshotIds(@NotNull List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        return jdbc.update("delete from advancement_snapshot where snapshot_id in (?)", StringUtils.join(ids, ","));
    }

    @CanIgnoreReturnValue
    public int insert(@NotNull AdvancementSnapshot snapshot) {
        var sql = "insert into advancement_snapshot (snapshot_id, player_id, advancements) values (?, ?, ?)";
        return jdbc.update(
                sql,
                snapshot.snapshotId(),
                snapshot.playerId().toString(),
                gson.toJson(snapshot.advancements())
        );
    }

    protected void initTables() {
        jdbc.execute("""
                    create table if not exists advancement_snapshot
                    (
                        snapshot_id  bigint   not null comment '快照 ID'
                            primary key,
                        player_id    char(36) not null comment '玩家 ID',
                        advancements json     not null comment '成就数据'
                    );
                    """);

        Exceptions.suppress(OneSync.getInstance(), () -> {
            jdbc.execute("""
                        create index advancement_snapshot_player_id_index
                            on advancement_snapshot (player_id);
                        """);
        }, false);

    }
}
