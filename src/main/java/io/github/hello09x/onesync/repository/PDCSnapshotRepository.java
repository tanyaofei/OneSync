package io.github.hello09x.onesync.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.Exceptions;
import io.github.hello09x.devtools.database.jdbc.JdbcTemplate;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.repository.model.PDCSnapshot;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Singleton
public class PDCSnapshotRepository {

    private final JdbcTemplate jdbc;

    private final PDCSnapshot.PDCSnapshotRowMapper rowMapper;

    @Inject
    public PDCSnapshotRepository(JdbcTemplate jdbc, PDCSnapshot.PDCSnapshotRowMapper rowMapper) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
        this.initTables();
    }

    @CanIgnoreReturnValue
    public int insert(@NotNull PDCSnapshot snapshot) {
        var sql = "insert into pdc_snapshot (snapshot_id, player_id, `data`) values (?, ?, ?)";
        return jdbc.update(sql, snapshot.snapshotId(), snapshot.playerId().toString(), snapshot.data());
    }

    public @Nullable PDCSnapshot selectBySnapshotId(@NotNull Long snapshotId) {
        return jdbc.queryForObject("select * from pdc_snapshot where snapshot_id = ?", rowMapper, snapshotId);
    }

    @CanIgnoreReturnValue
    public int deleteBySnapshotIds(@NotNull List<Long> snapshotIds) {
        if (snapshotIds.isEmpty()) {
            return 0;
        }
        return jdbc.update("delete from pdc_snapshot where snapshot_id in (?)", StringUtils.join(snapshotIds, ","));
    }

    protected void initTables() {
        jdbc.execute("""
                    create table if not exists pdc_snapshot
                    (
                        snapshot_id bigint   not null comment '快照 ID'
                            primary key,
                        player_id   char(36) not null comment '玩家 ID',
                        `data`      blob     not null comment 'PDC 数据'
                    );
                    """);

        Exceptions.suppress(OneSync.getInstance(), () -> {
            jdbc.execute("""
                        create index pdc_snapshot_player_id_index
                            on pdc_snapshot (player_id);
                        """);
        }, false);

    }
}
