package io.github.hello09x.onesync.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.JdbcTemplate;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.repository.model.VaultSnapshot;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static io.github.hello09x.devtools.core.utils.Exceptions.suppress;

@Singleton
public class VaultSnapshotRepository {

    private final JdbcTemplate jdbc;
    private final VaultSnapshot.VaultSnapshotRowMapper rowMapper;

    @Inject
    public VaultSnapshotRepository(JdbcTemplate jdbc, VaultSnapshot.VaultSnapshotRowMapper rowMapper) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
        this.initTables();
    }

    @CanIgnoreReturnValue
    public int insert(@NotNull VaultSnapshot snapshot) {
        var sql = "insert into vault_snapshot (snapshot_id, player_id, balance) values (?, ?, ?)";
        return jdbc.update(sql, snapshot.snapshotId(), snapshot.playerId().toString(), snapshot.balance());
    }

    public @Nullable VaultSnapshot selectBySnapshotId(@NotNull Long snapshotId) {
        return jdbc.queryForObject("select * from vault_snapshot where snapshot_id = ?", rowMapper, snapshotId);
    }

    @CanIgnoreReturnValue
    public int deleteBySnapshotIds(@NotNull List<Long> snapshotIds) {
        if (snapshotIds.isEmpty()) {
            return 0;
        }
        return jdbc.update("delete from vault_snapshot where snapshotIds in (?)", StringUtils.join(snapshotIds, ","));
    }

    protected void initTables() {
        jdbc.execute("""
                    create table if not exists vault_snapshot
                    (
                        snapshot_id bigint   not null comment '快照 ID'
                            primary key,
                        player_id   char(36) not null comment '玩家 ID',
                        balance     double   not null comment '余额'
                    );
                    """);


        suppress(OneSync.getInstance(), () -> {
            jdbc.execute("""
                         create index vault_snapshot_player_id_index
                             on vault_snapshot (player_id);
                         """);
        }, false);
    }
}
